(ns prefab.views.helpers
  (:require [hiccup.element :refer (link-to)]
            [hiccup.util :refer [url]]
            ))

(defn full-url
  "Generate a full URL from the given paramters"
  [& args]
  (apply url "http://prefab.clojurecup.com" args))

(defn share-twitter
  "Share a link on Twitter"
  ([link] (share-twitter link nil))
  ([link msg]
   (link-to {:class "text-vmiddle share-btn twitter"
             :title "Share this feed via Twitter"}
            (url "https://twitter.com/intent/tweet" {:url (full-url link) :text msg}))))

(defn share-facebook
  "Share a link on Facebook"
  ([link] (share-facebook link nil))
  ([link msg]
   (link-to {:class "text-vmiddle share-btn facebook"
             :title "Share this feed via Facebook"
             :onclick (str "window.open('" (url "https://www.facebook.com/sharer/sharer.php" {:u (full-url link)}) "', 'facebook-share-dialog', 'width=626,height=436'); return false;")}
            "#")))
