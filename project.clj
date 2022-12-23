(defproject dog-and-duck "0.1.0-SNAPSHOT"
  :cloverage {:output "docs/cloverage"
              :codecov? true
              :emma-xml? true}
  :codox {:metadata {:doc "**TODO**: write docs"
                     :doc/format :markdown}
          :output-path "docs/codox"
          :source-uri "https://github.com/simon-brooke/dog-and-duck/blob/master/{filepath}#L{line}"}
  :description "A playground for hacking ActivityPub stuff."
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/data.json "2.4.0"]
                 [org.clojure/math.numeric-tower "0.0.5"]
                 [org.clojure/spec.alpha "0.3.218"]
                 [mvxcvi/clj-pgp "1.1.0"]
                 [org.bouncycastle/bcpkix-jdk18on "1.72"] ;; required by clj-activitypub
                 [clj-http "3.12.3"] ;; required by clj-activitypub
                 [cheshire "5.11.0"] ;; if this is not present, clj-http/client errors with 'json-enabled?'
                 [com.taoensso/timbre "6.0.4"]]
  :license {:name "GPL-2.0-or-later"
            :url "https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html"}
  :plugins [[lein-cloverage "1.2.2"]
            [lein-codox "0.10.7"]]
  :repl-options {:init-ns dog-and-duck.scratch.core}
  :url "http://example.com/FIXME")
