; lein run -m prefab.prepopulate
(ns prefab.prepopulate
  (:require [org.httpkit.client :as http]
            [environ.core :refer (env)]
            [taoensso.timbre :refer (info infof error)]))

(def feeds
  [{:name "ClojureCup Blog"
    :urls ["http://clojurecup.com/blog/feed.xml"]}
   {:name "Reddit Frontpage"
    :urls ["http://www.reddit.com/r/all.rss"]}
   {:name "Slashdot"
    :urls ["http://prefab.clojurecup.com/feeds/427546384"]}
   {:name "NPR News"
    :urls ["http://www.npr.org/rss/rss.php?id=1001"]}
   {:name "Tech News"
    :urls ["http://feeds.bbci.co.uk/news/technology/rss.xml"
           "http://www.theregister.co.uk/headlines.atom"]}
   {:name "Matts Reddit Favourites"
    :urls ["http://reddit.com/r/vim/.rss"
           "http://reddit.com/r/clojure/.rss"
           "http://reddit.com/r/programming/.rss"]}
   {:name "ESPN News"
    :urls ["http://sports.espn.go.com/espn/rss/news"]}])

(defn create-feed [host {:keys [name urls] :as feed}]
  (infof "Creating feed '%s' %s" name urls)
  (let [options {:form-params {:name name "urls[]" urls} :keepalive 3000}
        resp @(http/post (str host "/feeds") options)]
    (when-let [e (:error resp)] (error e))))

(defn -main [& args]
  (let [host (env :host "http://localhost:8080")
        create-feed (partial create-feed host)]
    (info "Starting Prefab Prepopulation")
    (doseq [feed feeds] (create-feed feed))
    (info "Done!")))
