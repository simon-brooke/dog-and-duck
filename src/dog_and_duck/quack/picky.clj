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
    (:require [dog-and-duck.quack.picky.collections :refer [collection-page-faults
                                                            paged-collection-faults
                                                            simple-collection-faults]]
              [dog-and-duck.quack.picky.constants :refer [actor-types]]
              [dog-and-duck.quack.picky.utils :refer [any-or-faults
                                                      coll-object-reference-or-fault
                                                      concat-non-empty
                                                      has-activity-type?
                                                      has-actor-type? has-type?
                                                      has-type-or-fault
                                                      make-fault-object
                                                      object-faults
                                                      object-reference-or-faults
                                                      string-or-fault]])
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
  ([x]
   (concat-non-empty
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
       (make-fault-object :must :no-id-persistent)))))
  ([x types severity token]
   (concat-non-empty
    (persistent-object-faults x)
    (list
     (has-type-or-fault x types severity token)))))

(defn actor-faults
  "Return a list of faults found in actor `x`, or `nil` if none are."
  [x]
  (concat-non-empty
   (persistent-object-faults x)
   (list
    (when-not (has-actor-type? x)
      (make-fault-object :must :not-actor-type))
    (uri-or-fault
     (:inbox x) :must :no-inbox :invalid-inbox-uri)
    (uri-or-fault
     (:outbox x) :must :no-outbox :invalid-outbox-uri))))

(defn link-faults
  "A link object is required to have an `href` property. It may have all of
   `rel` | `mediaType` | `name` | `hreflang` | `height` | `width` | `preview`
   but I *think* they're all optional."
  [x]
  (concat-non-empty
   (object-reference-or-faults x "Link" :critical :expected-link)
   (list
    (uri-or-fault
     (:href x) :must :no-href-uri :invalid-href-uri)
    (string-or-fault (:mediaType x) :minor :no-media-type #"\w+\/[-+.\w]+")
   ;; TODO: possibly more here. Audit against the specs
    )))

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
  (concat-non-empty
   (persistent-object-faults x)
   (activity-type-faults x)
   (list
    (when-not
     (has-activity-type? x)
      (make-fault-object :must :not-activity-type))
    (when-not (string? (:summary x)) (make-fault-object :should :no-summary)))))

(defn collection-faults
  "Return a list of faults found in the collection `x`; if `type` is also 
   specified, it should be a string naming a specific collection type for
   which checks should be performed. 
   
   Every collection *should*(?) have a `totalItems` field (an integer).
   
   Beyond that, collections are either 'just collections' (in which case
   they *should* have an `items` field (a sequence)), or else they're paged
   collections, in which case they *must*(?) have a `first` field which is 
   a collection page or a URI pointing to a collection page, and *should* 
   have a `last` field which is similar.
   
   The pages of collections *should* be collection pages; the pages of 
   ordered collections *should* be ordered collection pages."
  ([x]
   (collection-faults
    x
    (first
     (remove nil?
             (map #(when (has-type? x %) %)
                  ["Collection"
                   "OrderedCollection"
                   "CollectionPage"
                   "OrderedCollectionPage"])))))
  ([x type]
   ;; (log/info "collection-faults called with argumens " x ", " type)
   (case type
     ("Collection" "OrderedCollection") (any-or-faults
                                         (list (simple-collection-faults x type)
                                               (paged-collection-faults x type))
                                         :must
                                         :no-items)
     ("CollectionPage" "OrderedCollectionPage") (collection-page-faults x type)
     (list (make-fault-object :critical :expected-collection)))))
