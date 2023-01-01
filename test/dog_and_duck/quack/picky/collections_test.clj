(ns dog-and-duck.quack.picky.collections-test
  (:require [clojure.test :refer [deftest is testing]]
            [dog-and-duck.quack.picky.collections :refer
             [collection-page-faults paged-collection-faults
              simple-collection-faults]]
            [dog-and-duck.quack.picky.constants :refer
             [activitystreams-context-uri]]
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

(deftest collection-identification-test
  (let [paged (-> "resources/test_documents/outbox.json" slurp clean first)
        page (-> "resources/test_documents/outbox_page.json"
                 slurp clean first)
        simple (-> "resources/activitystreams-test-documents/vocabulary-ex5-jsonld.json"
                   slurp clean first)
        not-a-collection {(keyword "@context") activitystreams-context-uri
                          :id "https://somewhere.out.there/object/14323:1671654380083"
                          :type "Test"}]
    (testing "Outbox sample"
      (let [actual (paged-collection-faults paged "OrderedCollection")
            expected nil] (is (= actual expected)
                              "There should be no faults from a perfect object")))
    (testing "Outbox page sample"
      (let [actual (collection-page-faults page "OrderedCollectionPage")
            expected nil] (is (= actual expected)
                              "There should be no faults from a perfect object")))
    (testing "Simple sample"
      (let [actual (set
                    (remove
                     nil?
                     (map :fault
                          (simple-collection-faults simple "Collection"))))
            expected #{:no-id-transient :no-context :not-object-reference}]
        (is (= actual expected)
            "There should be no faults from vocabulary-ex5-jsonld, either, but there are.")))
    (testing "Non-collection"
      (let [actual (empty? (simple-collection-faults not-a-collection "Collection"))
            expected false]
        (is (= actual expected)
            "There should be faults from anything which isn't a collection")))))