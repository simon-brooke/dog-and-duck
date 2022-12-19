(ns dog-and-duck.scratch.parser
  (:require [clojure.java.io :refer [file]]
            [clojure.string :refer [ends-with?]]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.data.json :as json]
            [dog-and-duck.quack.quack :as q]))

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

(defn clean
  "Take this `json` input, and return a sequence of ActivityPub objects 
   represented by it."
  [json]
  (let [feed (json/read-str json)]
    (map
     keywordize-keys
     (filter
      q/object?
      (cond (map? feed) (list (keywordize-keys feed))
            (coll? feed) (map keywordize-keys feed))))))

(clean (slurp "resources/activitystreams-test-documents/core-ex1-jsonld.json"))

(map
 #(when 
   (ends-with? (str %) ".json") 
    (let [objects (clean (slurp %))]
      (list (str %) 
            (count objects) 
            (map :type objects))))
 (file-seq (file "resources/activitystreams-test-documents")))
