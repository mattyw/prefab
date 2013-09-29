(ns prefab.ajax
  (:require [cheshire.core :as json]
            [ring.util.response :as resp]
            ))

(defn- build-response
  "Create a JSON-formatted response"
  [data]
  (-> (resp/response (json/generate-string data))
      (resp/content-type "application/json")))

(defn response
  "Create an appropriately-structured JSON payload for AJAX requests"
  [data]
  (build-response {:status "ok" :data data}))

(defn error
  "Create an appropriately-structured JSON payload for error message via AJAX"
  ([message] (error message nil))
  ([message data]
   (cond-> {:status "error" :message message}
     data (assoc :data data)
     true (build-response))))

(defn redirect
  "Create an appropriately-structured JSON payload for redirect via AJAX"
  [destination]
  (build-response {:status "ok" :location destination}))
