(ns prefab.views
  (:require [hiccup.core :refer :all]
            [hiccup.page :as page :refer [include-css]]
            ))

(defmacro defpage
  [page-name & content]
  "Creates a page with a common shell around it"
  `(defn ~page-name []
     (page/html5
       [:head
        [:meta {:charset "utf-8"}]
        [:title "Prefab"]
        [:meta {:name "description" :content "RSS Feed aggregation service"}]
        [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
        (include-css "/lib/bootstrap.min.css")]
       [:body ~@content])))

(defpage index-page
  "Hello, prefab-world!")
