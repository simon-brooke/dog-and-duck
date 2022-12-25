(ns dog-and-duck.quack.picky-test
  (:require [clojure.test :refer [deftest is testing]]
            [dog-and-duck.quack.picky.constants :refer [activitystreams-context-uri]]
            [dog-and-duck.quack.picky.utils :refer [filter-severity]]
            [dog-and-duck.quack.picky :refer [object-faults
                                              persistent-object-faults]]))

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

(deftest object-fault-tests
  (let [perfect {(keyword "@context") activitystreams-context-uri
                 :id "https://somewhere.out.there/object/14323:1671654380083"
                 :type "Test"}]
    (testing "no faults returned from fully specified objects"
      (let [actual (object-faults perfect)
            expected nil]
        (is (= actual expected) "There should be no faults from a perfect object"))
      (let [o (assoc perfect :age 10 :name "Sally")
            actual (object-faults o)
            expected nil]
        (is (= actual expected) "Adding additional fields should not cause faults")))
    (testing "Faults returned from improperly specified objects"
      (let [o "not an object"
            r (object-faults o)]
        (let [expected 4
              actual (count r)]
          (is (= actual expected) "Expect to see four faults from a non-object"))
        (let [expected 1
              actual (count (filter-severity r :must))]
          (is (= actual expected) "Expect one :critical fault from a non-object"))
        (let [expected :not-an-object
              actual (:fault (first (filter-severity r :must)))]
          (is (= actual expected) "Expect the one :critical fault to be :not-an-object")))
      (let [o {}
            r (object-faults o)]
        (let [expected 3
              actual (count r)]
          (is (= actual expected) "Expect to see three faults from an empty object"))
        (let [expected 0
              actual (count (filter-severity r :must))]
          (is (= actual expected) "Expect no :critical faults from an empty object"))
        (let [expected :no-context
              actual (:fault (first (filter-severity r :minor)))]
          (is (= actual expected) "Expect the one non-:minor fault to be :no-context")))
      (let [o (dissoc perfect (keyword "@context"))
            r (object-faults o)
            expected :no-context
            actual (:fault (first (filter-severity r :all)))]
        (is (= actual expected)
            "Expect the one fault from an object with no context to be :no-context."))
      (let [o (dissoc perfect :id)
            r (object-faults o)
            expected :no-id-transient
            actual (:fault (first (filter-severity r :all)))]
        (is (= actual expected)
            "Expect the one fault from an object with no id to be :no-id-transient."))
      (let [o (dissoc perfect :type)
            r (object-faults o)
            expected :no-type
            actual (:fault (first (filter-severity r :all)))]
        (is (= actual expected)
            "Expect the one fault from an object with no type to be :no-type.")))))

(deftest peristent-object-fault-tests
  (let [perfect {(keyword "@context") activitystreams-context-uri
                 :id "https://somewhere.out.there/object/14323:1671654380083"
                 :type "Test"}]
    (testing "no faults returned from fully specified objects"
      (let [actual (persistent-object-faults perfect)
            expected nil]
        (is (= actual expected) "There should be no faults from a perfect object"))
      (let [o (assoc perfect :age 10 :name "Sally")
            actual (persistent-object-faults o)
            expected nil]
        (is (= actual expected) "Adding additional fields should not cause faults")))
    (testing "faults specific to persistent objects"
      (let [o (dissoc perfect :id)
            expected 1
            actual (count
                    (filter
                     #(= (:fault %) :no-id-persistent)
                     (persistent-object-faults o)))]
        (is (= actual expected)
            "The fault from a persistent object with no id should be :no-id-persistent"))
      (let [o (assoc perfect :id "http://somewhere.out.there/object/14323:1671654380083")
            expected :id-not-https
            actual (-> o persistent-object-faults first :fault)]
        (is (= actual expected)
            "The fault from a persistent object with an HTTP id should be :id-not-https"))
      (let [o (assoc perfect :id "not a valid URI")
            expected :id-not-uri
            actual (-> o persistent-object-faults first :fault)]
        (is (= actual expected)
            "The fault from a persistent object with an id which is not a valid URI should be :id-not-uri")))))