(ns dog-and-duck.quack.fault-messages)

(def messages
  "Actual fault messages to which fault codes resolve."
  {:id-not-https "Publicly facing content SHOULD use HTTPS URIs"
   :id-not-uri "identifiers must be publicly dereferencable URIs"
   :no-context "Section 3 of the ActivityPub specification states Implementers SHOULD include the ActivityPub context in their object definitions`."
   :no-id-persistent "Persistent objects MUST have unique global identifiers."
   :no-id-transient "The ActivityPub specification allows objects without `id` fields only if they are intentionally transient; even so it is preferred that the object should have an explicit null id."
   :null-id-persistent "Persistent objects MUST have non-null identifiers."
   :no-type "The ActivityPub specification states that the `type` field is optional, but it is hard to process objects with no known type."
   :not-an-object "ActivityStreams object must be JSON objects."})