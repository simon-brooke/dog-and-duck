(ns dog-and-duck.quack.picky.objects
  (:require [clojure.data.json :as json]
            [dog-and-duck.quack.picky.control-variables :refer [*reify-refs*]]
            [dog-and-duck.quack.picky.time :refer [date-time-property-or-fault]]
            [dog-and-duck.quack.picky.utils :refer [concat-non-empty
                                                    has-context?
                                                    has-type?
                                                    has-type-or-fault
                                                    make-fault-object
                                                    nil-if-empty]]
            [taoensso.timbre :refer [warn]])
  (:import [java.io FileNotFoundException]
[java.net URI URISyntaxException]))

(defn object-faults
  "Return a list of faults found in object `x`, or `nil` if none are.
   
   If `expected-type` is also passed, verify that `x` has `expected-type`.
   `expected-type` may be passed as a string or as a set of strings. Detailed
   verification of the particular features of types is not done here."

  ;; TODO: many more properties which are nor required, nevertheless have required
  ;; property TYPES as detailed in
  ;; https://www.w3.org/TR/activitystreams-vocabulary/#properties
  ;; if these properties are present, these types should be checked.
  ([x]
   (nil-if-empty
    (remove empty?
            (list
             (when-not (map? x)
               (make-fault-object :critical :not-an-object))
             (when-not
              (has-context? x)
               (make-fault-object :should :no-context))
             (when-not (:type x)
               (make-fault-object :minor :no-type))
             (when-not (and (map? x) (contains? x :id))
               (make-fault-object :minor :no-id-transient))
             (date-time-property-or-fault x :endTime :must
                                          :not-valid-date-time false)
             (date-time-property-or-fault x :published :must
                                          :not-valid-date-time false)
             (date-time-property-or-fault x :startTime :must
                                          :not-valid-date-time false)))))
  ([x expected-type]
   (concat-non-empty
    (object-faults x)
    (when expected-type
      (list
       (has-type-or-fault x expected-type :critical :unexpected-type))))))

(def maybe-reify
  "If `*reify-refs*` is `true`, return the object at this `target` URI.
   Returns `nil` if
   
   1. `*reify-refs*` is false;
   2. the object was not found;
   3. access to the object was not permitted.
   
   Consequently, use with care."
  (memoize
   (fn [target]
     (try (let [uri (URI. target)]
            (when *reify-refs*
              (json/read-str (slurp uri))))
          (catch URISyntaxException _
            (warn "Reification target" target "was not a valid URI.")
            nil)
          (catch FileNotFoundException _
            (warn "Reification target" target "was not found.")
            nil)))))

(defn maybe-reify-or-faults
  "If `*reify-refs*` is `true`, runs basic checks on the object at this 
   `target` URI, if it is found, or a list containing a fault object with
   this `severity` and `token` if it is not."
  [value expected-type severity token]
  (let [object (maybe-reify value)]
    (cond object
          (object-faults object expected-type)
          *reify-refs* (list (make-fault-object severity token)))))

(defn object-reference-or-faults
  "If this `value` is either 
   
   1. an object of `expected-type`;
   2. a URI referencing an object of  `expected-type`; or
   3. a link object referencing an object of  `expected-type`
   
   and no faults are returned from validating the linked object, then return
   `nil`; else return a sequence comprising a fault object with this `severity`
   and `token`, prepended to the faults returned.
   
   As with `has-type-or-fault` (q.v.), `expected-type` may be passed as a
   string, as a set of strings, or `nil` (indicating the type of the 
   referenced object should not be checked).
   
   **NOTE THAT** if `*reify-refs*` is `false`, referenced objects will not
   actually be checked."
  [value expected-type severity token]
  (let [faults (cond
                 (string? value) (maybe-reify-or-faults value severity token expected-type)
                 (map? value) (if (has-type? value "Link")
                                (cond
                                  ;; if we were looking for a link and we've 
                                  ;; found a link, that's OK.
                                  (= expected-type "Link") nil
                                  (and (set? expected-type) (expected-type "Link")) nil
                                  (nil? expected-type) nil
                                  :else
                                  (object-reference-or-faults
                                   (:href value) expected-type severity token))
                                (object-faults value expected-type))
                 :else (throw
                        (ex-info
                         "Argument `value` was not an object or a link to an object"
                         {:arguments {:value value}
                          :expected-type expected-type
                          :severity severity
                          :token token})))]
    (when faults (cons (make-fault-object severity token) faults))))

(defn coll-object-reference-or-fault
  "As object-reference-or-fault, except `value` argument may also be a list of
   objects and/or object references."
  [value expected-type severity token]
  (cond
    (map? value) (object-reference-or-faults value expected-type severity token)
    (coll? value) (concat-non-empty
                   (map
                    #(object-reference-or-faults
                      % expected-type severity token)
                    value))
    :else (throw
           (ex-info
            "Argument `value` was not an object, a link to an object, nor a list of these."
            {:arguments {:value value}
             :expected-type expected-type
             :severity severity
             :token token}))))
