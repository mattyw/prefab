(ns prefab.system
  (:require [prefab.routes :as routes]
            [environ.core :refer (env)]
            [org.httpkit.server :refer [run-server]]))

(defrecord Prefab [server handler port redis])

(defn- start-handler [system]
  (assoc system :handler (routes/app system)))

(defn- start-redis [system]
  (assoc system :redis {:pool {} :spec {:host "127.0.0.1" :port 6379}}))

(defn- start-server [{:keys [handler port] :as system}]
  (assoc system :server (run-server handler {:port port})))

(defn- stop-server [{:keys [server] :as system}]
  (when server
    (server))
  (assoc system :server nil))

(defn start [system]
  (-> system
      start-redis
      start-handler
      start-server))

(defn stop [system]
  (-> system
      (assoc :redis nil :handler nil)
      stop-server))

(defn system []
  (map->Prefab {:port (env :port 8080)}))
