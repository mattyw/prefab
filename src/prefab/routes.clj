(ns prefab.routes
  (:require [compojure.core :refer :all]
            [compojure.handler]
            [compojure.route :as route]
            [ring.util.response :as resp :refer (response)]
            [prefab.views :as views]
            [prefab.feed :as feed]
            ))

(def feeds [{:feedUrl "http://prefab.com/feed/feed-1.rss"
             :title "Feed 1"
             :link "/feed/feed-1"
             :description "This is the description of Feed #1"
             :author "Scott Rabin"
             :entries [{:mediaGroup nil
                        :title "Feed 1: First Entry"
                        :link "http://www.google.com"
                        :content "Feed 1: First Entry, with real contents"
                        :contentSnippet "... not important"
                        :publishedDate "13 Apr 2007 12:40:07 -0700"
                        :categories []}]}
            {:feedUrl "http://prefab.com/feed/feed-2.rss"
             :title "Feed 2"
             :link "/feed/feed-2"
             :description "This is the description of Feed #2"
             :author "Matthew Williams"
             :entries [{:mediaGroup nil
                        :title "Feed 2: First Entry"
                        :link "http://www.google.com"
                        :content "Feed 2: First Entry, with real contents"
                        :contentSnippet "... not important"
                        :publishedDate "13 Apr 2008 12:40:07 -0700"
                        :categories []}]}
            {:feedUrl "http://prefab.com/feed/feed-3.rss"
             :title "Feed 3"
             :link "/feed/feed-3"
             :description "This is the description of Feed #3"
             :author "Logan Linn"
             :entries [{:mediaGroup nil
                        :title "Feed 3: First Entry"
                        :link "http://www.google.com"
                        :content "Feed 3: First Entry, with real contents"
                        :contentSnippet "... not important"
                        :publishedDate "13 Apr 2009 12:40:07 -0700"
                        :categories []}]}
            {:feedUrl "http://prefab.com/feed/feed-4.rss"
             :title "Feed 4"
             :link "/feed/feed-4"
             :description "This is the description of Feed #4"
             :author "Sam Carter"
             :entries [{:mediaGroup nil
                        :title "Feed 4: First Entry"
                        :link "http://www.google.com"
                        :content "Feed 4: First Entry, with real contents"
                        :contentSnippet "... not important"
                        :publishedDate "13 Apr 2010 12:40:07 -0700"
                        :categories []}]}])

(defn app [system]
  (->
    (routes
      (GET "/feed/:id" [id]
           (views/feed-view (rand-nth feeds)))
      (POST "/feed" {{:keys [urls]} :params}
            (when-let [urls (if (coll? urls) (set urls))]
              (wcar redis (feed/create-feed urls))
              (redirect-after-post (feed-url (feed/feed-id urls)))))

      (GET "/random" []
           (resp/redirect (-> feeds rand-nth :link)))
      (GET "/" [] (views/index-page 10 (take 3 (shuffle feeds))))
      (route/resources "/")
      (route/not-found "Not found."))
    compojure.handler/site))
