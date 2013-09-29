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

(defn feed-url [id] (str "/feeds/" id))

(defn app [{:keys [redis] :as system}]
  (->
    (routes
      (GET "/feeds" []
           (views/list-feeds (feed/all-feed-ids redis)))
      (GET "/feeds/new" []
           (views/feed-edit nil))
      (GET "/feeds/:id/edit" [id]
           (views/feed-edit (some-> (wcar redis (feed/get-feed id)) :urls)))
      (GET "/feeds/:id" [id]
           (if-let [feed (feed/get-feed redis id)]
             (views/feed-view id feed (map fetcher/get-feed (repeat redis) (:urls feed)))))
      (HEAD "/feeds/:id" [id]
            (if (feed/feed-exists? redis id)
              (response "")
              (resp/not-found "")))
      (POST "/feeds" {{:keys [urls name]} :params}
            (when-let [urls (if (feed/valid-urls? (seq urls)) (set urls))]
              (when-let [[feed-id created?] (feed/create-feed redis name urls)]
                (-> (prefab.ajax/redirect (feed-url feed-id))
                    (assoc :flash (if created?
                                    "Feed created!"
                                    "A feed with those URLs already exists!"))))))

      (GET "/random" []
           (resp/redirect "/")) ; TODO grab a random Fab url
      (GET "/" [] (views/index-page (feed/number-of-feeds redis) []))
      (route/resources "/")
      (route/not-found "Not found."))
    compojure.handler/site))
