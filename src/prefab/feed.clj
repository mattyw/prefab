(ns prefab.feed
  (:require [taoensso.carmine :as car]))

(defn feed-id [urls]
  (-> urls set hash))

(defn feed-key [id]
  (str "prefab:feed:" id))

(defn feed-urls [id]
  (car/smembers (feed-key id)))

(defn create-feed
  ""
  [urls]
  (let [id (feed-id urls)
        k (feed-key id)]
    (car/del k)
    (apply car/sadd k urls)
    id))
