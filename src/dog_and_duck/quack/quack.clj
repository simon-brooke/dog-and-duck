(ns dog-and-duck.quack.quack
  "Validator for ActivityPub objects: if it walks like a duck, and it quacks like a duck..."
  ;;(:require [clojure.spec.alpha as s])
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

(defn object?
  "Returns `true` iff `x` is recognisably an ActivityStreams object.
   
   **NOTE THAT** The ActivityStreams spec 
   [says](https://www.w3.org/TR/activitystreams-core/#object):
   
   > All properties are optional (including the id and type)
   
   But we are *just not having that*, because otherwise we're flying blind.
   We *shall* reject objects lacking at least `:type`. Missing `:id` keys are
   tolerable because they represent transient objects, which we expect to 
   handle."
  [x]
  (and (map? x) (:type x) true))

(defn persistent-object?
  "`true` iff `x` is a persistent object.

   Transient objects in ActivityPub are not required to have an `id` key, but persistent
   ones must have a key, and it must be an IRI (but normally a URI)."
  [x]
  (try
    (and (object? x) (uri? (URI. (:id x))))
    (catch URISyntaxException _ false)))

(persistent-object? {:type "test" :id "https://mastodon.scot/@barfilfarm"})

(def ^:const actor-types
  "The set of types we will accept as actors.
   
   There's an [explicit set of allowed actor types]
   (https://www.w3.org/TR/activitystreams-vocabulary/#actor-types)."
  #{"Application"
    "Group"
    "Organization"
    "Person"
    "Service"})

(defn actor-type?
  ;; TODO: better as a macro
  [x]
  (if (actor-types x) true false))

(def ^:const verb-types
  "The set of types we will accept as verbs.
   
   There's an [explicit set of allowed verb types]
   (https://www.w3.org/TR/activitystreams-vocabulary/#activity-types)."
  #{"Accept" "Add" "Announce" "Arrive" "Block" "Create" "Delete" "Dislike"
    "Flag" "Follow" "Ignore" "Invite" "Join" "Leave" "Like" "Listen" "Move"
    "Offer" "Question" "Reject" "Read" "Remove" "TentativeAccept"
    "TentativeReject" "Travel" "Undo" "Update" "View"})

(defn verb-type?
  ;; TODO: better as a macro
  [x]
  (if (verb-types x) true false))

(def ^:const activitystreams-context-uri
  "The URI of the context of an ActivityStreams object is expected to be this
   literal string."
  "https://www.w3.org/ns/activitystreams")

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

(defmacro has-context? [x]
  `(context? ((keyword "@context") ~x)))

(defn actor?
  "Returns `true` if `x` quacks like an actor, else false."
  [x]
  (and
   (object? x)
   (has-context? x)
   (uri? (URI. (:inbox x)))
   (uri? (URI. (:outbox x)))
   (actor-type? (:type x))
   true))

(defn activity?
  "`true` iff `x` quacks like an activity, else false.
   
   **NOTE THAT** [Section 4.1 of the spec]
   (https://www.w3.org/TR/activitypub/#actor-objects) says explicitly that
   
   >  Actor objects MUST have, in addition to the properties mandated by 3.1 Object Identifiers, the following properties:
   >
   >  inbox
   >    A reference to an [ActivityStreams] OrderedCollection comprised of all the messages received by the actor; see 5.2 Inbox. 
   > outbox
   >    An [ActivityStreams] OrderedCollection comprised of all the messages produced by the actor; see 5.1 Outbox. 
   
   However, none of the provided examples in the [activitystreams-test-documents repository]() does in fact have these properties"
  [x]
  (try
    (and (object? x)
         (has-context? x)
         (string? (:summary x))
         (actor? (:actor x))
         (verb-type? (:type x))
         (or (object? (:object x)) (uri? (URI. (:object x))))
         true)
    (catch URISyntaxException _ false)))

(defn link?
  "`true` iff `x` quacks like a link, else false."
  [x]
  (and (object? x)
       (= (:type x) "Link")
       (uri? (URI. (:href x)))
       true))

(defn link-or-uri?
  "`true` iff `x` is either a URI or a link, else false.
   
   There are several points in the specification where e.g. the `:image`
   property (if present) may be either a link or a URI."
  [x]
  (and
   (cond (string? x) (uri? (URI. x))
         :else (link? x))
   true))

(defn collection?
  "`true` iff `x` quacks like a collection of type `type`, else `false`.
   
   With one argument, will recognise plain collections and ordered collections,
   but (currently) not collection pages."
  ([x type]
   (let [items (or (:items x) (:orderedItems x))]
     (and
      (cond
        (:items x) (nil? (:orderedItems x))
        (:orderedItems x) (nil? (:items x))) ;; can't have both properties
      (object? x)
      (= (:type x) type)
      (coll? items)
      (every? object? items)
      (integer? (:totalItems x))
      true)))
  ([x]
   (or (collection? x "Collection")
       (collection? x "OrderedCollection"))))

(defn unordered-collection?
  "`true` iff `x` quacks like an unordered collection, else `false`."
  [x]
  (collection? x "Collection"))

(defn ordered-collection?
  "`true` iff `x` quacks like an ordered collection, else `false`."
  [x]
  (collection? x "OrderedCollection"))

(defn collection-page?
  "`true` iff `x` quacks like a page in a paged collection, else `false`."
  [x]
  (collection? x "CollectionPage"))

(defn ordered-collection-page?
  "`true` iff `x` quacks like a page in an ordered paged collection, else `false`."
  [x]
  (collection? x "OrderedCollectionPage"))


