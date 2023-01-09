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
  (reduce
   concat-non-empty
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
         (map (fn [f] (assoc f :document (.getName %))) faults))
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

;; (count r)
;; (last r)
;; (clojure.pprint/pprint (last r))

(defn distribution
  "Distribution of values of function `f` when applied to `vals`.
   
   I *know* there's a library function that does this, probably better, but I
   don't remember what it's called!"
  [f vals]
  (loop [result {} values vals]
    (cond (empty? values) result
          :else (let [r (apply f (list (first values)))
                      i (if (result r) (inc (result r)) 1)]
                  (recur (assoc result r i) (rest values))))))

(distribution :fault r)