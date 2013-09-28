(ns prefab.feed-test
  (:use clojure.test
        prefab.feed))

(deftest test-url-validation
  (testing "That urls are validated"
    (is (= true (validate-feed "http://blog.mattyw.net/atom.xml")))
    (is (= false (validate-feed "foobar")))))

(deftest test-feeds-validation
  (testing "That feeds are validated"
    (is (= true (validate-feeds ["http://blog.mattyw.net/atom.xml"])))
    (is (= false (validate-feeds ["http://blog.mattyw.net/atom.xml" "foobar"])))))
