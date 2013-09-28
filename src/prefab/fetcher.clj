(ns prefab.fetcher
  (:require [taoensso.carmine :as car :refer (wcar)]
            [taoensso.carmine.message-queue :as mq]
            [taoensso.timbre :refer (error warn info)]
            [clj-rome.reader :refer (build-feed)]
            [clj-rome.fetcher :refer (build-url-fetcher with-fetcher retrieve-feed)])
  (:import [java.io IOException]))

(def qname "prefab:fetcher")
(def max-attempts 3)

(defn url-key [url] (str "prefab:url:" url))

(defn get-feed [redis url]
  (wcar redis
        (car/get (url-key url))))

(defn enqueue [redis url]
  (wcar redis
        (mq/enqueue qname url)))

(defn extract-entry [entry]
  (-> entry
      (select-keys [:link :contributors :author :authors
                    :title :uri :update-date :categories :links])
      (assoc :description (get-in entry [:description :value]))
      (assoc :title-ex (get-in entry [:title-ex :value]))))

(defn fetch [url]
  (let [feed (build-feed url)]
    (->
      (select-keys feed [:uri :title :author :authors :description :encoding])
      (assoc :entries (map extract-entry (:entries feed))))))

(defn fetcher-handler
  [redis {url :message attempts :attempts}]
  (try
    (info "Fetching feed: %s" url)
    (wcar redis
          (car/set (url-key url) (fetch url)))
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
