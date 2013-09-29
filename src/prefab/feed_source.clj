(ns prefab.feed-source)

(defprotocol IFeedData
  (title [this] "The title of the item")
  (link  [this] "The link to the HTML version of item"))

(defprotocol IFeedSource
  "Interface describing the relevant accessors to a given feed's data"
  (feedUrl     [this] "The URL for the feed")
  ;(title       [this] "The title of the feed")
  ;(link        [this] "The URL of the HTML version of the feed")
  (description [this] "The description of the feed")
  (author      [this] "The author of the feed")
  (entries     [this] "A collection of individual entries in the feed"))

(defprotocol IFeedEntry
  "Interface describing the relevant accessors to a given feed entry's data"
  ; mediaGroup
  ;(title         [this] "The title of the entry")
  ;(link          [this] "The URL for the HTML version of the entry")
  (content        [this] "The contents of the entry")
  ; contentSnippet
  (published-date [this] "The publication date of the entry"))
  ; categories

(defrecord RSSFeedEntry [link title description published-date]
  IFeedData
  (title          [this] title)
  (link           [this] link)

  IFeedEntry
  (content        [this] description)
  (published-date [this] published-date))

(defrecord RSSFeed [uri link title author authors entries description published-date]
  IFeedData
  (title       [this] title)
  (link        [this] (or uri link))

  IFeedSource
  (feedUrl     [this] (or link uri))
  (description [this] description)
  (author      [this] (seq (or author authors)))
  (entries     [this] (map map->RSSFeedEntry entries)))

(defrecord AtomFeedEntry [])
(defrecord AtomFeed [])

(defn parse-feed
  "Takes a raw hashmap of feed data and returns the correct
  adapter record, or `nil` for unknown types"
  [feed]
  (condp #(contains? %2 %1) feed
    :entries (merge (map->RSSFeed (select-keys feed [:uri :link :title :author :authors :published-date]))
                    {:entries
                     (map #(assoc
                             (select-keys % [:uri :link :title])
                             :description
                             (get-in % [:description :value] (:description %)))
                          (:entries feed))
                     :description
                     (get-in feed [:description :value] (:description feed))})
    :todo    (map->AtomFeed feed)
    nil))

(defn feed?
  "Determine if the given argument is a valid feed"
  [feed]
  (satisfies? IFeedSource feed))
