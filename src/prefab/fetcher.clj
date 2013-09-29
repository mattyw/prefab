(ns prefab.fetcher
  (:require [prefab.util :refer (min->ms)]
            [prefab.feed-source :as feed]
            [prefab.refresher :as refresher]
            [taoensso.carmine :as car :refer (wcar)]
            [taoensso.carmine.message-queue :as mq]
            [taoensso.timbre :refer (error warn info infof)]
            [clj-rome.reader :refer (build-feed)]
            [clj-rome.fetcher :refer (build-url-fetcher with-fetcher retrieve-feed)])
  (:import [java.io IOException]))

(def qname "prefab:fetcher")
(def max-attempts 3)
(def refresh-interval (min->ms 5))

(defn url-key [url] (str "prefab:url:" url))

(defn get-feed [redis url]
  (feed/parse-feed (wcar redis
                         (car/get (url-key url)))))

(defn enqueue [url] (mq/enqueue qname url))

(defn clear-queue [redis] (mq/clear-queues redis qname))

(defn fetch [url]
  (-> url build-feed feed/parse-feed))

(defn fetcher-handler
  [redis {url :message attempts :attempts}]
  (try
    (infof "Fetching feed: %s" url)
    (wcar redis
          (car/set (url-key url) (fetch url))
          (refresher/refresh-in refresh-interval url))
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
             {:handler (partial fetcher-handler redis)}))

(defn stop-fetcher [fetcher]
  (when fetcher (mq/stop fetcher)))
