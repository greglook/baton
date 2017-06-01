(defproject mvxcvi/baton "0.1.0-SNAPSHOT"
  :description "Distributed lock management interface"
  :url "https://github.com/greglook/baton"
  :license {:name "Public Domain"
            :url "http://unlicense.org/"}

  :deploy-branches ["master"]
  :pedantic? :abort

  :dependencies
  [[org.clojure/clojure "1.8.0"]
   [clojure-future-spec "1.9.0-alpha14"]])
