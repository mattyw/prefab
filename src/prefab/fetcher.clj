(ns prefab.fetcher
  (:require [taoensso.carmine :as car :refer (wcar)]
            [taoensso.carmine.message-queue :as mq]
            [taoensso.timbre :refer (error warn info)]
            [clj-rome.reader]
            [clj-rome.fetcher :refer (build-url-fetcher with-fetcher retrieve-feed)])
  (:import [java.io IOException]))

(def qname "fetcher")
(def max-attempts 3)

(defn enqueue [redis url]
  (wcar redis
        (mq/enqueue qname url)))

(defn fetcher-handler
  [redis url-fetcher {:keys [message attempts]}]
  (try
    (info "Fetching feed: %s" message)
    ;(with-fetcher url-fetcher
      ;(retrieve-feed message))
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
             {:handler (partial fetcher-handler
                                redis
                                nil
                                ;(build-url-fetcher :disk "/tmp/prefab-cache")
                                )})) ;; TODO add cache

(defn stop-fetcher [fetcher]
  (when fetcher (mq/stop fetcher)))
