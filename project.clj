(defproject mvxcvi/baton "0.1.0-SNAPSHOT"
  :description "Distributed lock management interface"
  :url "https://github.com/greglook/baton"
  :license {:name "Public Domain"
            :url "http://unlicense.org/"}

  :aliases
  {"coverage" ["cloverage"]}

  :deploy-branches ["master"]
  :pedantic? :abort

  :plugins
  [[lein-cloverage "1.0.10"]]

  :dependencies
  [[org.clojure/clojure "1.8.0"]
   [clojure-future-spec "1.9.0-beta4"]]

  :codox
  {:metadata {:doc/format :markdown}
   :source-uri "https://github.com/greglook/baton/blob/master/{filepath}#L{line}"
   :output-path "target/doc/api"})
