(ns dog-and-duck.quack.picky.objects
  (:require [clojure.data.json :as json]
            [clojure.set :refer [union]]
            [dog-and-duck.quack.picky.constants :refer [actor-types
                                                        noun-types
                                                        re-rfc5646]]
            [dog-and-duck.quack.picky.control-variables :refer [*reify-refs*]]
            [dog-and-duck.quack.picky.time :refer [date-time-property-or-fault
                                                   xsd-date-time?
                                                   xsd-duration?]]
            [dog-and-duck.quack.picky.utils :refer [concat-non-empty
                                                    cond-make-fault-object
                                                    has-activity-type?
                                                    has-context?
                                                    has-type?
                                                    has-type-or-fault
                                                    make-fault-object
                                                    nil-if-empty
                                                    object-or-uri?
                                                    truthy?
                                                    xsd-non-negative-integer?]]
            [taoensso.timbre :refer [info warn]])
  (:import [java.io FileNotFoundException]
           [java.net URI URISyntaxException]))

(defn- xsd-float?
  [pv]
  (or (integer? pv) (float? pv)))

;;;     Copyright (C) Simon Brooke, 2022

;;;     This program is free software; you can redistribute it and/or
;;;     modify it under the terms of the GNU General Public License
;;;     as published by the Free Software Foundation; either version 2
;;;     of the License, or (at your option) any later version.

;;;     This program is distributed in the hope that it will be useful,
;;;     but WITHOUT ANY WARRANTY; without even the implied warranty of
;;;     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;;;     GNU General Public License for more details.

;;;     You should have received a copy of the GNU General Public License
;;;     along with this program; if not, write to the Free Software
;;;     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

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
   * `:validator` a function of one argument returning a boolean, which will 
      be applied to the value or values of the identified property."
  {:accuracy {:functional false
              :if-invalid [:must :invalid-number]
              :validator (fn [pv] (and (xsd-float? pv)
                                       (>= pv 0)
                                       (<= pv 100)))}
   :actor {:functional false
           :if-invalid [:must :invalid-actor]
           :if-missing [:must :no-actor]
           :required has-activity-type?
           :validator object-or-uri?}
   :altitude {:functional false
              :if-invalid [:must :invalid-number]
              :validator xsd-float?}
   :anyOf {:collection true
           :functional false
           ;; a Question should have a `:oneOf` or `:anyOf`, but at this layer
           ;; that's hard to check.
           :if-invalid [:must :invalid-option]
           :validator object-or-uri?}
   :attachment {:functional false
                :if-invalid [:must :invalid-attachment]
                :validator object-or-uri?}
   :attributedTo {:functional false
                  :if-invalid [:must :invalid-attribution]
                  :validator object-or-uri?}
   :audience {:functional false
              :if-invalid [:must :invalid-audience]
              :validator object-or-uri?}
   :bcc {:functional false
         :if-invalid [:must :invalid-audience] ;; do we need a separate message for bcc, cc, etc?
         :validator object-or-uri?}
   :cc {:functional false
        :if-invalid [:must :invalid-audience] ;; do we need a separate message for bcc, cc, etc?
        :validator object-or-uri?}
   :closed {:functional false
            :if-invalid [:must :invalid-closed]
            :validator (fn [pv] (truthy? (or (object-or-uri? pv)
                                             (xsd-date-time? pv)
                                             (#{"true" "false"} pv))))}
   :content {:functional false
             :if-invalid [:must :invalid-content]
             :validator string?}
   :context {:functional false
             :if-invalid [:must :invalid-context]
             :validator object-or-uri?}
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
             :validator (fn [pv] (object-or-uri? pv #{"CollectionPage"
                                                      "OrderedCollectionPage"}))}
   :deleted {:functional true
             :if-missing [:minor :tombstone-missing-deleted]
             :if-invalid [:must :invalid-deleted]
             :required (fn [x] (has-type? x "Tombstone"))
             :validator xsd-date-time?}
   :describes {:functional true
               :required (fn [x] (has-type? x "Profile"))
               :if-invalid [:must :invalid-describes]
               ;; TODO: actually the spec says this MUST be an object and
               ;; not a URI, which it doesn't say anywhere else, but this seems
               ;; to make no sense?
               :validator object-or-uri?}
   :duration {:functional false
              :if-invalid [:must :invalid-duration]
              :validator xsd-duration?}
   :endTime {:functional true
             :if-invalid [:must :invalid-date-time]
             :validator xsd-date-time?}
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
           :validator (fn [pv] (object-or-uri? pv #{"CollectionPage"
                                                    "OrderedCollectionPage"}))}
   :formerType {:functional false
                :if-missing [:minor :tombstone-missing-former-type]
                :if-invalid [:must :invalid-former-type]
                :required (fn [x] (has-type? x "Tombstone"))
                ;; The narrative of the spec says this should be an `Object`,
                ;; but in all the provided examples it's a string.
                :validator string?}
   :generator {:functional false
               :if-invalid [:must :invalid-generator]
               :validator object-or-uri?}
   :height {:functional false
            :if-invalid [:must :invalid-non-negative]
            :validator xsd-non-negative-integer?}
   :href {:functional false
          :if-invalid [:must :invalid-href]
          :validator (fn [pv] (try (uri? (URI. pv))
                                   (catch URISyntaxException _ false)))}
   :hreflang {:validator (fn [pv] (truthy? (re-matches re-rfc5646 pv)))}
   :icon {:functional false
          :if-invalid [:must :invalid-icon]
          ;; an icon is also expected to have a 1:1 aspect ratio, but that's
          ;; too much detail at this level of verification
          :validator (fn [pv] (object-or-uri? pv "Image"))}
   :id {:functional true
        :if-missing [:minor :no-id-transient]
        :if-invalid [:must :invalid-id]
        :validator (fn [pv] (try (uri? (URI. pv))
                                 (catch URISyntaxException _ false)))}
   :image {:functional false
           :if-invalid [:must :invalid-image]
           :validator (fn [pv] (object-or-uri? pv "Image"))}
   :inReplyTo {:functional false
               :if-invalid [:must :invalid-in-reply-to]
               :validator (fn [pv] (object-or-uri? pv noun-types))}
   :instrument {:functional false
                :if-invalid [:must :invalid-instrument]
                :validator object-or-uri?}
   :items {:collection true
           :functional false
           :if-invalid [:must :invalid-items]
           :if-missing [:must :no-items-or-pages]
           :required (fn [x] (or (has-type? x "CollectionPage")
                                 (and (has-type? x "Collection")
                                      ;; if it's a collection and has pages,
                                      ;; it doesn't need items.
                                      (not (:current x))
                                      (not (:first x))
                                      (not (:last x)))))
           :validator (fn [pv] (and (coll? pv) (every? object-or-uri? pv)))}
   :last {:functional true
          :if-missing [:minor :paged-collection-no-last]
          :if-invalid [:must :paged-collection-invalid-last]
          :required (fn [x] (if (and
                                 (string? x)
                                 (try (uri? (URI. x))
                                      (catch URISyntaxException _ false)))
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
          :validator (fn [pv] (object-or-uri? pv #{"CollectionPage"
                                                   "OrderedCollectionPage"}))}
   :latitude {:functional true
              :if-invalid [:must :invalid-latitude]
              ;; The XSD spec says this is an IEEE 754-2008, and the IEEE
              ;; wants US$104 for me to find out what that is. So I don't
              ;; strictly know that an integer is valid here.
              :validator xsd-float?}
   :location {:functional false
              :if-invalid [:must :invalid-location]
              :validator (fn [pv] (object-or-uri? pv #{"Place"}))}
   :longitude {:functional true
               :if-invalid [:must :invalid-longitude]
               :validator xsd-float?}
   :mediaType {:functional true
               :if-invalid [:must :invalid-mime-type]
               :validator (fn [pv] (truthy? (re-matches #"\w+/[-.\w]+(?:\+[-.\w]+)?" pv)))}
   :name {:functional false
          :if-invalid [:must :invalid-name]
          :validator string?}
   :next {:functional true
          :if-invalid [:must :invalid-next-page]
          :validator (fn [pv] (object-or-uri? pv #{"CollectionPage"
                                                   "OrderedCollectionPage"}))}
   :object {:functional false
            :if-invalid [:must :invalid-direct-object]
            :validator object-or-uri?}
   :oneOf {:collection true
           :functional false
           ;; a Question should have a `:oneOf` ot `:anyOf`, but at this layer
           ;; that's hard to check.
           :if-invalid [:must :invalid-option]
           :validator object-or-uri?}
   
   :orderedItems {:collection true
           :functional false
           :if-invalid [:must :invalid-items]
           :if-missing [:must :no-items-or-pages]
           :required (fn [x] (or (has-type? x "OrderedCollectionPage")
                                 (and (has-type? x "OrderedCollection")
                                      ;; if it's a collection and has pages,
                                      ;; it doesn't need items.
                                      (not (:current x))
                                      (not (:first x))
                                      (not (:last x)))))
           :validator (fn [pv] (and (coll? pv) (every? object-or-uri? pv)))}
   :origin {:functional false
            :if-invalid [:must :invalid-origin]
            :validator object-or-uri?}
   :partOf {:functional true
            :if-missing [:must :missing-part-of]
            :if-invalid [:must :invalid-part-of]
            :required (fn [x] (object-or-uri? x #{"CollectionPage"
                                                  "OrderedCollectionPage"}))
            :validator (fn [pv] (object-or-uri? pv #{"Collection"
                                                     "OrderedCollection"}))}
   :prev {:functional true
          :if-invalid [:must :invalid-prior-page]
          :validator (fn [pv] (object-or-uri? pv #{"CollectionPage"
                                                   "OrderedCollectionPage"}))}
   :preview {:functional false
             :if-invalid [:must :invalid-preview]
             ;; probably likely to be an Image or Video, but that isn't stated.
             :validator object-or-uri?}
   :published {:functional true
               :if-invalid [:must :invalid-date-time]
               :validator xsd-date-time?}
   :replies {:functional true
             :if-invalid [:must :invalid-replies]
             :validator (fn [pv] (object-or-uri? pv #{"Collection"
                                                      "OrderedCollection"}))}
   :radius {:functional true
            :if-invalid [:must :invalid-positive-number]
            :validator (fn [pv] (and (xsd-float? pv) (> pv 0)))}
   :rel {:functional false
         :if-invalid [:must :invalid-link-relation]
         ;; TODO: this is not really good enough.
         :validator (fn [pv] (truthy? (re-matches #"[a-zA-A0-9_\-\.\:\?/\\]*" pv)))}
   :relationship {;; this exists in the spec, but it doesn't seem to be required and it's
                  ;; extremely hazily specified. 
                  }
   :result {:functional false
            :if-invalid [:must :invalid-result]
            :validator object-or-uri?}
   :startIndex {:functional true
                :if-invalid [:must :invalid-start-index]
                :validator xsd-non-negative-integer?}
   :start-time {:functional true
                :if-invalid [:must :invalid-date-time]
                :validator xsd-date-time?}
   :subject {:functional true
             :if-invalid [:must :invalid-subject]
             :if-missing [:minor :no-relationship-subject]
             :required (fn [x] (has-type? x "Relationship"))
             :validator object-or-uri?}
   :summary {:functional false
             :if-invalid [:must :invalid-summary]
             ;; TODO: HTML formatting is allowed, but other forms of formatting
             ;; are not. Can this be validated?
             :validator string?}
   :tag {:functional false
         :if-invalid [:must :invalid-tag]
         :validator object-or-uri?}
   :target {:functional false
            :if-invalid [:must :invalid-target]
            :validator object-or-uri?}
   :to {:functional false
        :if-invalid [:must :invalid-to]
        :validator (fn [pv] (object-or-uri? pv actor-types))}
   :totalItems {:functional true
                :if-invalid [:must :invalid-total-items]
                :validator xsd-non-negative-integer?}
   :type {:functional false
          :if-missing [:minor :no-type]
          :if-invalid [:must :invalid-type]
          ;; strictly, it's an `anyURI`, but realistically these are not checkable.
          :validator string?}
   :units {:functional true
           :if-invalid [:must :invalid-units]
           ;; the narrative says that `anyURI`, but actually unless it's a recognised
           ;; unit the property is useless. These are the units explicitly specified.
           :validator (fn [pv] (#{"cm" "feet" "inches" "km" "m" "miles"} pv))}
   :updated {:functional true
             :if-invalid [:must :invalid-updated]
             :validator xsd-date-time?}
   :url {:functional false
         :if-invalid [:must :invalid-url-property]
         :validator (fn [pv] (object-or-uri? pv "Link"))}
   :width {:functional true
           :if-invalid [:must :invalid-width]
           :validator xsd-non-negative-integer?}})

(defn check-property-required [obj prop clause]
  (let [required (:required clause)
        [severity token] (:if-missing clause)]
    (when required
      (when
       (and (apply required (list obj)) (not (obj prop)))
        (make-fault-object severity token)))))

(defn check-property-valid
  [obj prop clause]
  ;; (info "obj" obj "prop" prop "clause" clause)
  (let [val (obj prop)
        validator (:validator clause)
        [severity token] (:if-invalid clause)]
    (when (and val validator)
      (cond-make-fault-object
       (apply validator (list val))
       severity token))))

(defn check-property [obj prop]
  (assert (map? obj))
  (assert (keyword? prop))
  (let [clause (object-expected-properties prop)]
    (nil-if-empty
     (remove nil?
             (list
              (check-property-required obj prop clause)
              (check-property-valid obj prop clause))))))

(defn properties-faults
  "Return a lost of faults found on properties of the object `x`, or
   `nil` if none are."
  [x]
  (apply 
   concat-non-empty
   (let [props (set (keys x))
         required (set
                   (filter
                    #((object-expected-properties %) :required)
                    (keys object-expected-properties)))]
     (map
      (fn [p] (check-property x p))
      (union props required)))))

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
   (concat-non-empty
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
               (make-fault-object :minor :no-id-transient))))
    (properties-faults x)))
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
