(ns prefab.ajax
  (:require [clojure.data.json :as json]
            [ring.util.response :as resp]
            ))

(defn- build-response
  "Create a JSON-formatted response"
  [data]
  (-> (resp/response (json/write-str data))
      (resp/content-type "application/json")))

(defn response
  "Create an appropriately-structured JSON payload for AJAX requests"
  [data]
  (build-response {:status "ok" :data data}))

(defn redirect
  "Create an appropriately-structured JSON payload for redirect via AJAX"
  [destination]
  (build-response {:status "ok" :location destination}))
