(ns dog-and-duck.quack.picky.objects
  (:require [clojure.data.json :as json]
            [dog-and-duck.quack.picky.constants :refer [actor-types noun-types]]
            [dog-and-duck.quack.picky.control-variables :refer [*reify-refs*]]
            [dog-and-duck.quack.picky.time :refer [date-time-property-or-fault
                                                   xsd-date-time?
                                                   xsd-duration?]]
            [dog-and-duck.quack.picky.utils :refer [concat-non-empty
                                                    has-activity-type?
                                                    has-context?
                                                    has-type?
                                                    has-type-or-fault
                                                    make-fault-object
                                                    nil-if-empty
                                                    object-or-uri?]]
            [taoensso.timbre :refer [warn]])
  (:import [java.io FileNotFoundException]
           [java.net URI URISyntaxException]))

(def object-expected-properties
  "Requirements of properties of object, cribbed from
   https://www.w3.org/TR/activitystreams-vocabulary/#properties
   
   Note the following sub-key value types:
   
   * `:collection` opposite of `:functional`: if true, value should be a
      collection (in the Clojure sense), not a single object;
   * `:functional` if true, value should be a single object; if false, may
      be a single object or a sequence of objects, but each must pass 
      validation checks;
   * `:if-invalid` a sequence of two keywords, first indicating severity,
      second being a message key;
   * `:if-missing` a sequence of two keywords, first indicating severity,
      second being a message key;
   * `:required` a boolean, or a function of one argument returning a 
      boolean, in which case the function will be applied to the object
      having the property;
   * `:verifier` a function of one argument returning a boolean, which will 
      be applied to the value or values of the identified property."
  {:accuracy {:functional false
              :if-invalid [:must :invalid-number]
              :verifier number?}
   :actor {:functional false
           :if-invalid [:must :invalid-actor]
           :if-missing [:must :no-actor]
           :required has-activity-type?
           :verifier object-or-uri?}
   :altitude {:functional false
              :if-invalid [:must :invalid-number]
              :verifier number?}
   :anyOf {:collection true
           :functional false
           ;; a Question should have a `:oneOf` ot `:anyOf`, but at this layer
           ;; that's hard to check.
           :if-invalid [:must :invalid-option]
           :verifier object-or-uri?}
   :attachment {:functional false
                :if-invalid [:must :invalid-attachment]
                :verifier object-or-uri?}
   :attributedTo {:functional false
                  :if-invalid [:must :invalid-attribution]
                  :verifier object-or-uri?}
   :audience {:functional false
              :if-invalid [:must :invalid-audience]
              :verifier object-or-uri?}
   :bcc {:functional false
         :if-invalid [:must :invalid-audience] ;; do we need a separate message for bcc, cc, etc?
         :verifier object-or-uri?}
   :cc {:functional false
        :if-invalid [:must :invalid-audience] ;; do we need a separate message for bcc, cc, etc?
        :verifier object-or-uri?}
   :closed {:functional false
            :if-invalid [:must :invalid-closed]
            :verifier (fn [pv] (or (object-or-uri? pv)
                                   (xsd-date-time? pv)
                                   (#{"true" "false"} pv)))}
   :content {:functional false
             :if-invalid [:must :invalid-content]
             :verifier string?}
   :context {:functional false
             :if-invalid [:must :invalid-context]
             :verifier object-or-uri?}
   :current {:functional true
             :if-missing [:minor :paged-collection-no-current]
             :if-invalid [:must :paged-collection-invalid-current]
             :required (fn [x] ;; if an object is a collection which has pages,
                                 ;; it ought to have a `:current` page. But 
                                 ;; 1. it isn't required to, and
                                 ;; 2. there's no certain way of telling that it
                                 ;;    does have pages - although if it has a
                                 ;;    `:first`, then it is.
                         (and
                          (or (has-type? x "Collection")
                              (has-type? x "OrderedCollection"))
                          (:first x)))
             :verifier (fn [pv] (object-or-uri? pv #{"CollectionPage"
                                                     "OrderedCollectionPage"}))}
   :duration {:functional false
              :if-invalid [:must :invalid-duration]
              :verifier xsd-duration?}
   :first {:functional true
           :if-missing [:minor :paged-collection-no-first]
           :if-invalid [:must :paged-collection-invalid-first]
           :required (fn [x] ;; if an object is a collection which has pages,
                                 ;; it ought to have a `:first` page. But 
                                 ;; 1. it isn't required to, and
                                 ;; 2. there's no certain way of telling that it
                                 ;;    does have pages - although if it has a
                                 ;;    `:last`, then it is.
                       (and
                        (or (has-type? x "Collection")
                            (has-type? x "OrderedCollection"))
                        (:last x)))
           :verifier (fn [pv] (object-or-uri? pv #{"CollectionPage"
                                                   "OrderedCollectionPage"}))}
   :generator {:functional false
               :if-invalid [:must :invalid-generator]
               :verifier object-or-uri?}
   :icon {:functional false
          :if-invalid [:must :invalid-icon]
          ;; an icon is also expected to have a 1:1 aspect ratio, but that's
          ;; too much detail at this level of verification
          :verifier (fn [pv] (object-or-uri? pv "Image"))}
   :id {:functional true
        :if-missing [:minor :no-id-transient]
        :if-invalid [:must :invalid-id]
        :verifier (fn [pv] (try (uri? (URI. pv))
                                (catch URISyntaxException _ false)))}
   :image {:functional false
           :if-invalid [:must :invalid-image]
           :verifier (fn [pv] (object-or-uri? pv "Image"))}
   :inReplyTo {:functional false
               :if-invalid [:must :invalid-in-reply-to]
               :verifier (fn [pv] (object-or-uri? pv noun-types))}
   :instrument {:functional false
                :if-invalid [:must :invalid-instrument]
                :verifier object-or-uri?}
   :items {:collection true
           :functional false
           :if-invalid [:must :invalid-items]
           :if-missing [:must :no-items-or-pages]
           :required (fn [x] (or (has-type? x #{"CollectionPage"
                                                "OrderedCollectionPage"})
                                 (and (has-type? x #{"Collection"
                                                     "OrderedCollection"})
                                      ;; if it's a collection and has pages,
                                      ;; it doesn't need items.
                                      (not (:current x))
                                      (not (:first x))
                                      (not (:last x)))))
           :verifier object-or-uri?}
   :last {:functional true
          :if-missing [:minor :paged-collection-no-last]
          :if-invalid [:must :paged-collection-invalid-last]
          :required (fn [x] (if (try (uri? (URI. x))
                                     (catch URISyntaxException _ false))
                              true
                                 ;; if an object is a collection which has pages,
                                 ;; it ought to have a `:last` page. But 
                                 ;; 1. it isn't required to, and
                                 ;; 2. there's no certain way of telling that it
                                 ;;    does have pages - although if it has a
                                 ;;    `:first`, then it is.
                              (and
                               (has-type? x #{"Collection"
                                              "OrderedCollection"})
                               (:first x))))
          :verifier (fn [pv] (object-or-uri? pv #{"CollectionPage"
                                                  "OrderedCollectionPage"}))}
   :location {:functional false
              :if-invalid [:must :invalid-location]
              :verifier (fn [pv] (object-or-uri? pv #{"Place"}))}
   :name {:functional false
             :if-invalid [:must :invalid-name]
             :verifier string?}
   :oneOf {:collection true
           :functional false
           ;; a Question should have a `:oneOf` ot `:anyOf`, but at this layer
           ;; that's hard to check.
           :if-invalid [:must :invalid-option]
           :verifier object-or-uri?}
   :origin {:functional false
            :if-invalid :invalid-origin
            :verifier object-or-uri?}
   :next {:functional true
          :if-invalid [:must :invalid-next-page]
          :verifier (fn [pv] (object-or-uri? pv #{"CollectionPage"
                                                  "OrderedCollectionPage"}))}
   :object {:functional false
            :if-invalid [:must :invalid-direct-object]
            :verifier object-or-uri?}
   :prev {:functional true
          :if-invalid [:must :invalid-prior-page]
          :verifier (fn [pv] (object-or-uri? pv #{"CollectionPage"
                                                  "OrderedCollectionPage"}))}
   :preview {:functional false
             :if-invalid [:must :invalid-preview]
             ;; probably likely to be an Image or Video, but that isn't stated.
             :verifier object-or-uri?}
   :replies {:functional true
             :if-invalid [:must :invalid-replies]
             :verifier (fn [pv] (object-or-uri? pv #{"Collection"
                                                     "OrderedCollection"}))}
   :result {:functional false
            :if-invalid [:must :invalid-result]
            :verifier object-or-uri?}
   :tag {:functional false
         :if-invalid [:must :invalid-tag]
         :verifier object-or-uri?}
   :target {:functional false
            :if-invalid [:must :invalid-target]
            :verifier object-or-uri?}
   :to {:functional false
        :if-invalid [:must :invalid-to]
        :verifier (fn [pv] (object-or-uri? pv actor-types))}
   :type {:functional false
          :if-missing [:minor :no-type]
          :if-invalid [:must :invalid-type]
          ;; strictly, it's an 'anyURI', but realistically these are not checkable.
          :verifier string?}
   :url {:functional false
         :if-invalid [:must :invalid-url-property]
         :verifier (fn [pv] (object-or-uri? pv "Link"))}})

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
