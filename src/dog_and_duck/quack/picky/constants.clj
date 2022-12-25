(ns dog-and-duck.quack.picky.constants
  "Constants supporting the picky validator.")

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

(def ^:const activitystreams-context-uri
  "The URI of the context of an ActivityStreams object is expected to be this
   literal string."
  "https://www.w3.org/ns/activitystreams")

(def ^:const actor-types
  "The set of types we will accept as actors.
   
   There's an [explicit set of allowed actor types]
   (https://www.w3.org/TR/activitystreams-vocabulary/#actor-types)."
  #{"Application"
    "Group"
    "Organization"
    "Person"
    "Service"})

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

(def ^:const validation-fault-context-uri
  "The URI of the context of a validation fault report object shall be this
   literal string."
  "https://simon-brooke.github.io/dog-and-duck/codox/Validation_Faults.html")

(def ^:const verb-types
  "The set of types we will accept as verbs.
   
   There's an [explicit set of allowed verb types]
   (https://www.w3.org/TR/activitystreams-vocabulary/#activity-types)."
  #{"Accept" "Add" "Announce" "Arrive" "Block" "Create" "Delete" "Dislike"
    "Flag" "Follow" "Ignore" "Invite" "Join" "Leave" "Like" "Listen" "Move"
    "Offer" "Question" "Reject" "Read" "Remove" "TentativeAccept"
    "TentativeReject" "Travel" "Undo" "Update" "View"})

