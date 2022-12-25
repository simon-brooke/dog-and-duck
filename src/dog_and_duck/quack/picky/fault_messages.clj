(ns dog-and-duck.quack.picky.fault-messages
  "Narrative values for fault reports of specific types, used by the picky
   validator.")

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

(def messages
  "Actual fault messages to which fault codes resolve."
  {:id-not-https "Publicly facing content SHOULD use HTTPS URIs"
   :id-not-uri "identifiers must be publicly dereferencable URIs"
   :no-context "Section 3 of the ActivityPub specification states Implementers SHOULD include the ActivityPub context in their object definitions`."
   :no-id-persistent "Persistent objects MUST have unique global identifiers."
   :no-id-transient "The ActivityPub specification allows objects without `id` fields only if they are intentionally transient; even so it is preferred that the object should have an explicit null id."
   :no-inbox "Actor objects MUST have an `inbox` property, whose value MUST be a reference to an ordered collection."
   :no-outbox "Actor objects MUST have an `outbox` property, whose value MUST be a reference to an ordered collection."
   :no-type "The ActivityPub specification states that the `type` field is optional, but it is hard to process objects with no known type."
   :not-actor-type "The `type` value of the object was not a recognised actor type."
   :null-id-persistent "Persistent objects MUST have non-null identifiers."
   :not-an-object "ActivityStreams object must be JSON objects."})