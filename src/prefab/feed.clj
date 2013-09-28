(ns prefab.feed
  (:require [taoensso.carmine :as car :refer (wcar)]
            [prefab.fetcher :as fetcher]
            [taoensso.timbre :refer (error)]
            [org.httpkit.client :as http]))

(defn feed-id [urls]
  (-> urls set hash))

(defn number-of-feeds
  [redis]
  (count
    (wcar redis
      (car/keys "prefab:feed*"))))

(defn feed-key [id]
  (str "prefab:feed:" id))

(defn feed-urls [id]
  (car/smembers (feed-key id)))

(defn validate-feed
  "A quick - hacky way of validating if the feed is valid or not"
  [url]
  (let [data (http/get url)]
    (boolean (and
               (= (:status @data) 200)
               (some #(re-find % (:body @data)) [#"<item>" #"<entry>"])))))

(defn validate-feeds
  [urls]
  (every? validate-feed urls))

(defn add-feed
  [key urls]
  (apply car/sadd key urls)
  (doseq [url urls]
    (fetcher/enqueue url)))

(defn create-feed
  ""
  [urls]
  (let [id (feed-id urls)
        k (feed-key id)]
    (car/del k)
    (cond
      (validate-feeds urls)
        (add-feed k urls)
      :else
        (error "feed not valid"))
    id))
