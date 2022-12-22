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
    (:require [clojure.set :refer [intersection]]
              [dog-and-duck.quack.fault-messages :refer [messages]]
              [dog-and-duck.utils.process :refer [get-hostname get-pid]]
              [taoensso.timbre :as timbre
      ;; Optional, just refer what you like:
               :refer [warn]])
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

;; ERRATA

(def ^:dynamic *reify-refs*
  "If `true`, references to objects in fields will be reified and validated. 
   If `false`, they won't, but an `:info` level fault report will be generated.
   
   There are several things in the spec which, in a document, may correctly be
   either
   
   1. a fully fleshed out object, or
   2. a URI pointing to such an object.
   
   Obviously to fully validate a document we ought to reify all the refs and 
   check that they are themselves valid, but
   
   a. in some of the published test documents the URIs do not reference a
      valid document;
   b. there will be performance costs to reifying all the refs;
   c. in perverse cases, reifying refs might result in runaway recursion.
   
   TODO: I think that in production this should default to `true`."
  false)

(def ^:dynamic *reject-severity*
  "The severity at which the binary validator will return `false`.
   
   In practice documents seen in the wild do not typically appear to be 
   fully valid, and this does not matter. This allows the sensitivity of
   the binary validator (`dog-and-duck.quack.quack`) to be tuned. It's in
   this (`dog-and-duck.quack.picky`) namespace, not that one, because this
   namespace is where concerns about severity are handled."
  :must)

(def ^:const context-key
  "The Clojure reader barfs on `:@context`, although it is in principle a valid 
   keyword. So we'll make it once, here, to make the code more performant and
   easier to read."
  (keyword "@context"))

(def ^:const severity
  "Severity of faults found, as follows:
   
   0. `:info` not actually a fault, but an issue noted during validation;
   1. `:minor` things which I consider to be faults, but which 
      don't actually breach the spec;
   2. `:should` instances where the spec says something SHOULD
      be done, which isn't;
   3. `:must` instances where the spec says something MUST
      be done, which isn't;
   4. `:critical` instances where I believe the fault means that
      the object cannot be meaningfully processed."
  #{:info :minor :should :must :critical})

(def ^:const severity-filters
  "Hack for implementing a severity hierarchy"
  {:all #{}
   :info #{}
   :minor #{:info}
   :should #{:info :minor}
   :must #{:info :minor :should}
   :critical severity})

(defn truthy?
  "Return `true` if `x` is truthy, else `false`."
  [x]
  (if x true false))

(defn has-type?
  "Return `true` if object `x` has type `type`, else `false`.
   
   The values of `type` fields of ActivityStreams objects may be lists; they
   are considered to have a type if the type token is a member of the list."
  [x type]
  (assert (map? x) (string? type))
  (let [tv (:type x)]
    (cond
      (coll? tv) (truthy? (not-empty (filter #(= % type) tv)))
      :else (= tv type))))

(defn filter-severity
  "Return a list of reports taken from these `reports` where the severity
   of the report is greater than this or equal to this `severity`."
  [reports severity]
  (cond (nil? reports) nil
        (and
         (coll? reports)
         (every? map? reports)
         (every? :severity reports)) (remove
                                      #((severity-filters severity) (:severity %))
                                      reports)
        :else reports)) ;; TODO this actually shouldn't happen and we should
                        ;; error if it does

(def ^:const activitystreams-context-uri
  "The URI of the context of an ActivityStreams object is expected to be this
   literal string."
  "https://www.w3.org/ns/activitystreams")

(def ^:const validation-fault-context-uri
  "The URI of the context of a validation fault report object shall be this
   literal string."
  "https://simon-brooke.github.io/dog-and-duck/codox/Validation_Faults.html")

(defn context?
  "Returns `true` iff `x` quacks like an ActivityStreams context, else false.
   
   A context is either
   1. the URI (actually an IRI) `activitystreams-context-uri`, or
   2. a collection comprising that URI and a map."
  [x]
  (cond
    (nil? x) false
    (string? x) (and (= x activitystreams-context-uri) true)
    (coll? x) (and (context? (first (remove map? x)))
                   (= (count x) 2)
                   true)
    :else false))

(defmacro has-context?
  "True if `x` is an ActivityStreams object with a valid context, else `false`."
  [x]
  `(context? (context-key ~x)))

(defn make-fault-object
  "Return a fault object with these `severity`, `fault` and `narrative` values.
   
   An ActivityPub object MUST have a globally unique ID. Whether this is 
   meaningful depends on whether we persist fault report objects and serve
   them, which at present I have no plans to do."
  ;; TODO: should not pass in the narrative; instead should use the :fault value
  ;; to look up the narrative in a resource file.
  [severity fault]
  (assoc {}
         context-key validation-fault-context-uri
         :id (str "https://"
                  (get-hostname)
                  "/fault/"
                  (get-pid)
                  ":"
                  (inst-ms (java.util.Date.)))
         :type "Fault"
         :severity severity
         :fault fault
         :narrative (or (messages fault)
                        (do
                          (warn "No narrative provided for fault token " fault)
                          (str fault)))))

(defmacro nil-if-empty
  "if `x` is an empty collection, return `nil`; else return `x`."
  [x]
  `(if (coll? ~x)
     (nil-if-empty ~x)
     ~x))

(defn has-type-or-fault
  "If object `x` has a `:type` value which is `acceptable`, return `nil`;
   else return a fault object with this `severity` and `token`.
   
   `acceptable` may be passed as either a string, or as a set of strings."
  [x acceptable severity token]
  (let [tv (:type x)]
    (when-not 
     (cond 
      (and (string? tv) (string? acceptable)) (= tv acceptable)
      (and (string? tv) (set? acceptable)) (acceptable tv)
      (and (coll? tv) (string? acceptable)) ((set tv) acceptable)
      (and (coll? tv) (set? acceptable)) (not-empty 
                                          (intersection (set tv) acceptable))
      ;; TODO else should error
      )
      (make-fault-object severity token))))

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
  [u severity if-missing-token if-invalid-token]
  (try
    (uri? (URI. u))
    (catch URISyntaxException _
      (make-fault-object severity if-invalid-token))
    (catch NullPointerException _
      (make-fault-object severity if-missing-token))))

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

(def ^:const actor-types
  "The set of types we will accept as actors.
   
   There's an [explicit set of allowed actor types]
   (https://www.w3.org/TR/activitystreams-vocabulary/#actor-types)."
  #{"Application"
    "Group"
    "Organization"
    "Person"
    "Service"})

(defmacro actor-type?
  "Return `true` if the `x` is a recognised actor type, else `false`."
  [^String x]
  `(if (actor-types ~x) true false))

(defn has-actor-type?
  "Return `true` if the object `x` has a type which is an actor type, else 
   `false`."
  [x]
  (let [tv (:type x)]
    (cond
      (coll? tv) (truthy? (not-empty (filter actor-type? tv)))
      :else (actor-type? tv))))

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

(def ^:const verb-types
  "The set of types we will accept as verbs.
   
   There's an [explicit set of allowed verb types]
   (https://www.w3.org/TR/activitystreams-vocabulary/#activity-types)."
  #{"Accept" "Add" "Announce" "Arrive" "Block" "Create" "Delete" "Dislike"
    "Flag" "Follow" "Ignore" "Invite" "Join" "Leave" "Like" "Listen" "Move"
    "Offer" "Question" "Reject" "Read" "Remove" "TentativeAccept"
    "TentativeReject" "Travel" "Undo" "Update" "View"})

(defmacro verb-type?
  "`true` if `x`, a string, represents a recognised ActivityStreams activity
   type."
  [^String x]
  `(if (verb-types ~x) true false))

(defn has-activity-type?
  "Return `true` if the object `x` has a type which is an activity type, else 
   `false`."
  [x]
  (let [tv (:type x)]
    (cond
      (coll? tv) (truthy? (not-empty (filter verb-type? tv)))
      :else (actor-type? tv))))

(defn string-or-fault
  "If this `value` is not a string, return a fault object with this `severity` 
   and `token`, else `nil`. If `pattern` is also passed, it is expected to be
   a Regex, and the fault object will be returned unless `value` matches the 
   `pattern`."
  ([value severity token]
   (when-not (string? value) (make-fault-object severity token)))
  ([value severity token pattern]
   (when not (and (string? value) (re-matches pattern value))
         (make-fault-object severity token))) )

(defn link-faults
  "A link object is required to have an `href` property. It may have all of
   `rel` | `mediaType` | `name` | `hreflang` | `height` | `width` | `preview`
   but I *think* they're all optional."
  [x]
  (list
   (uri-or-fault
    (:href x) :must :no-href-uri :invalid-href-uri)
   (string-or-fault (:mediaType x) :minor :no-media-type #"\w+\/[-+.\w]+")))

(defn object-reference-or-fault
  "If this `value` is either 
   
   1. an object of `expected-type`;
   2. a URI; or
   3. a link object
   
   then return `nil`; else return a fault object with this `severity` and `token`.
   
   As with `has-type-or-fault` (q.v.), `expected-type` may be passed as a
   string or as a set of strings.
   
   **NOTE THAT** TODO if `*reify-refs*` is `true` and `value` is either a URI or
   a link, the linked object should be checked and validated as an object of
   `expected-type`."
  [value expected-type severity token]
  (cond TODO continue here in the morning))

(defn activity-type-faults
  "Some specific activity types have specific requirements which are not
   requirements."
  ([x]
   (if (coll? (:type x))
     (map #(activity-type-faults x %) (:type x))
     (activity-type-faults x (:type x))))
  ([x token]
   (case token
     "Link" (link-faults x)
     )))

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
                    (when-not (string? (:summary x)) (make-fault-object :should :no-summary))
                    
                    )))))
