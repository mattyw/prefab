(ns prefab.feed
  (:require [taoensso.carmine :as car :refer (wcar)]
            [clojure.string :as str]
            [prefab.fetcher :as fetcher]
            [taoensso.timbre :refer (error)]
            [org.httpkit.client :as http]))

(def hkey-feeds (car/key "prefab" "feeds"))
(def hkey-names (car/key "prefab" "feed-names"))
(def lkey-ids (car/key "prefab" "feed-ids"))

(defn feed-id [urls]
  (-> urls set hash))

(defn- normalize-name [name]
  (if-let [name (if (string? name) (str/trim name))]
    (if-not (str/blank? name)
      name)))

(defn name-key [name]
  (-> name
      (str/replace #"\s+" "-")
      (str/lower-case)
      (car/key)))

(defn all-feed-ids
  ([redis] (wcar redis (all-feed-ids)))
  ([] (car/hkeys hkey-feeds)))

(defn get-feeds
  "Returns map of feeds"
  ([redis limit] (get-feeds redis limit 0))
  ([redis limit offset]
   (if-let [ids (seq (wcar redis (car/lrange lkey-ids offset (dec (+ offset limit)))))]
     (->> (wcar redis (apply car/hmget hkey-feeds ids))
          (zipmap ids)))))

(defn number-of-feeds ;; TODO rename to num-feeds
  [redis]
  (wcar redis (car/hlen hkey-feeds)))

(defn valid-url?
  "A quick - hacky way of validating if the feed is valid or not"
  [url]
  (let [data (http/get url)] ;; TODO use HEAD request
    (boolean (and
               (= (:status @data) 200)
               (some #(re-find % (:body @data)) [#"<item>" #"<entry>"])))))

(defn valid-urls? [urls] (every? valid-url? urls)) ;; TODO parallelize

(defn valid-name?
  ([redis name] (wcar redis (valid-name? name)))
  ([name] (car/hexists hkey-names (name-key name))))

(defn feed-exists?
  ([redis id] (wcar redis (feed-exists? id)))
  ([id] (not (zero? (car/hexists hkey-feeds id)))))

(defn get-feed
  ([redis id] (wcar redis (get-feed id)))
  ([id] (when id (car/hget hkey-feeds id))))

(defn get-feed-by-name
  ([redis name] (wcar redis (get-feed-by-name name)))
  ([name]
   (car/lua
     "local id = redis.call('hget', _:hkey-names, _:name)
     if id then
       return redis.call('hget', _:hkey-feeds, id)
     end"
     {:hkey-names hkey-names
      :hkey-feeds hkey-feeds}
     {:name (name-key name)})))

(defn create-feed
  [redis name urls]
  (let [id (feed-id urls)
        feed {:name (normalize-name name) :urls urls}
        result (wcar redis
                     (car/lua
                       "local current_id = redis.call('hget', _:hkey-names, _:name)
                        if current_id then
                          if current_id == _:id then return 0
                          else return 'invalid-name' end
                        end
                        if redis.call('hsetnx', _:hkey-feeds, _:id, _:feed) == 0 then
                          return 0
                        end
                        if _:name ~= '' then
                          redis.call('hset', _:hkey-names, _:name, _:id)
                        end
                        redis.call('lpush', _:lkey-ids, _:id)
                        return 1"
                       {:hkey-names hkey-names
                        :hkey-feeds hkey-feeds
                        :lkey-ids   lkey-ids}
                       {:id id
                        :feed (car/freeze feed)
                        :name (name-key name)}))]
    (when (not= result "invalid-name")
      (doseq [url urls]
        (try
          (if-not (fetcher/has-feed? redis url)
            (fetcher/fetch redis url))
          (catch Exception e
            (error e "Failed to pre-fetch URL:" url))))
      (wcar redis
            (car/save))
      [id (not (zero? result))])))
