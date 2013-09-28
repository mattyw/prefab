(ns prefab.routes
  (:require [compojure.core :refer :all]
            [compojure.handler]))


(defn app [system]
  (->
    (routes
      (GET "/" [] "Hello, prefab-world!"))
    compojure.handler/site))
