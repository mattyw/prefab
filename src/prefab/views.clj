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
  [:p {:class "lead"} "Prefab is an RSS-feed aggregator. Create a feed with your list of RSS URLs to get a chronologically ordered list."]
  [:p "Prefab is a new way of doing rss feed aggregation."]
  [:ul
    [:li "You create a prefab feed with a list of existing rss feeds."]
    [:li "All prefab feeds are public"]
    [:li "All feeds are immutable, so editing existing feeds simply creates a new feed."]
    [:li "New feeds can be created by combining 2 or more existing feeds."]
    [:li "Because all feeds are public and immutable there's no need to signup."]]
  [:span feed-count " feeds and counting"]
  (unordered-list {:class "list-unstyled"} (map #(vector :a {:href (:link %)} (:title %)) random-feeds))
  [:a {:class "btn btn-primary" :href "/feed"} "Create New Feed"]
  "&nbsp;"
  [:a {:class "btn btn-primary" :href "/random"} "Go to random feed"])

(defn entry
  "Renders a specific entry within a given feed"
  [[entry source]]
  [:article.feed-entry.panel.panel-default
   [:div.panel-heading
    [:h2.panel-title
     [:small.pull-right (:title source) " at " (:published-date entry)]
     (:title entry)]]
   [:div.panel-body (:content entry)]])

(defpage feed-view
  [id feeds]
  (let [entries (mapcat #(map vector (:entries %) (repeat %)) feeds)]
    (list
      [:a {:href (str "/feed/edit/" id)} "(edit)"]
      (ordered-list {:class "list-unstyled"} (map entry (->> entries
                                                             (sort-by #(:published-date (first %)))
                                                             reverse))))))

(defpage feed-edit
  [parent-feed-urls]
  (form/form-to {:id "feed-create"} [:post "/feed"]
                ;[:div {:class "form-group"}
                ; [:label {:for "feed-name"} "Feed Name"]
                ; (form/text-field {:class "form-control" :id "feed-name"} "Feed[name]")]
                [:div.form-group
                 [:label {:for "feed-urls"} "RSS Feeds"]
                 (form/text-area {:class "form-control" :id "feed-urls" :rows 8} "Feed[urls]"
                                 (when-not (nil? parent-feed-urls) (clojure.string/join "\n" parent-feed-urls)))]
                [:button.btn.btn-success.pull-right {:type "submit"} "Create"]))
