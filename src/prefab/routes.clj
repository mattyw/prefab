(ns prefab.routes
  (:require [compojure.core :refer :all]
            [compojure.handler]
            [compojure.route :as route]
            [ring.util.response :as resp :refer (response redirect-after-post)]
            [taoensso.carmine :as car :refer (wcar)]
            [taoensso.timbre :refer (debugf)]
            [prefab.views :as views]
            [prefab.feed :as feed]
            [prefab.ajax]
            [prefab.fetcher :as fetcher]
            [clojure.data.json :as json]
            ))

(defn feed-url [id] (str "/feeds/" id))

(defn parse-feed-id
  "If url is a Prefab feed, returns the id, otherwise nil"
  [host url]
  (-> (re-pattern (str "(?:" host "/feeds/(-?\\d+))$|^(-?\\d+)$"))
      (re-find url)
      (second)))

(defn expand-url
  "Returns a seq of URLs. Expands URL or id of a Prefab feed to a seq of that
  feed's URLs. If URL is not for a Prefab feed, returns the URL in a seq"
  [redis host url]
  (if-let [feed (some->> (parse-feed-id host url) (feed/get-feed redis))]
    (seq (:urls feed))
    (list url)))

(defn create-feed [redis headers name urls]
  (let [host (get headers "host")
        urls (set (mapcat (partial expand-url redis host) urls))]
    (debugf "name=%s URLS: %s" name urls)
    (if-let [[feed-id created?] (feed/create-feed redis name urls)]
      (-> (prefab.ajax/redirect (feed-url feed-id))
          (assoc :flash (if created?
                          "Feed created!"
                          "A feed with those URLs already exists!")))
      (response "Failed to create feed"))))

(defn app [{:keys [redis] :as system}]
  (->
    (routes
      (GET "/feeds" []
           (views/list-feeds (feed/all-feed-ids redis)))
      (GET "/feeds/new" []
           (views/feed-edit nil))
      (GET "/feeds/:id/edit" [id]
           (if-let [feed (feed/get-feed redis id)]
             (views/feed-edit (:urls feed))))
      (GET "/feeds/:id" [id]
           (if-let [feed (feed/get-feed redis id)]
             (views/feed-view id feed (map fetcher/get-feed (repeat redis) (:urls feed)))))
      (HEAD "/feeds/:id" [id]
            (if (feed/feed-exists? redis id)
              (response "")
              (resp/not-found "")))
      (POST "/feeds" {{:keys [urls name]} :params headers :headers}
            (debugf "Creating feed from URLs: %s" urls)
            (when (coll? urls)
              (create-feed redis headers name urls)))

      (GET "/random" []
           (resp/redirect "/")) ; TODO grab a random Fab url
      (GET "/" [] (views/index-page (feed/number-of-feeds redis) []))
      (route/resources "/")
      (route/not-found "Not found."))
    compojure.handler/site))
