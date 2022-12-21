(ns dog-and-duck.quack.quack
  "Validator for ActivityPub objects: if it walks like a duck, and it quacks 
   like a duck...
   
   **NOTE THAT the ActivityPub spec 
   [says](https://www.w3.org/TR/activitypub/#obj)
   
   > Servers SHOULD validate the content they receive to avoid content 
   > spoofing attacks
   
   but in practice ActivityPub content collected in the wild bears only 
   a hazy relationship to the spec, so this is difficult. I suspect that
   I may have to implement a `*strict*` dynamic variable, so that users can 
   toggle some checks off."
  
  ;;(:require [clojure.spec.alpha as s])
  (:require [dog-and-duck.quack.picky :refer [filter-severity has-context? 
                                              object-faults]])
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
   handle.
   
   **NOTE THAT** The ActivityPub spec [says](https://www.w3.org/TR/activitypub/#obj)
   
   > Implementers SHOULD include the ActivityPub context in their object 
   > definitions
   
   but in samples found in the wild they typically don't."
  ([x]
  (and (map? x) (:type x) true))
  ([x severity]
   (empty? (filter-severity (object-faults x) severity))))

(defn persistent-object?
  "`true` iff `x` is a persistent object.

   Transient objects in ActivityPub are not required to have an `id` key, but persistent
   ones must have a key, and it must be an IRI (but normally a URI)."
  [x]
  (try
    (and (object? x) (uri? (URI. (:id x))))
    (catch URISyntaxException _ false)))

;; (persistent-object? {:type "test" :id "https://mastodon.scot/@barfilfarm"})

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
  "Return `true` iff the `x` is a recognised actor type, else `false`."
  [^String x]
  `(if (actor-types ~x) true false))

;; (actor-type? "Group")

(def ^:const verb-types
  "The set of types we will accept as verbs.
   
   There's an [explicit set of allowed verb types]
   (https://www.w3.org/TR/activitystreams-vocabulary/#activity-types)."
  #{"Accept" "Add" "Announce" "Arrive" "Block" "Create" "Delete" "Dislike"
    "Flag" "Follow" "Ignore" "Invite" "Join" "Leave" "Like" "Listen" "Move"
    "Offer" "Question" "Reject" "Read" "Remove" "TentativeAccept"
    "TentativeReject" "Travel" "Undo" "Update" "View"})

(defmacro verb-type?
  ;; TODO: better as a macro
  [^String x]
  `(if (verb-types ~x) true false))


(defn actor?
  "Returns `true` if `x` quacks like an actor, else false.
   
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
  (and
   (object? x)
   (has-context? x)
   (uri? (URI. (:inbox x)))
   (uri? (URI. (:outbox x)))
   (actor-type? (:type x))
   true))

(defn actor-or-uri?
  "`true` if `x` is either a URI or an actor.
   
   **TODO**: I need to decide about whether to reify referenced objects
   before validation or after. After reification, every reference to an actor
   *must be* to an actor object, but before, may only be to a URI pointing to 
   one."
  [x]
  (and 
   (cond (string? x) (uri? (URI. x))
        :else (actor? x)) 
       true))

(defn activity?
  "`true` iff `x` quacks like an activity, else false."
  [x]
  (try
    (and (object? x)
         (has-context? x)
         (string? (:summary x))
         (actor-or-uri? (:actor x))
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
  "`true` iff `x` quacks like a collection of type `object-type`, else `false`.
   
   With one argument, will recognise plain collections and ordered collections,
   but (currently) not collection pages."
  ([x ^String object-type]
   (let [items (or (:items x) (:orderedItems x))]
     (and
      (cond
        (:items x) (nil? (:orderedItems x))
        (:orderedItems x) (nil? (:items x)) ;; can't have both properties
        (integer? (:totalItems x)) true ;; can have neither, provided it has totalItems.
        :else false) 
      (object? x)
      (= (:type x) object-type)
      (if items
        (and (coll? items)
             (every? object? items) ;; if there are items, they must form a
                                    ;; collection of objects.
             true)
        true) ;; but it's OK if there aren't.
      true)
     ;; test for totalItems not done here, because collection pages don't
     ;; have it.
     ))
  ([x]
   (and
    (or (collection? x "Collection")
        (collection? x "OrderedCollection"))
    (integer? (:totalItems x))
    true)))

(defn unordered-collection?
  "`true` iff `x` quacks like an unordered collection, else `false`."
  [x]
  (and (collection? x "Collection") (integer? (:totalItems x)) true))

(defn ordered-collection?
  "`true` iff `x` quacks like an ordered collection, else `false`."
  [x]
  (and (collection? x "OrderedCollection") (integer? (:totalItems x)) true))

(defn collection-page?
  "`true` iff `x` quacks like a page in a paged collection, else `false`."
  [x]
  (collection? x "CollectionPage"))

(defn ordered-collection-page?
  "`true` iff `x` quacks like a page in an ordered paged collection, else `false`."
  [x]
  (collection? x "OrderedCollectionPage"))


