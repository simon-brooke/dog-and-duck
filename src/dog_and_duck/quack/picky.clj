(ns dog-and-duck.quack.picky "Fault-finder for ActivityPub documents. 
                              
                              Generally, each `-faults` function will return:
                              1. `nil` if no faults were found;
                              2. a sequence of fault objects if faults were found.
                              
                              Each fault object shall have the properties:
                              1. `:@context` whose value shall be the URL of a 
                                 document specifying this vocabulary;
                              2. `:type` whose value shall be `Fault`;
                              3. `:severity` whose value shall be one of 
                                 `minor`, `should`, `must` or `critical`;
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
 (:require [dog-and-duck.utils.process :refer [pid]]))

(def ^:const severity
  "Severity of faults found, as follows:
   
   1. `:minor` things which I consider to be faults, but which 
      don't actually breach the spec;
   2. `:should` instances where the spec says something SHOULD
      be done, which isn't;
   3. `:must` instances where the spec says something MUST
      be done, which isn't;
   4. `:critical` instances where I believe the fault means that
      the object cannot be meaningfully processed."
  #{:minor :should :must :critical})

(def ^:const severity-filters
  "Hack for implementing a severity hierarchy"
  {:all #{}
   :minor #{:minor}
   :should #{:minor :should}
   :must #{:minor :should :must}
   :critical severity})

(defn filter-severity
  "Return a list of reports taken from these `reports` where the severity
   of the report is greater than this `severity`."
  [reports severity]
  (assert 
   (and 
    (coll? reports) 
    (every? map? reports) 
    (every? :severity reports)))
  (remove 
   #((severity-filters severity) (:severity %))
   reports))

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
  `(context? ((keyword "@context") ~x)))



(defn make-fault-object
  "Return a fault object with these `severity`, `fault` and `narrative` values.
   
   An ActivityPub object MUST have a globally unique ID. Whether this is 
   meaningful depends on whether we persist fault report objects and serve
   them, which at present I have no plans to do."
  [severity fault narrative]
  (assoc {}
         (keyword "@context") validation-fault-context-uri
         :id (str "https://"
                  (.. java.net.InetAddress getLocalHost getHostName)
                  "/fault/"
                  pid
                  ":"
                  (inst-ms (java.util.Date.)))
         :type "Fault"
         :severity severity
         :fault fault
         :narrative narrative))

(defn object-faults
  [x]
  (remove 
   empty?
   (list
    (when-not
     (has-context? x)
      (make-fault-object 
       :should 
       :no-context 
       "Section 3 of the ActivityPub specification states 
        `Implementers SHOULD include the ActivityPub context in 
        their object definitions`.")
    (when-not (:type x) 
      (make-fault-object 
       :minor 
       :no-type
       "The ActivityPub specification states that the `type` field is
        optional, but it is hard to process objects with no known type."))
      (when-not (contains? x :id)
        (make-fault-object
         :minor
         :no-id-transient
         "The ActivityPub specification allows objects without `id` fields
          only if they are intentionally transient; even so it is preferred
          that the object should have an explicit null id."
         ))
    ))))
