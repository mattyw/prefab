(ns prefab.feed
  (:require [taoensso.carmine :as car]
            [prefab.fetcher :as fetcher]
            [org.httpkit.client :as http]))

(defn feed-id [urls]
  (-> urls set hash))

(defn feed-key [id]
  (str "prefab:feed:" id))

(defn feed-urls [id]
  (car/smembers (feed-key id)))

(defn validate-feed
  "A quick - hacky way of validating if the feed is valid or not"
  [url]
  (let [data (http/get url)]
  (cond
    (= (:status @data) 200)
      (re-find #"<entry>" (:body @data))
    :else
      nil)))

(defn validate-feeds
  [urls]
  (let [validated (map validate-feed urls)]
      (every? (fn [url] (= "<entry>" url)) validated)))

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
        (println "feed not valid"))
    id))
