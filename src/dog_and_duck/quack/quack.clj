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
  "Return `true` iff `x` is recognisably an ActivityStreams object.
   
   **NOTE THAT** The ActivityStreams spec 
   [says](https://www.w3.org/TR/activitystreams-core/#object):
   
   > All properties are optional (including the id and type)
   
   But we are *just not having that*, because otherwise we're flying blind.
   We *shall* reject objects lacking at least `:type`. Missing `:id` keys are
   tolerable because they represent transient objects, which we expect to 
   handle."
  [x]
  (and (map? x) (:type x) true))

(object? nil)
(object? {:type "test"})

(defn persistent-object?
  "`true` iff `x` is a persistent object.

   Transient objects in ActivityPub are not required to have an `id` key, but persistent
   ones must have a key, and it must be an IRI (but normally a URI)."
  [x]
  (try
    (and (object? x) (uri? (URI. (:id x))))
    (catch URISyntaxException _ false)))

(persistent-object? {:type "test" :id "https://mastodon.scot/@barfilfarm"})

(defn actor?
  "TODO!"
  [x]
  true)

(def verb?
  "The set of types we will accept as verbs.
   
   There's an [explicit set of allowed verbs]
   (https://www.w3.org/TR/activitystreams-vocabulary/#activity-types)."
  #{"Accept" "Add" "Announce" "Arrive" "Block" "Create" "Delete" "Dislike"
    "Flag" "Follow" "Ignore" "Invite" "Join" "Leave" "Like" "Listen" "Move"
    "Offer" "Question" "Reject" "Read" "Remove" "TentativeAccept"
    "TentativeReject" "Travel" "Undo" "Update" "View"})

(defn activity?
  "`true` iff `x` is an activity, else false.

  see "
  [x]
  (try
    (and (object? x)
         (uri? (URI. ((keyword "@context") x)))
         (string? (:summary x))
         (actor? (:actor x))
         (verb? (:type x))
         (or (object? (:object x)) (uri? (URI. x))))
    (catch URISyntaxException _ false)))