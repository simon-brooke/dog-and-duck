(ns dog-and-duck.quack.picky.control-variables
  "Control variables for the picky validator.")

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

(def ^:dynamic *reify-refs*
  "If `true`, references to objects in fields will be reified and validated. 
   If `false`, they won't, but an `:info` level fault report will be generated.
   
   There are several things in the spec which, in a document, may correctly be
   either
   
   1. a fully fleshed out object, or
   2. a URI pointing to such an object.
   
   Obviously to fully validate a document we ought to reify all the refs and 
   check that they are themselves valid, but
   
   a. in some of the published test documents the URIs do not reference a
      valid document;
   b. there will be performance costs to reifying all the refs;
   c. in perverse cases, reifying refs might result in runaway recursion.
   
   TODO: I think that in production this should default to `true`."
  false)

(def ^:dynamic *reject-severity*
  "The severity at which the binary validator will return `false`.
   
   In practice documents seen in the wild do not typically appear to be 
   fully valid, and this does not matter. This allows the sensitivity of
   the binary validator (`dog-and-duck.quack.quack`) to be tuned. It's in
   this (`dog-and-duck.quack.picky`) namespace, not that one, because this
   namespace is where concerns about severity are handled."
  :must)
