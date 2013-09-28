(ns prefab.system
  (:require [prefab.routes :as routes]
            [prefab.fetcher :as fetcher]
            [taoensso.timbre :refer (error warn info infof)]
            [environ.core :refer (env)]
            [org.httpkit.server :refer (run-server)]))

(defrecord Prefab [server handler port redis fetchers])

(defn- start-handler [system]
  (assoc system :handler (routes/app system)))

(defn- start-server [{:keys [handler port] :as system}]
  (infof "Starting HTTP server on port %d" port)
  (assoc system :server (run-server handler {:port port})))

(defn- stop-server [{:keys [server] :as system}]
  (info "Shutting down HTTP server...")
  (when server
    (server))
  (assoc system :server nil))

(defn- start-fetchers [{:keys [redis] :as system}]
  (assoc system :fetchers
         (doall (repeatedly (env :num-fetchers 1)
                            #(fetcher/fetcher redis)))))

(defn- stop-fetchers [{:keys [fetchers] :as system}]
  (infof "Stopping %d fetcher(s)..." (count fetchers))
  (doseq [fetcher fetchers]
    (fetcher/stop-fetcher fetcher))
  (assoc system :fetchers nil))

(defn start [system]
  (-> system
      start-handler
      start-server
      start-fetchers))

(defn stop [system]
  (-> system
      (assoc :handler nil)
      stop-server
      stop-fetchers))

(defn system [port]
  (map->Prefab {:port (env :port port)
                :redis {:pool {}
                        :spec {:host (env :redis-host "127.0.0.1")
                               :port (env :redis-port 6379)}}}))

(defn -main []
  (-> (system 8080)
      start-handler
      start-server
      start-fetchers))
