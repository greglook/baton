(ns baton.lock
  "Locks offer a mechanism to ensure that only one process is modifying a
  protected resource at a time. Lock managers can be backed by a system which
  offers a way to do atomic compare-and-set requests."
  (:require
    [clojure.future :refer [inst?]]
    [clojure.spec :as s]))


;; ## Specs

;; The lock key is a globally unique identifier for the currently held instance
;; of the lock. This is used as part of the conditional update logic.
(s/def ::key (s/and string? not-empty))

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
  (s/keys :req [::agent
                ::leased-at
                ::expires-at]
          :opt [::key]))
