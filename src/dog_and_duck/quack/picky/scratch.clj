(ns dog-and-duck.quack.picky.scratch
  "Development scratchpad"
  (:require [clojure.data.json :refer [read-str]]
            [clojure.java.io :refer [file]]
            [clojure.walk :refer [keywordize-keys]]
            [dog-and-duck.quack.picky.objects :refer
             [object-faults]]
            [dog-and-duck.quack.picky.utils :refer [concat-non-empty]]))

(def r
  (remove
   nil?
   (map
    #(try
       (let [contents (read-str (slurp %))
             faults (cond (map? contents) (object-faults
                                           (keywordize-keys contents))
                        ;;   (coll? contents) (apply
                        ;;                     concat-non-empty
                        ;;                     (map (fn [obj]
                        ;;                            (object-faults
                        ;;                             (keywordize-keys obj)))
                        ;;                          contents))
                          )]
         (when-not (nil? faults)
           [(.getName %) faults]))
       (catch Exception any
         [(.getName %) (str "Exception "
                            (.getName (.getClass any))
                            ": "
                            (.getMessage any))]))
    (filter
     #(and (.isFile %) (.endsWith (.getName %) ".json"))
     (file-seq (file "resources/activitystreams-test-documents"))))))
