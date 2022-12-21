# Validation Faults in ActivityPub documents

## Motivation

This document is intended to provide an extension vocabulary for [ActivityStreams](https://www.w3.org/TR/activitystreams-core/) documents, which provides vocabulary for categorising and describing faults in [ActivityPub](https://www.w3.org/TR/activitypub/) documents.

The motivation is to be able to serialise a validation report on an ActivityPub document as an ActivityStreams document.

## Intepretation

### Conformance

As well as sections marked as non-normative, all authoring guidelines, diagrams, examples, and notes in this specification are non-normative. Everything else in this specification is normative.

The key words MAY, MUST, MUST NOT, SHOULD, and SHOULD NOT are to be interpreted as described in [RFC2119].

### 'the spec'

Where the phrase 'the spec' is used in this document, it refers to a concatenation of the ActivityStreams specification and the ActivityPub specification.

## The `Fault` object type

The `Fault` object type is a novel object type introduced by this document to describe validation faults. Objects with the `Fault` object type MUST have at least the following fields (additional fields are not required but are optional):

1. `:@context` whose value shall be the URL of a document specifying this vocabulary;
2. `:type` whose value shall be `Fault`;
3. `:severity` whose value shall be one of `minor`, `should`, `must` or `critical`;
4. `:fault` whose value shall be a unique token representing the particular fault type;
5. `:narrative` whose value shall be a natural language description of the fault type.

### The Fields

#### Context

The value of the `@context` field of a fault report object shall be the URL of this
document, currently `https://simon-brooke.github.io/dog-and-duck/codox/Validation_Faults.html`.

#### Type 

The value of the `type` field of a fault report object MUST be `Fault`.

#### Severity

Each fault report object MUST have a `severity` field whose value MUST be one of

   1. `:minor` things which I consider to be faults, but which
      don't actually breach the spec;
   2. `:should` instances where the spec says something SHOULD
      be done, which isn't;
   3. `:must` instances where the spec says something MUST
      be done, which isn't;
   4. `:critical` instances where I believe the fault means that
      the object cannot be meaningfully processed.

#### Fault

Unique codes shall be assigned to each fault type, and shall be documented in this section.

It is intended that there should ultimately be a well known site at which the fault codes can be resolved to natural language explanations in as many natural languages as possible of the nature of the particular fault.


