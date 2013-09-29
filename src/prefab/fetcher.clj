(ns prefab.fetcher
  (:require [prefab.util :refer (min->ms)]
            [prefab.feed-source :as feed-source]
            [prefab.refresher :as refresher]
            [taoensso.carmine :as car :refer (wcar)]
            [taoensso.carmine.message-queue :as mq]
            [taoensso.timbre :refer (error warn info infof)]
            [clj-rome.reader :refer (build-feed)]
            [clj-rome.fetcher :refer (build-url-fetcher with-fetcher retrieve-feed)])
  (:import [java.io IOException]))

(def qname "fetcher")
(def hkey-urls (car/key "prefab" "url"))
(def max-attempts 3)
(def refresh-interval (min->ms 5))

(defn get-feed
  ([redis url] (wcar redis (get-feed url)))
  ([url] (car/hget hkey-urls url)))

(defn has-feed? [redis url]
  (= 1 (wcar redis (car/hexists hkey-urls url))))

(defn all-urls [redis]
  (wcar redis (car/hkeys hkey-urls)))

(defn enqueue [url] (mq/enqueue qname url))

(defn clear-queue [redis] (mq/clear-queues redis qname))

(defn fetch-all [redis]
  (when-let [urls (all-urls redis)]
    (wcar redis (doseq [url urls] (enqueue url)))))

(defn fetch
  "Fetches a feed, caches it, then schedules it to be refreshed"
  [redis url]
  (infof "Fetching feed: %s" url)
  (wcar redis
        (car/hset hkey-urls url (-> url build-feed feed-source/parse-feed))
        (refresher/refresh-in refresh-interval url)))

(defn fetcher-handler
  [redis {url :message attempts :attempts}]
  (try
    (fetch redis url)
    {:status :success}
    (catch IOException e
      (if (< attempts max-attempts)
        (do
          (warn "Failed to fetch feed (attempt %d of %d)" attempts max-attempts)
          {:status :retry})
        (do
          (error "Failed to fetch feed after %d attempts" max-attempts)
          {:status :error})))
    (catch Exception e
      (error e "Failed to fetch feed")
      {:status :error})))

(defn fetcher [redis]
  (info "Starting fetcher worker")
  (mq/worker redis qname
             {:handler (partial fetcher-handler redis)
              :eoq-backoff-ms (fn [druns] (mq/exp-backoff druns {:max 5000}))}))

(defn stop-fetcher [fetcher]
  (when fetcher (mq/stop fetcher)))
