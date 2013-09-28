(ns prefab.routes
  (:require [compojure.core :refer :all]
            [compojure.handler]
            [compojure.route :as route]
            [ring.util.response :as resp :refer (response)]
            ))


(defn app [system]
  (->
    (routes
      (GET "/feed/:id" [id]
           (response "TODO fetch & show feed"))
      (POST "/feed" {params :params}
            (response "TODO create feed"))

      (GET "/" [] "Hello, prefab-world!")
      (route/resources "/")
      (route/not-found "Not found."))
    compojure.handler/site))
