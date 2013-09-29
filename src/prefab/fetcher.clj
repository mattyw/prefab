(ns prefab.fetcher
  (:require [prefab.util :refer (min->ms)]
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

(defn get-feed [redis url]
  (wcar redis (car/hget hkey-urls url)))

(defn has-feed? [redis url]
  (= 1 (wcar redis (car/hexists hkey-urls url))))

(defn enqueue [url] (mq/enqueue qname url))

(defn clear-queue [redis] (mq/clear-queues redis qname))

(defn- extract-entry [entry]
  (-> entry
      (select-keys [:link :contributors :author :authors :title :uri
                    :published-date :categories :links])
      (assoc :content (get-in entry [:description :value]))
      (assoc :title-ex (get-in entry [:title-ex :value]))))

(defn- extract-feed [feed]
  (-> feed
      (select-keys [:uri :title :author :authors :description :encoding])
      (assoc :entries (map extract-entry (:entries feed)))))

(defn fetch
  "Fetches a feed, caches it, then schedules it to be refreshed"
  [redis url]
  (infof "Fetching feed: %s" url)
  (wcar redis
        (car/hset hkey-urls url (-> url build-feed extract-feed))
        (refresher/refresh-in refresh-interval url)))

(defn fetcher-handler
  [redis {url :message attempts :attempts}]
  (try
    (fetch redis url)
    {:status :success}
    (catch IOException e
      (if (< attempts max-attempts)
        (do
          (warn "Failed to fetch feed (atttempt %d of %d)" attempts max-attempts)
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
             {:handler (partial fetcher-handler redis)}))

(defn stop-fetcher [fetcher]
  (when fetcher (mq/stop fetcher)))
