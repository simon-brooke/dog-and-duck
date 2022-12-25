(ns dog-and-duck.quack.picky "Fault-finder for ActivityPub documents. 
                              
                              Generally, each `-faults` function will return:
                              
                              1. `nil` if no faults were found;
                              2. a sequence of fault objects if faults were found.
                              
                              Each fault object shall have the properties:
                              
                              1. `:@context` whose value shall be the URL of a 
                                 document specifying this vocabulary;
                              2. `:type` whose value shall be `Fault`;
                              3. `:severity` whose value shall be one of 
                                 `info`, `minor`, `should`, `must` or `critical`;
                              4. `:fault` whose value shall be a unique token
                                 representing the particular fault type;
                              5. `:narrative` whose value shall be a natural
                                 language description of the fault type.
                              
                              Note that the reason for the `:fault` property is
                              to be able to have a well known place, linked to
                              from the @context URL, which allows narratives 
                              for each fault type to be served in as many
                              natural languages as possible.
                              
                              The idea further is that it should ultimately be
                              possible to serialise a fault report as a 
                              document which in its own right conforms to the
                              ActivityStreams spec."
    (:require [dog-and-duck.quack.picky.constants :refer [actor-types]]
              [dog-and-duck.quack.picky.control-variables :refer [*reify-refs*]]
              [dog-and-duck.quack.picky.utils :refer [has-context? 
                                                      has-activity-type? 
                                                      has-actor-type? has-type?
                                                      has-type-or-fault
                                                      make-fault-object
                                                      nil-if-empty]]
              [clojure.data.json :as json])
    (:import [java.net URI URISyntaxException]))

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

(defn object-faults
  "Return a list of faults found in object `x`, or `nil` if none are.
   
   If `expected-type` is also passed, verify that `x` has `expected-type`.
   `expected-type` may be passed as a string or as a set of strings."
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
               (make-fault-object :minor :no-id-transient))))))
  ([x expected-type]
   (nil-if-empty
    (remove empty?
            (concat
             (object-faults x)
             (list
              ;; TODO: should resolve the correct `-faults`function for the
              ;; `expected-type` and call that; but that's for later.
              (has-type-or-fault x expected-type :critical :unexpected-type)))))))

(defn uri-or-fault
  "If `u` is not a valid URI, return a fault object with this `severity` and 
   `if-invalid-token`. If it's `nil`, return a fault object with this `severity`
   and `if-missing-token`. Otherwise return nil."
  ([u severity if-missing-token]
   (uri-or-fault u severity if-missing-token if-missing-token))
  ([u severity if-missing-token if-invalid-token]
   (try
     (if (uri? (URI. u))
       nil
       (make-fault-object severity if-invalid-token))
     (catch URISyntaxException _
       (make-fault-object severity if-invalid-token))
     (catch NullPointerException _
       (make-fault-object severity if-missing-token)))))

(defn persistent-object-faults
  "Return a list of faults found in persistent object `x`, or `nil` if none are."
  [x]
  (nil-if-empty
   (remove empty?
           (concat
            (object-faults x)
            (list
             (if (contains? x :id)
               (try (let [id (URI. (:id x))]
                      (when-not (= (.getScheme id) "https")
                        (make-fault-object :should :id-not-https)))
                    (catch URISyntaxException _
                      (make-fault-object :must :id-not-uri))
                    (catch NullPointerException _
                      (make-fault-object :must :null-id-persistent)))
               (make-fault-object :must :no-id-persistent)))))))

(defn actor-faults
  "Return a list of faults found in actor `x`, or `nil` if none are."
  [x]
  (nil-if-empty
   (remove empty?
           (concat (persistent-object-faults x)
                   (list
                    (when-not (has-actor-type? x)
                      (make-fault-object :must :not-actor-type))
                    (uri-or-fault
                     (:inbox x) :must :no-inbox :invalid-inbox-uri)
                    (uri-or-fault
                     (:outbox x) :must :no-outbox :invalid-outbox-uri))))))

(defn string-or-fault
  "If this `value` is not a string, return a fault object with this `severity` 
   and `token`, else `nil`. If `pattern` is also passed, it is expected to be
   a Regex, and the fault object will be returned unless `value` matches the 
   `pattern`."
  ([value severity token]
   (when-not (string? value) (make-fault-object severity token)))
  ([value severity token pattern]
   (when not (and (string? value) (re-matches pattern value))
         (make-fault-object severity token))))

(defn object-reference-or-faults
  "If this `value` is either 
   
   1. an object of `expected-type`;
   2. a URI referencing an object of  `expected-type`; or
   3. a link object referencing an object of  `expected-type`
   
   and no faults are returned from validating the linked object, then return
   `nil`; else return a sequence comprising a fault object with this `severity`
   and `token`, prepended to the faults returned.
   
   As with `has-type-or-fault` (q.v.), `expected-type` may be passed as a
   string or as a set of strings.
   
   **NOTE THAT** if `*reify-refs*` is `false`, referenced objects will not
   actually be checked."
  [value expected-type severity token]
  (let [faults (cond
                 (string? value) (try (let [uri (URI. value)
                                            object (when *reify-refs*
                                                     (json/read-str (slurp uri)))]
                                        (when object
                                          (object-faults object expected-type)))
                                      (catch URISyntaxException _
                                        (make-fault-object severity token)))
                 (map? value) (if (has-type? value "Link")
                                (cond
                                  ;; if we were looking for a link and we've 
                                  ;; found a link, that's OK.
                                  (= expected-type "Link") nil
                                  (and (set? expected-type) (expected-type "Link")) nil
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
    (coll? value) (nil-if-empty
                   (remove nil?
                           (reduce concat
                                   (map
                                    #(object-reference-or-faults
                                      % expected-type severity token)
                                    value))))
    :else (throw
           (ex-info
            "Argument `value` was not an object, a link to an object, nor a list of these."
            {:arguments {:value value}
             :expected-type expected-type
             :severity severity
             :token token}))))

(defn link-faults
  "A link object is required to have an `href` property. It may have all of
   `rel` | `mediaType` | `name` | `hreflang` | `height` | `width` | `preview`
   but I *think* they're all optional."
  [x]
  (nil-if-empty
   (remove empty?
           (concat
            (object-reference-or-faults x "Link" :critical :expected-link)
            (list
             (uri-or-fault
              (:href x) :must :no-href-uri :invalid-href-uri)
             (string-or-fault (:mediaType x) :minor :no-media-type #"\w+\/[-+.\w]+")
   ;; TODO: possibly more here. Audit against the specs
             )))))

(def ^:const base-activity-required-properties
  "Properties most activities should have. Values are validating functions, each.
   
   See https://www.w3.org/TR/activitystreams-vocabulary/#dfn-activity"
  {:summary (fn [v] (when-not (string? v)
                      (list (make-fault-object :should :no-summary))))
   :actor (fn [v] (object-reference-or-faults v actor-types :must :no-actor))
   :object (fn [v] (object-reference-or-faults v nil :must :no-object))})

(def ^:const intransitive-activity-required-properties
  "Properties intransitive activities should have.
   
   See https://www.w3.org/TR/activitystreams-vocabulary/#dfn-intransitiveactivity"
  (dissoc base-activity-required-properties :object))

(def ^:const accept-required-properties
  "As base-activity-required-properties, except that the type of the object
   is restricted."
  (assoc base-activity-required-properties
         :object
         (fn [v]
           (object-reference-or-faults v #{"Invite" "Person"}
                                       :must
                                       :bad-accept-target))))

(def ^:const activity-required-properties
  "Properties activities should have, keyed by activity type. Values are maps 
   of the format of `base-activity-required-properties`, q.v."
  {"Accept" accept-required-properties
   "Add" base-activity-required-properties
   "Announce" base-activity-required-properties
   "Arrive" intransitive-activity-required-properties
   ;; TODO: is `:location` required for arrive?
   "Block" base-activity-required-properties
   "Create" base-activity-required-properties
   "Delete" base-activity-required-properties
   "Dislike" base-activity-required-properties
   "Flag" base-activity-required-properties
   "Follow" base-activity-required-properties
   ;; TODO: is `:object` required to be an actor?
   "Ignore" base-activity-required-properties
   "Invite" (assoc base-activity-required-properties :target
                   (fn [v]
                     (coll-object-reference-or-fault v #{"Event" "Group"}
                                                     :must
                                                     :bad-accept-target)))
   ;; TODO: are here other things one could meaningfully be invited to?
   "Join" base-activity-required-properties
   "Leave" base-activity-required-properties
   "Like" base-activity-required-properties
   "Listen" base-activity-required-properties
   "Move" base-activity-required-properties
   "Offer" base-activity-required-properties
   "Question" intransitive-activity-required-properties
   "Reject" base-activity-required-properties
   "Read" base-activity-required-properties
   "Remove" base-activity-required-properties
   "TentativeReject" base-activity-required-properties
   "TentativeAccept" accept-required-properties
   "Travel" base-activity-required-properties
   "Undo" base-activity-required-properties
   "Update" base-activity-required-properties
   "View" base-activity-required-properties})

(defn activity-type-faults
  "Return a list of faults found in the activity `x`; if `type` is also 
   specified, it should be a string naming a specific activity type for
   which checks should be performed.
   
   Some specific activity types have specific requirements which are not
   requirements."
  ([x]
   (if (coll? (:type x))
     (map #(activity-type-faults x %) (:type x))
     (activity-type-faults x (:type x))))
  ([x type]
   (let [checks (activity-required-properties type)]
     (map
      #(apply (checks %) (x %))
      (keys checks)))))

(defn activity-faults
  [x]
  (nil-if-empty
   (remove empty?
           (concat (persistent-object-faults x)
                   (activity-type-faults x)
                   (list
                    (when-not
                     (has-activity-type? x)
                      (make-fault-object :must :not-activity-type))
                    (when-not (string? (:summary x)) (make-fault-object :should :no-summary)))))))
