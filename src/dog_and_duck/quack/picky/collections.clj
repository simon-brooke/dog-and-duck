(ns dog-and-duck.quack.picky.collections
  (:require [dog-and-duck.quack.picky.utils :refer [concat-non-empty
                                                    cond-make-fault-object
                                                    object-faults
                                                    object-reference-or-faults]]))


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

(defn paged-collection-faults
  "Return a list of faults found in `x` considered as a paged collection
   object of this sub-`type`, or `nil` if none are found."
  [x type]
  (concat-non-empty
   (object-faults x type)
   (list (object-reference-or-faults x type :critical :expected-collection)
         (cond-make-fault-object (integer? (:totalItems x)) :should :no-total-items)
         (object-reference-or-faults (:first x) nil :must :no-first-page)
         (object-reference-or-faults (:last x) nil :should :no-last-page))))

(defn simple-collection-faults
  "Return a list of faults found in `x` considered as a non-paged collection
   object of this sub-`type`, or `nil` if none are found."
  [x type]
  (concat-non-empty
   (object-faults x type)
   (cons
    (list (object-reference-or-faults x type :critical :expected-collection)
          (cond-make-fault-object (integer? (:totalItems x)) :should :no-total-items)
          (cond-make-fault-object (coll? (:items x)) :must :no-items-collection))
    (map #(object-reference-or-faults % nil :must :not-object-reference) (:items x)))))

(defn collection-page-faults
  [x type]
  (concat-non-empty
   (simple-collection-faults x type)
   (list
    (object-reference-or-faults (:partOf x)
                                (apply str (drop-last 4 type))
                                :should
                                :n-part-of)
    (object-reference-or-faults (:next x) type :minor :no-next-page)
    (object-reference-or-faults (:prev x) type :minor :no-prev-page))))
