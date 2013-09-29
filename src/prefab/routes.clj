(ns prefab.routes
  (:require [compojure.core :refer :all]
            [compojure.handler]
            [compojure.route :as route]
            [ring.util.response :as resp :refer (response redirect-after-post)]
            [taoensso.carmine :as car :refer (wcar)]
            [taoensso.timbre :refer (debug debugf)]
            [prefab.views :as views]
            [prefab.feed :as feed]
            [prefab.feed-source :as feedsrc]
            [prefab.ajax :as ajax]
            [prefab.fetcher :as fetcher]
            [prefab.util :refer (int*)]
            [clojure.data.json :as json]
            ))

(defn feed-url [id] (str "/feeds/" id))

(defn parse-feed-id
  "If url is a Prefab feed, returns the id, otherwise nil"
  [host url]
  (let [pattern (str "(?:" host "/feeds/(-?\\d+))$|^(-?\\d+)$")
        matches (re-find (re-pattern pattern) url)]
    (or (second matches)
        (-> matches next next first)))) ;; 3rd

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
    (if-let [[feed-id created?] (feed/create-feed redis name urls)]
      (-> (ajax/redirect (feed-url feed-id))
          (assoc :flash (if created?
                          {:type :success :message "Feed created!"}
                          {:type :error :message "A feed with those URLs already exists!"})))
      (ajax/error "Failed to create feed"))))

(defn app [{:keys [redis] :as system}]
  (->
    (routes
      (GET "/feeds" {:keys [query-params]}
           (let [page (int* (get query-params "page" 0))
                 size (min (int* (get query-params "size" 50)) 100)
                 feeds (feed/get-feeds redis size page)
                 num-feeds (feed/number-of-feeds redis)
                 prev-page (if (pos? page) (format "/feeds?page=%d&size=%d" (dec page) size))
                 next-page (if (< (* (inc page) size) num-feeds) (format "/feeds?page=%d&size=%d" (inc page) size))]
             (views/list-feeds feeds prev-page next-page)))
      (GET "/feeds/new" []
           (views/feed-edit nil))
      (GET "/feeds/random" []
           (if-let [id (feed/rand-feed-id redis)]
             (-> (resp/redirect (feed-url id))
                 (resp/status 307))
             (-> (resp/redirect "/")
                 (resp/status 307)
                 (assoc :flash "No feeds exist!"))))
      (GET "/feeds/:id/edit" [id]
           (if-let [feed (feed/get-feed redis id)]
             (views/feed-edit (:urls feed))))
      (GET "/feeds/:id" [id]
           (if-let [feed (feed/get-feed redis id)]
             (views/feed-view id feed (map (comp feedsrc/parse-feed fetcher/get-feed) (repeat redis) (:urls feed)))))
      (HEAD "/feeds/:id" [id]
            (if (feed/feed-exists? redis id)
              (response "")
              (resp/not-found "")))
      (POST "/feeds" {{:keys [urls name]} :params headers :headers}
            (debugf "Creating feed from URLs: %s" urls)
            (when (coll? urls)
              (create-feed redis headers name urls)))
      (GET "/feed-name-exists" {{name "name"} :query-params}
           (if (feed/feed-name-exists? redis name) "true" "false"))
      (GET "/" [] (views/index-page (feed/number-of-feeds redis) []))
      (route/resources "/")
      (route/not-found "Not found."))
    views/wrap-render-flash
    compojure.handler/site))
