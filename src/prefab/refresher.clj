(ns prefab.refresher
  (:require [taoensso.carmine :as car :refer (wcar)]
            [taoensso.timbre :refer (error warn info infof debugf)])
  (:import [java.util.concurrent Executors TimeUnit]))

(def qname "prefab:refresher")
(def zkey "prefab:refresher-urls") ;; redis sorted set
(def default-interval 10000) ;; 10sec

(defrecord Refresher [redis pool refresh-fn fetch-limit interval])

(defn- now [] (System/currentTimeMillis))

(defn refresh-at
  ([redis t url]
   (wcar redis (refresh-at t url)))
  ([t url]
   (debugf "Scheduled refresh for %s at %d" url t)
   (car/zadd zkey t url)))

(defn refresh-in
  ([redis delay url]
   (wcar redis (refresh-in delay url)))
  ([delay url] (refresh-at (+ delay (now)) url)))

(defn get-expired [redis limit]
  (wcar redis (car/zrangebyscore zkey 0 (now) "LIMIT" 0 limit)))

(defn refresh-expired
  "Finds items that need to be refreshed and enqueues them to fetcher"
  [redis fetch-limit refresh-fn]
  (let [urls (get-expired redis fetch-limit)
        t (now)]
    (when (seq urls)
      (infof "Refreshing feeds: %s" urls))
    (wcar redis
          (doseq [url urls]
            (refresh-fn url)
            (car/zrem zkey url)))))

(defn clear-refresher
  "Clears scheduled refreshings"
  ([] (car/del zkey))
  ([redis] (wcar redis (clear-refresher))))

(defn refresher
  "Creates new refresher"
  [{:keys [redis pool refresh-fn fetch-limit interval num-threads]
    :or {num-threads 1 fetch-limit 30 interval default-interval}}]
  (map->Refresher {:redis redis
                   :pool pool
                   :refresh-fn refresh-fn
                   :fetch-limit fetch-limit
                   :interval interval
                   :num-threads num-threads}))

(defn- start-pool [{:keys [redis pool refresh-fn fetch-limit interval] :as refresher}]
  (infof "Starting refresher with threads=%d, interval=%dms" (.getCorePoolSize pool) interval)
  (.scheduleWithFixedDelay pool #(refresh-expired redis fetch-limit refresh-fn)
                           interval interval TimeUnit/MILLISECONDS)
  refresher)

(defn start-refresher [{:keys [pool redis num-threads] :as refresher}]
  (let [pool (or pool (Executors/newScheduledThreadPool num-threads))]
    (clear-refresher redis)
    ;; TODO initial refreshes
    (-> (assoc refresher :pool pool)
        (start-pool))))

(defn stop-refresher [{:keys [redis pool interval] :as refresher}]
  (info "Shutting down refresher...")
  (when pool
    (.shutdown pool)
    (when-not (.awaitTermination pool (+ 100 interval) TimeUnit/MILLISECONDS)
      (.shutdownNow pool)))
  (assoc refresher :pool nil))
