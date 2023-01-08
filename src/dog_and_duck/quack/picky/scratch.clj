(ns dog-and-duck.quack.picky.scratch
  "Development scratchpad"
  (:require [clojure.data.json :refer [read-str]]
            [clojure.java.io :refer [file]]
            [clojure.walk :refer [keywordize-keys]]
            [dog-and-duck.quack.picky.objects :refer
             [object-faults properties-faults]]
            [dog-and-duck.quack.picky.utils :refer [concat-non-empty
                                                    filter-severity]]))

(def files (filter
            #(and (.isFile %) (.endsWith (.getName %) ".json"))
            (file-seq (file "resources/activitystreams-test-documents"))))

(def r
  (remove
   empty?
   (map
    #(try
       (let [contents (read-str (slurp %))
             faults (cond (map? contents) (filter-severity
                                           (object-faults
                                            (keywordize-keys contents))
                                           :should)
                        ;;   (coll? contents) (apply
                        ;;                     concat-non-empty
                        ;;                     (map (fn [obj]
                        ;;                            (object-faults
                        ;;                             (keywordize-keys obj)))
                        ;;                          contents))
                          )]
         (when-not (empty? faults)
           [(.getName %) faults]))
       (catch Exception any
         [(.getName %) (str "Exception "
                            (.getName (.getClass any))
                            ": "
                            (.getMessage any))]))
    (filter
     #(and (.isFile %) (.endsWith (.getName %) ".json"))
     (file-seq (file "resources/activitystreams-test-documents"))))))

(count (filter-severity (object-faults (keywordize-keys (read-str (slurp "resources/activitystreams-test-documents/vocabulary-ex189-jsonld.json")))) :critical))

(count (filter
        #(and (.isFile %) (.endsWith (.getName %) ".json"))
        (file-seq (file "resources/activitystreams-test-documents"))))

(count r)
(last r)
(clojure.pprint/pprint (last r))