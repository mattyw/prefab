(ns prefab.util)

(defprotocol Coersions
  (int* [x]))

(extend-protocol Coersions
  String
  (int* [x] (Integer/parseInt x))
  Integer
  (int* [x] x)
  Object
  (int* [x] (int x)))
