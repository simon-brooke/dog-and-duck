(ns dog-and-duck.quack.picky.utils
  "Utility functions supporting the picky validator"
  (:require [clojure.set :refer [intersection]]
            [dog-and-duck.quack.picky.constants :refer [activitystreams-context-uri
                                                        actor-types
                                                        context-key severity-filters
                                                        validation-fault-context-uri
                                                        verb-types]]
            [dog-and-duck.quack.picky.fault-messages :refer [messages]]
            [dog-and-duck.utils.process :refer [get-hostname get-pid]]
            [taoensso.timbre :as timbre
      ;; Optional, just refer what you like:
             :refer [warn]]))

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


(defn actor-type?
  "Return `true` if the `x` is a recognised actor type, else `false`."
  [^String x]
  (if (actor-types x) true false))

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

(defn verb-type?
  "`true` if `x`, a string, represents a recognised ActivityStreams activity
   type."
  [^String x]
  (if (verb-types x) true false))

(defn has-activity-type?
  "Return `true` if the object `x` has a type which is an activity type, else 
   `false`."
  [x]
  (let [tv (:type x)]
    (cond
      (coll? tv) (truthy? (not-empty (filter verb-type? tv)))
      :else (verb-type? tv))))

(defn has-actor-type?
  "Return `true` if the object `x` has a type which is an actor type, else 
   `false`."
  [x]
  (let [tv (:type x)]
    (cond
      (coll? tv) (truthy? (not-empty (filter actor-type? tv)))
      :else (actor-type? tv))))

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
        :else
        (throw
         (ex-info
          "Argument `reports` was not a collection of fault reports"
          {:arguments {:reports reports
                       :severity severity}}))))

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
  `(if (and (coll? ~x) (empty? ~x)) nil
       ~x))

(defn concat-non-empty
  "Quick function to replace the pattern (nil-if-empty (remove nil? (concat ...)))
   which I'm using a lot!"
  [& lists]
  (nil-if-empty (remove nil? (apply concat lists))))

(defn has-type-or-fault
  "If object `x` has a `:type` value which is `acceptable`, return `nil`;
   else return a fault object with this `severity` and `token`.
   
   `acceptable` may be passed as either nil, a string, or a set of strings.
   If `acceptable` is `nil`, no type specific tests will be performed."
  [x acceptable severity token]
  (when acceptable
    (let [tv (:type x)]
      (when-not
       (cond
         (and (string? tv) (string? acceptable)) (= tv acceptable)
         (and (string? tv) (set? acceptable)) (acceptable tv)
         (and (coll? tv) (string? acceptable)) ((set tv) acceptable)
         (and (coll? tv) (set? acceptable)) (not-empty
                                             (intersection (set tv) acceptable))
         :else
         (throw (ex-info "Type value or `acceptable` argument not as expected."
                         {:arguments {:x x
                                      :acceptable acceptable
                                      :severity severity
                                      :token token}})))
        (make-fault-object severity token)))))
