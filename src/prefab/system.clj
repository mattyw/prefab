(ns prefab.system
  (:require [prefab.routes :as routes]
            [prefab.fetcher :as fetcher]
            [prefab.refresher :as refresher]
            [prefab.util :as util :refer (int*)]
            [taoensso.timbre :as timbre :refer (error warn info infof)]
            [clojure.string :as str]
            [environ.core :refer (env)]
            [org.httpkit.server :refer (run-server)]))

(defrecord Prefab [server handler port redis fetchers refresher])

;; server

(defn- start-server [{:keys [handler port] :as system}]
  (infof "Starting HTTP server on port %d" port)
  (assoc system :server (run-server handler {:port port})))

(defn- stop-server [{:keys [server] :as system}]
  (info "Shutting down HTTP server...")
  (when server
    (server))
  (assoc system :server nil))

;; fetchers

(defn- start-fetchers [{:keys [redis] :as system}]
  (assoc system :fetchers
         (doall (repeatedly (env :num-fetchers 1)
                            #(fetcher/fetcher redis)))))

(defn- stop-fetchers [{:keys [fetchers] :as system}]
  (infof "Stopping %d fetcher(s)..." (count fetchers))
  (doseq [fetcher fetchers]
    (fetcher/stop-fetcher fetcher))
  (assoc system :fetchers nil))

;; refresher

(defn- start-refresher [{:keys [redis] :as system}]
  (->> (refresher/refresher {:redis redis
                             :refresh-fn fetcher/enqueue})
       (refresher/start-refresher)
       (assoc system :refresher)))

(defn- stop-refresher [{:keys [refresher] :as system}]
  (when refresher
    (refresher/stop-refresher refresher))
  (assoc system :refresher nil))

(defn refresh-feeds [{:keys [redis] :as system}]
  (info "Enqueuing refreshes for all existing feeds (if any)...")
  (fetcher/fetch-all redis)
  system)

;; all together now

(defn start [system]
  (-> system
      (assoc :handler (routes/app system))
      start-server
      start-fetchers
      start-refresher
      refresh-feeds))

(defn stop [system]
  (-> system
      stop-refresher
      stop-fetchers
      stop-server
      (assoc :handler nil)))

(defn system [port]
  (map->Prefab {:port (int* (env :port port))
                :redis {:pool {}
                        :spec {:host (env :redis-host "127.0.0.1")
                               :port (int* (env :redis-port 6379))}}}))

(defn -main [& args]
  (timbre/set-level! (-> (env :log-level "WARN") str/lower-case keyword))
  (start (system 8080)))
