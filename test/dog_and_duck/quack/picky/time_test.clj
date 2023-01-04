(ns dog-and-duck.quack.picky.time-test
  (:require [clojure.test :refer [deftest is testing]]
            [dog-and-duck.quack.picky.time :refer
             [date-time-property-or-fault xsd-date-time?]]))

;;;     Copyright (C) Simon Brooke, 2023

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

(deftest date-time-test
  (testing "xsd-date-time?"
    (let [expected true
          actual (xsd-date-time? "2002-05-30T09:00:00")]
          (is (= actual expected)))))