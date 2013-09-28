(defproject prefab "0.1.0-SNAPSHOT"
  :description "TODO"
  :url "TODO"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.json "0.2.3"]
                 [com.taoensso/timbre "2.6.2"]
                 [com.taoensso/carmine "2.2.1"]
                 [ring "1.2.0"]
                 [compojure "1.1.5"]
                 [http-kit "2.1.10"]
                 [environ "0.4.0"]
                 [midje "1.5.1"]
                 [hiccup "1.0.4"]
                 [clj-rome "0.4.0"]
                 [clj-rome-fetcher "0.1.0"]
                 ]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.4"]]
                   :source-paths ["dev"]
                   :plugins [[lein-midje "3.1.2"]]}
            :production
                  {:main prefab.system}})
