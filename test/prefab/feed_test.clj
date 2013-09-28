(ns prefab.feed-test
  (:use clojure.test
        prefab.feed))

(deftest test-url-validation
  (testing "That urls are validated"
    (is (= true (valid-url? "http://blog.mattyw.net/atom.xml")))
    (is (= true (valid-url? "http://xkcd.com/rss.xml")))
    (is (= true (valid-url? "http://www.reddit.com/r/vim/.rss")))
    (is (= false (valid-url? "http://www.bbc.co.uk")))
    (is (= false (valid-url? "foobar")))))

(deftest test-feeds-validation
  (testing "That feeds are validated"
    (is (= true (valid-urls? ["http://blog.mattyw.net/atom.xml"])))
    (is (= false (valid-urls? ["http://blog.mattyw.net/atom.xml" "foobar"])))))
