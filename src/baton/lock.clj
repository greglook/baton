(ns baton.lock
  "Locks offer a mechanism to ensure that only one process is modifying a
  protected resource at a time. Lock managers can be backed by a system which
  offers a way to do atomic compare-and-set requests."
  (:require
    [clojure.future :refer [inst? uuid?]]
    [clojure.spec.alpha :as s])
  (:import
    java.time.Instant
    java.util.UUID))


;; ## Specs

;; The lock name identifies the resource which is being guarded by the lock.
(s/def ::name (s/and string? not-empty))

;; The lock key is a globally unique identifier for the currently held instance
;; of the lock. This is used as part of the conditional update logic.
(s/def ::key uuid?)

;; Point in time the current lease began.
(s/def ::leased-at inst?)

;; Point in time that the lease is valid until.
(s/def ::expires-at inst?)

;; The agent string is an identifier for the process holding the lock. This is
;; opaque to the library, but should be used to provide meaningful data to the
;; consumers.
(s/def ::agent string?)

;; A lease is representented as a map of the above attributes.
(s/def ::lease
  (s/keys :req [::name
                ::leased-at
                ::expires-at
                ::agent]
          :opt [::key]))



;; ## Lease Protocol

(defprotocol LeaseManager
  "A lease manager handles aquiring, refreshing, and releasing locks on
  protected resources."

  (lease-info
    [manager lock-name]
    "Returns information about the currently-held lock on a resource. Returns
    nil if the resource is not currently locked.")

  (lock!
    [manager lock-name agent-str duration]
    "Attempt to acquire a lock on the resource protected by the named lock.
    Returns a lease map on success, or throws an exception on failure with
    details about the current lock holder.

    The provided agent string is treated opaquely but should be a useful
    identifier. The duration is a requested period in milliseconds which the
    lock will expire after.")

  (renew!
    [manager lock-name lock-key duration]
    "Renew a currently-held lock on a resource by providing the key and a new
    requested duration. Returns a lock info map on success. Throws an exception
    if the lock is not held by this process.")

  (release!
    [manager lock-name lock-key]
    "Release a currently-held lock on a resource. Returns true if the lock was
    released, false if the resource is not currently locked. Throws an
    exception if the lock is held by another agent.")

  (force-unlock!
    [manager lock-name]
    "Forcibly open the named lock. Useful for manual interventions."))



;; ## Utilities

(defn expired?
  "Determine whether the given lease is expired."
  [lease]
  (let [expiry (::expires-at lease)]
    (or (nil? expiry) (.isAfter (Instant/now) expiry))))


(defn new-lease
  "Construct a new lease value."
  [lock-name agent-str duration]
  {::name lock-name
   ::key (UUID/randomUUID)
   ::leased-at (Instant/now)
   ::expires-at (.plusMillis (Instant/now) duration)
   ::agent agent-str})
