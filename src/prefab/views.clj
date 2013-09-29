(ns prefab.views
  (:require [hiccup.core :refer :all]
            [hiccup.element :refer (unordered-list ordered-list link-to)]
            [hiccup.page :as page :refer (include-css include-js)]
            [hiccup.form :as form]
            [ring.util.response :refer (response)]
            [clojure.string :as str]
            [taoensso.timbre :refer (debug debugf)]
            [prefab.util :refer (str-take)]
            [prefab.views.helpers :as helper]
            [prefab.feed-source :refer (title link content entries published-date feed?)]
            [prefab.feed :as feed]))

(defn feed-url [id] (str "/feeds/" id))
(defn feed-edit-url [id] (str "/feeds/" id "/edit"))
(defn feed-report-url [id] (str "/feeds/" id "/report"))

(def ^:dynamic *request* nil)

(defmacro with-request
  "Macro to capture current request in dynamic var for magically convenient things"
  [request & body]
  `(binding [*request* ~request]
     ~@body))

(defn wrap-with-request
  "Middleware to capture current request in dynamic var for magically convenient things"
  [handler]
  (fn [request]
    (with-request request
      (handler request))))

(defn format-title
  [title]
  (if title (str title " - Prefab") "Prefab"))

(defmacro defpage
  [page-name page-vars blocks]
  "Creates a function to render page with a common shell around it
  The function first argument is request"
  (let [blocks (if (map? blocks) blocks `{:content ~blocks})
        title (:title blocks "Prefab")]
    `(defn ~page-name ~page-vars
       (let [flash# (get *request* :flash)
             url# (get *request* :uri)
             title# (format-title ~(:title blocks))]
         (response
           (page/html5
             [:head
              [:meta {:charset "utf-8"}]
              [:title title#]
              [:meta {:name "description" :content "RSS Feed aggregation service"}]
              [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
              (when *request*
                (list [:meta {:property "og:url" :content (get *request* :uri)}]
                      [:meta {:property "og:type" :content "website"}]
                      [:meta {:property "og:title" :content title#}]
                      [:meta {:property "og:image" :content ""}]))
              (include-css "/lib/bootstrap.min.css")
              (include-css "/css/prefab.css")
              (include-js "//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js")
              (include-js "/js/twitter.js")
              (include-js "/lib/bootstrap.min.js")
              [:script {:type "text/javascript" :src "/lib/require.js" :data-main "/js/main"}]
              ~(:head blocks)]
             [:body
              [:div#wrap
               [:header.navbar.navbar-default {:role "banner"}
                [:div.navbar-header
                 [:button.navbar-toggle {:type "button" :data-toggle "collapse" :data-target ".prefab-navbar-collapse"}
                  [:span.sr-only "Toggle navigation"]
                  (repeat 3 [:span.icon-bar])]
                 [:a.navbar-brand {:href "/"} "Prefab"]
                 [:a.btn.btn-success.navbar-btn.pull-left {:href "http://clojurecup.com/app.html?app=prefab"} "Vote for us!"]]
                [:nav.collapse.navbar-collapse.prefab-navbar-collapse {:role "navigation"}
                 [:ul.nav.navbar-nav
                  [:li [:a {:href "/feeds/new"} "New Feed"]]
                  [:li [:a {:href "/feeds"} "Browse Feeds"]]
                  [:li [:a {:href "/feeds/random"} "Random Feed"]]]]]
               [:div#main.container {:role "main"}
                (when flash# [:div {:role "flash" :class (str "alert alert-" ({:success "success" :error "danger"} (:type flash#)))} (:message flash#)])
                ~(:content blocks)]]
              [:footer.navbar.text-center
               [:small.text-muted.authors "Created for ClojureCup by Matty Williams, Sam Carter, Logan Linn, and Scott Rabin"]
               [:div {:class "panel-body"}
                [:a
                 {:href "https://twitter.com/share"
                  :class "twitter-share-button"
                  :data-url "http://prefab.clojurecup.com"
                  :data-text "Prefab: A new way of doing RSS feed aggregation"
                  :data-hashtags "clojurecup"} "Tweet"]]
               ]]))))))

(defpage index-page
  [feed-count random-feeds]
  {:head (list [:meta {:property "og:video" :content "//www.youtube.com/embed/kab9yAnHkwE"}])
   :title "Prefab"
   :content
   (list [:h1 "Welcome to Prefab"]
         [:p {:class "lead"} "Prefab is a new way of doing RSS feed aggregation."]
         [:p "It's new because:"]
         [:ul
          [:li "All prefab feeds are public"]
          [:li "All feeds are immutable, so editing existing feeds creates a new feed."]
          [:li "New feeds can be created by combining 2 or more existing feeds."]
          [:li "Because all feeds are public and immutable there's no need to signup."]]
         [:h2 [:a {:href "/gettingstarted"} "Watch our getting started video"]]
         [:span feed-count " feeds and counting"]
         (unordered-list {:class "list-unstyled"} (map #(vector :a {:href (link %)} (title %)) random-feeds))
         [:a {:class "btn btn-primary" :href "/feeds/new"} "Create New Feed"]
         "&nbsp;"
         [:a {:class "btn btn-primary" :href "/feeds"} "List all feeds"]
         "&nbsp;"
         [:a {:class "btn btn-primary" :href "/feeds/random"} "Go to random feed"]
         [:br]
         )})

(defpage getting-started
  []
  [:iframe {:width "960" :height "720" :src "//www.youtube.com/embed/sGV6Af0gGjo" :frameborder "0" :allowfullscreen "true"}])

(defn entry
  "Renders a specific entry within a given feed"
  [[entry source]]
  [:article.feed-entry.panel.panel-default
   [:div.panel-heading
    [:h2.panel-title
     [:small.pull-right (title source) " at " (published-date entry)]
     [:a {:href (link entry)} (title entry)]]]
   [:div.panel-body (content entry)]])

(defpage feed-view
  [id {:keys [urls name] :as feed} feeds]
  {:title (when name (if (<= (count name) 8)
                       name
                       (str (str-take 8 name) "...")))
   :content
   (let [feed-entries (mapcat #(map vector (entries %) (repeat %)) (filter feed? feeds))
         feed-name (if (empty? name) "(No name)" name)]
     (list
       [:div.social-sharing.pull-right.h1
        (helper/share-twitter (feed-url id)
                              (if (empty? name)
                                "Check out this RSS feed! #prefab #clojurecup"
                                (format "Check out this RSS feed, %s! #prefab #clojurecup" feed-name)))
        (helper/share-facebook (feed-url id)
                               (if (empty? name)
                                 (str "Check out this RSS feed on Prefab! " (helper/full-url (feed-url id)))
                                 (format "Check out this RSS feed, %s! %s" feed-name (feed-url id))))]
       [:h1 feed-name " "
        [:small.text-vmiddle [:a.glyphicon.glyphicon-plus {:href (feed-edit-url id)
                                                           :title (str "Create a new feed based on " feed-name)}]]
        [:small.text-vmiddle [:a.glyphicon.glyphicon-flag {:href (feed-report-url id)
                                                           :title "Report feed"}]]]
       (ordered-list {:class "list-unstyled"} (map entry (->> feed-entries
                                                              (sort-by #(published-date (first %)))
                                                              reverse)))))})

(defpage list-feeds
  [feeds prev-page next-page]
  {:title "Feeds"
   :content
   (list
     [:h1 "Feeds"]
     (if (empty? feeds)
       [:div "No feeds found! Be the first to " [:a {:href "/feeds/new"} "create one!"]])
     [:ul.list-unstyled.row {} (map (fn [[id {:keys [name]}]]
                                      [:li.col-md-4.col-sm-6 (link-to {} (feed-url id) (or name "(no name)"))])
                                    feeds)]
     (when prev-page [:a {:href prev-page} "< Prev"])
     (when next-page [:a {:href next-page} "Next >"]))})

(defpage reported-feeds
  [feeds]
  (ordered-list {} feeds))

(defpage reported-thanks
  []
  "Thank you for reporting this feed. Our admins will looks into it")

(defpage feed-edit
  [feed-urls]
  {:title (if (empty? feed-urls) "New Feed" "Edit Feed")
   :content
   (form/form-to {:id "feed-create"} [:post "/feeds"]
                 [:div {:class "form-group"}
                  [:label {:for "feed-name"} "Feed Name"]
                  (form/text-field {:id "feed-name"
                                    :class "form-control"
                                    :placeholder "(optional)"
                                    :maxlength feed/max-len-feed-name}
                                   "Feed[name]")]
                 [:div.form-group
                  [:label {:for "feed-urls"} "RSS Feeds"]
                  (form/text-area {:class "form-control" :id "feed-urls" :rows 8} "Feed[urls]"
                                  (when (seq feed-urls) (str/join "\n" feed-urls)))]
                 [:button.btn.btn-success.pull-right {:type "submit"} "Create"])})
