(ns prefab.views
  (:require [hiccup.core :refer :all]
            [hiccup.element :refer [unordered-list ordered-list]]
            [hiccup.page :as page :refer [include-css include-js]]
            [hiccup.form :as form]
            ))

(defmacro defpage
  [page-name page-vars & content]
  "Creates a page with a common shell around it"
  `(defn ~page-name ~page-vars
     (page/html5
       [:head
        [:meta {:charset "utf-8"}]
        [:title "Prefab"]
        [:meta {:name "description" :content "RSS Feed aggregation service"}]
        [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
        (include-css "/lib/bootstrap.min.css")
        (include-js "//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js")
        [:script {:type "text/javascript" :src "/lib/require.js" :data-main "/js/main"}]]
       [:body
        [:header {:class "navbar navbar-default" :role "banner"}
         [:a {:href "/" :class "navbar-brand"} "Prefab"]]
        [:div {:role "main" :class "container"} ~@content]])))

(defpage index-page
  [feed-count random-feeds]
  [:h1 "Welcome to Prefab"]
  [:span feed-count " feeds and counting"]
  (unordered-list {:class "list-unstyled"} (map #(vector :a {:href (:link %)} (:title %)) random-feeds))
  [:a {:class "btn btn-primary" :href "/feed"} "Create New Feed"]
  "&nbsp;"
  [:a {:class "btn btn-primary" :href "/random"} "Go to random feed"])

(defn entry
  "Renders a specific entry within a given feed"
  [entry]
  [:article {:class "feed-entry"}
   [:h2 (:title entry)]
   [:div (:content entry)]])

(defpage feed-view
  [feed]
  [:h1 (:title feed)]
  (ordered-list {:class "list-unstyled"} (map entry (:entries feed))))

(defpage feed-edit
  [parent-feed]
  (form/form-to {:id "feed-create"} [:post "/feed"]
                [:div {:class "form-group"}
                 [:label {:for "feed-name"} "Feed Name"]
                 (form/text-field {:class "form-control" :id "feed-name"} "Feed[name]")]
                [:div {:class "form-group"}
                 [:label {:for "feed-urls"} "RSS Feeds"]
                 (form/text-area {:class "form-control" :id "feed-urls" :rows 8} "Feed[urls]")]
                [:button {:type "submit" :class "btn btn-success pull-right"} "Create"]))
