(ns baton.manager.local
  "In-memory lock manager. Local lock managers may be constructed using the
  `local:-` URI form.

  This store is most suitable for testing and other situations which call for a
  single in-process manager."
  (:require
    [baton.lock :as lock])
  (:import
    java.time.Instant))


(defn- cas-in!
  "Helper function which does a swap! which only updates the subkey given if
  the previous value matches the one given. Returns true if the write
  succeeded."
  [state k old-val new-val]
  (let [new-data (swap! state (fn [data]
                                (if (= (get data k) old-val)
                                  (if new-val
                                    (assoc data k new-val)
                                    (dissoc data k))
                                  data)))]
    (= (get new-data k) new-val)))


(defn- try-acquire!
  "Attempt to acquire the named lock, or throw an exception."
  [leases lock-name curr-lease agent-str duration]
  (if (cas-in! leases lock-name curr-lease (lock/new-lease lock-name agent-str duration))
    ; Acquisition successful.
    true
    ; Beaten to lock.
    (let [lease (get @leases lock-name)]
      (throw (ex-info (format "Beaten to acquisition of lock %s by %s"
                              lock-name (::lock/agent lease))
                      {:lock-name lock-name
                       :lease lease})))))


(defrecord LocalLeaseManager
  [leases]

  lock/LeaseManager

  (lease-info
    [this lock-name]
    (get @leases lock-name))


  (lock!
    [this lock-name agent-str duration]
    (if-let [lease (get @leases lock-name)]
      ; Lock appears to be held.
      (if (lock/expired? lease)
        ; Lease has expired.
        (try-acquire! leases lock-name lease agent-str duration)
        ; Lease is valid.
        (throw (ex-info (format "Could not acquire lock %s currently held by %s"
                                lock-name (::lock/agent lease))
                        {:lock lock-name
                         :lease lease})))
      ; Lock appears to be open.
      (try-acquire! leases lock-name nil agent-str duration)))


  (renew!
    [this lock-name lock-key duration]
    (if-let [lease (get @leases lock-key)]
      ; Lock is currently leased.
      (if (= lock-key (::lock/key lease))
        ; We're holding the lock, renew duration.
        (let [new-expiry (.plusMillis (Instant/now) duration)
              lease' (assoc lease ::lock/expires-at new-expiry)]
          (if (cas-in! leases lock-name lease lease')
            ; Write succeeded, lease has been updated.
            lease'
            ; Write failed, someone else updated the lease.
            (let [current (get @leases lock-key)]
              (throw (ex-info (format "Conflict while renewing lease on lock %s"
                                      lock-name)
                              {:lock lock-name
                               :old-lease lease
                               :new-lease current})))))
        ; Someone else is holding the lock.
        (throw (ex-info (format "Cannot renew lease on lock %s which belongs to %s"
                                lock-name (::lock/agent lease))
                        {:lock lock-name
                         :lease lease})))
      ; No one is holding the lock.
      (throw (ex-info (format "Cannot renew lease on lock %s which is not held"
                              lock-name)
                      {:lock lock-name}))))


  (release!
    [this lock-name lock-key]
    (if-let [lease (get @leases lock-key)]
      ; Lock is or was held.
      (if (= lock-key (::lock/key lease))
        ; We're holding the lock, so release it.
        (cas-in! leases lock-name lease nil)
        ; Someone else is holding the lock.
        (throw (ex-info (format "Cannot release lock %s whose lease belongs to %s"
                                lock-name (::lock/agent lease))
                        {:lock lock-name
                         :lease lease})))
      ; Lock was not held.
      false))


  (force-unlock!
    [this lock-name]
    (swap! leases dissoc lock-name)
    true))


(alter-meta! #'->LocalLeaseManager assoc :private true)
(alter-meta! #'map->LocalLeaseManager assoc :private true)


(defn local-lease-manager
  [& {:as opts}]
  (map->LocalLeaseManager
    (assoc opts :leases (atom (sorted-map) :validator map?))))
