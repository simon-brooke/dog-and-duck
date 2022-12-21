(ns dog-and-duck.utils.process
  (:require [clojure.string :refer [split]]))

(def pid
  "OK, this is hacky as fuck, but I hope it works. The problem is that the
   way to get the process id has changed several times during the history
   of Java development, and the code for one version of Java won't even compile
   in a different version."
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
    (eval (read-string cmd))))
