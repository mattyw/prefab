(ns prefab.routes
  (:require [compojure.core :refer :all]
            [compojure.handler]
            [compojure.route :as route]
            [ring.util.response :as resp :refer (response redirect-after-post)]
            [taoensso.carmine :as car :refer (wcar)]
            [prefab.views :as views]
            [prefab.feed :as feed]
            [prefab.ajax]
            [prefab.fetcher :as fetcher]
            [clojure.data.json :as json]
            ))

(defn feed-url [id] (str "/feed/" id))

(defn app [{:keys [redis] :as system}]
  (->
    (routes
      (GET "/feed" []
           (views/feed-edit nil))
      (GET "/feed/edit/:id" [id]
           (views/feed-edit (wcar redis (feed/feed-urls id))))
      (GET "/feed/:id" [id]
           (views/feed-view id (map fetcher/get-feed (repeat redis) (wcar redis (feed/feed-urls id)))))
      (POST "/feed" {{:keys [urls]} :params}
            (when-let [urls (if (coll? urls) (set urls))]
              (wcar redis (feed/create-feed urls)
                    (doseq [url urls]
                      (fetcher/enqueue url)))
              (prefab.ajax/redirect (feed-url (feed/feed-id urls)))))

      (GET "/random" []
           (resp/redirect "/")) ; TODO grab a random Fab url
      (GET "/" [] (views/index-page (feed/number-of-feeds redis) []))
      (route/resources "/")
      (route/not-found "Not found."))
    compojure.handler/site))
