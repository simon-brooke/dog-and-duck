(defproject dog-and-duck "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/data.json "2.4.0"]
                 [org.clojure/math.numeric-tower "0.0.5"]
                 [org.clojure/spec.alpha "0.3.218"]
                 [mvxcvi/clj-pgp "1.1.0"]
                 [org.bouncycastle/bcpkix-jdk18on "1.72"] ;; required by clj-activitypub
                 [clj-http "3.12.3"] ;; required by clj-activitypub
                 [cheshire "5.11.0"] ;; if this is not present, clj-http/client errors with 'json-enabled?'
                 ]
  :repl-options {:init-ns dog-and-duck.scratch.core})
