(ns prefab.feed-test
  (:use clojure.test
        prefab.feed))

(deftest test-validation
  (testing "That feeds are validated"
    (is (= true (validate-feeds ["http://blog.mattyw.net/atom.xml"])))
    (is (= nil (validate-feeds ["http://blog.mattyw.net/atom.xml" "foobar"])))))
