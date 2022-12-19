(ns dog-and-duck.scratch.parser
  (:require [clojure.walk :refer [keywordize-keys]]
            [clojure.data.json :as json]
            [dog-and-duck.quack.quack :as q]))

(defn clean 
  "Take this `json` input, and return a sequence of ActivityPub objects 
   represented by it."
  [json]
  (let [feed (json/read-str json)]
    (filter
     q/object?
     (cond (map? feed) (list (keywordize-keys feed))
           (coll? feed) (map keywordize-keys feed)))))

(map :type (map keywordize-keys (json/read-str (slurp "resources/feed.json"))))

(keys (first (map keywordize-keys (json/read-str (slurp "resources/feed.json")))))

(q/object? (first (map keywordize-keys (json/read-str (slurp "resources/feed.json")))))