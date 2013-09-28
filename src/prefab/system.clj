(ns prefab.system
  (:require [prefab.routes :as routes]
            [prefab.fetcher :as fetcher]
            [environ.core :refer (env)]
            [org.httpkit.server :refer (run-server)]))

(defrecord Prefab [server handler port redis fetcher])

(defn- start-handler [system]
  (assoc system :handler (routes/app system)))

(defn- start-server [{:keys [handler port] :as system}]
  (assoc system :server (run-server handler {:port port})))

(defn- stop-server [{:keys [server] :as system}]
  (when server
    (server))
  (assoc system :server nil))

(defn- start-fetcher [{:keys [redis] :as system}]
  (assoc system :fetcher (fetcher/fetcher redis)))

(defn- stop-fetcher [{:keys [fetcher] :as system}]
  (fetcher/stop-fetcher fetcher)
  (assoc system :fetcher nil))

(defn start [system]
  (-> system
      start-handler
      start-server
      start-fetcher))

(defn stop [system]
  (-> system
      (assoc :handler nil)
      stop-server
      stop-fetcher))

(defn system []
  (map->Prefab {:port (env :port 8080)
                :redis {:pool {}
                        :spec {:host (env :redis-host "127.0.0.1")
                               :port (env :redis-port 6379)}}}))
