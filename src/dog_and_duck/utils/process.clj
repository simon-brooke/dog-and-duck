(ns dog-and-duck.utils.process
  (:require [clojure.string :refer [split]]))

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

(def get-pid
  "Get the process id of the current process.
   
   OK, this is hacky as fuck, but I hope it works. The problem is that the
   way to get the process id has changed several times during the history
   of Java development, and the code for one version of Java won't even compile
   in a different version."
  (memoize
   (fn []
     (let [java-version (read-string (apply str (take 2
                                                   (split
                                                    (System/getProperty "java.version")
                                                    #"[_\.]"))))
        cmd (case java-version
              18 "(let [[_ pid hostname]
                    (re-find
                      #\"^(\\d+)@(.*)\"
                      (.getName
                        (java.lang.management.ManagementFactory/getRuntimeMXBean)))]
                    pid)"
              (19 110) "(.pid (java.lang.ProcessHandle/current))"
              111 "(.getPid (java.lang.management.ManagementFactory/getRuntimeMXBean))"
              ":default")]
    (eval (read-string cmd))))))

(def get-hostname 
  "return the hostname of the current host.
   
   Java's methods for getting the hostname are quite startlingly slow, we
   do not want todo this repeatedly!"
  (memoize (fn [] (.. java.net.InetAddress getLocalHost getHostName))))