(ns prefab.routes
  (:require [compojure.core :refer :all]
            [compojure.handler]
            [compojure.route :as route]
            [ring.util.response :as resp :refer (response)]
            [prefab.views :as views]
            ))


(defn app [system]
  (->
    (routes
      (GET "/feed/:id" [id]
           (response "TODO fetch & show feed"))
      (POST "/feed" {params :params}
            (response "TODO create feed"))

      (GET "/" [] (views/index-page 10 [{:title "Feed 1" :link "/feed/feed-1"}
                                        {:title "Feed 2" :link "/feed/feed-2"}
                                        {:title "Feed 3" :link "/feed/feed-3"}]))
      (route/resources "/")
      (route/not-found "Not found."))
    compojure.handler/site))
