(ns dog-and-duck.quack.picky.distribution)

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

(defn distribution
  "Distribution of values of function `f` when applied to `vals`.
   
   I *know* there's a library function that does this, probably better, but I
   don't remember what it's called!"
  [f vals]
  (loop [result {} values vals]
    (if (empty? values) result 
        (let [r (apply f (list (first values)))]
                  (recur 
                   (assoc result r (if (result r) (inc (result r)) 1)) 
                   (rest values))))))