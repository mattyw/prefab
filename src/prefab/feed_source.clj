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
  (title          [this] (:title this))
  (link           [this] (:link this))

  IFeedEntry
  (content        [this] (get-in this [:description :value]))
  (published-date [this] (:published-date this)))

(defrecord RSSFeed [uri link title author authors entries description published-date]
  IFeedData
  (title       [this] (:title this))
  (link        [this] (some this [:uri :link]))

  IFeedSource
  (feedUrl     [this] (some this [:link :uri]))
  (description [this] (get-in this [:description :value]))
  (author      [this] (seq (some this [:author :authors])))
  (entries     [this] (prn this)(map map->RSSFeedEntry (:entries this))))

(defrecord AtomFeedEntry [])
(defrecord AtomFeed [])

(extend-type nil
  IFeedSource
  (entries [this] (prn "what")))

(defn parse-feed
  "Takes a raw hashmap of feed data and returns the correct
  adapter record, or `nil` for unknown types"
  [feed]
  (prn feed)
  (condp #(contains? %2 %1) feed
    :entries (map->RSSFeed feed)
    :todo    (map->AtomFeed feed)
    nil))
