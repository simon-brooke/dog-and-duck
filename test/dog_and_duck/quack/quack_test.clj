(ns dog-and-duck.quack.quack-test
  (:require [clojure.test :refer [deftest is testing]]
            [dog-and-duck.quack.picky.constants :refer [activitystreams-context-uri
                                                        context-key]]
            [dog-and-duck.quack.picky.utils :refer [actor-type? context?
                                                    verb-type?]]
            [dog-and-duck.quack.quack :refer [actor? 
                                              object? ordered-collection-page?
                                              persistent-object?]]
            [dog-and-duck.scratch.parser :refer [clean]]))

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

(deftest object-test
  (testing "object recognition"
    (let [expected false
          actual (object? nil)]
      (is (= actual expected)))
    (let [expected true
          actual (object? {:type "Test"})]
      (is (= actual expected)))
    (let [expected true
          actual (object?
                  (first
                   (clean
                    (slurp "resources/activitystreams-test-documents/empty.json"))))]
      (is (= actual expected)))
    (let [expected true
          actual (object?
                  (first
                   (clean
                    (slurp "resources/activitystreams-test-documents/core-ex1-jsonld.json"))))]
      (is (= actual expected)))))

(deftest persistent-object-test
  (testing "persistent object recognition"
    (let [expected false
          actual (persistent-object? nil)]
      (is (= actual expected) "Not persistent: not an object."))
    (let [expected true
          actual (persistent-object? {:type "Test" :id "https://foo.bar/@ban"})]
      (is (= actual expected) "Is persistent: has both id and type."))
    (let [expected false
          actual (persistent-object?
                  (first
                   (clean
                    (slurp "resources/activitystreams-test-documents/simple0001.json"))))]
      (is (= actual expected) "Not persistent: has no id."))
    (let [expected true
          actual (persistent-object?
                  (first
                   (clean
                    (slurp "resources/activitystreams-test-documents/simple0008.json"))))]
      (is (= actual expected) "Is persistent: has both id and type."))))

(deftest actor-type-test
  (testing "identification of actor types"
    (let [expected false
          actual (actor-type? nil)]
      (is (= actual expected) "nil is not an actor"))
    (let [expected false
          actual (actor-type? "Duck")]
      (is (= actual expected) "A duck is not an actor"))
    (let [expected true
          actual (actor-type? "Person")]
      (is (= actual expected) "A person is an actor"))
    (let [expected true
          actual (actor-type? "Service")]
      (is (= actual expected) "A service is an actor"))))

(deftest verb-type-test
  (testing "identification of verb types"
    (let [expected false
          actual (verb-type? nil)]
      (is (= actual expected) "nil is not a verb"))
    (let [expected false
          actual (verb-type? "Quack")]
      (is (= actual expected) "Quack is not a verb"))
    (let [expected true
          actual (verb-type? "Create")]
      (is (= actual expected) "Create is a verb"))
    (let [expected true
          actual (verb-type? "Reject")]
      (is (= actual expected) "Reject is a verb"))))

(deftest context-test
  (testing "identification of valid contexts"
    (let [expected false
          actual (context? "https://foo.bar/ban/")]
      (is (= actual expected)
          "Only `activitystreams-context-uri` is valid as a context on its own"))
    (let [expected true
          actual (context? activitystreams-context-uri)]
      (is (= actual expected)
          "`activitystreams-context-uri` is valid as a context on its own"))
    (let [expected false
          actual (context? [{:foo "bar"} "https://foo.bar/ban/"])]
      (is (= actual expected)
          "Only `activitystreams-context-uri` is valid as a context uri"))
    (let [expected true
          actual (context? [{:foo "bar"} activitystreams-context-uri])]
      (is (= actual expected)
          "`activitystreams-context-uri` is valid as a context uri"))
    (let [expected true
          actual (context? [activitystreams-context-uri {:foo "bar"}])]
      (is (= actual expected)
          "order of elements within a context should not matter"))))

(deftest actor-test
  (testing "identification of actors"
    (let [expected false
          actual (actor? (-> "resources/activitystreams-test-documents/simple0008.json" slurp clean first))]
      (is (= actual expected) "A Note is not an actor"))
    (let [expected false
          actual (actor? (-> "resources/activitystreams-test-documents/simple0020.json" slurp clean first :actor))]
      (is (= actual expected) "The Person in this file is not valid as an actor, because it lacks a context, https id, and outbox."))
    (let [o (assoc (-> "resources/activitystreams-test-documents/simple0020.json"
                       slurp
                       clean
                       first
                       :actor)
                   context-key activitystreams-context-uri
                   :id "https://example.org/@sally"
                   :inbox "https://example.org/@sally/inbox"
                   :outbox "https://example.org/@sally/outbox")
          expected true
          actual (actor? o)]
      (is (= actual expected) (str "The Person from this file is now valid as an actor, because it has a context." o)))))

(deftest ordered-collection-page-test
  (testing "identification of ordered collection pages."
    (let [expected false
          actual (ordered-collection-page? (-> "resources/activitystreams-test-documents/simple0020.json" slurp clean first))]
      (is (= actual expected) "A Note is not an ordered collection page."))
    (let [expected true
          actual (ordered-collection-page? (-> "resources/test_documents/outbox_page.json" slurp clean first))]
      (is (= actual expected) "A page from an outbox is an ordered collection page."))))