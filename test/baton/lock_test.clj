(ns baton.lock-test
  (:require
    [clojure.spec.alpha :as s]
    [clojure.test :refer :all]
    [baton.lock :as lock]))


(deftest lease-construction
  (is (s/valid? ::lock/lease (lock/new-lease "foo" "agent:bar" 300000))))
