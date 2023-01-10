(defproject dog-and-duck "0.1.0"
  :cloverage {:output "docs/cloverage"
              :codecov? true
              :emma-xml? true}
  :codox {:metadata {:doc "**TODO**: write docs"
                     :doc/format :markdown}
          :output-path "docs/codox"
          :source-uri "https://github.com/simon-brooke/dog-and-duck/blob/master/{filepath}#L{line}"}
  :description "A playground for hacking ActivityPub stuff."
  :dependencies [[clj-activitypub/activitypub "0.52"]
                 [clojure.java-time "1.1.0"]
                 [com.taoensso/timbre "6.0.4"]
                 [hiccup "1.0.5"]
                 [mvxcvi/clj-pgp "1.1.0"]
                 [org.bouncycastle/bcpkix-jdk18on "1.72"] 
                 [org.clojars.simon_brooke/internationalisation "1.0.5"]
                 [org.clojure/clojure "1.11.1"]
                 [org.clojure/data.json "2.4.0"]
                 [org.clojure/math.numeric-tower "0.0.5"]
                 [org.clojure/spec.alpha "0.3.218"]
                 [org.clojure/tools.cli "1.0.214"]
                 [trptr/java-wrapper "0.2.3"]]
  :license {:name "GPL-2.0-or-later"
            :url "https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html"}
  :main dog-and-duck.quack.cli
  :plugins [[lein-cloverage "1.2.2"]
            [lein-codox "0.10.7"]]
  :profiles {:uberjar {:aot :all}}
  :repl-options {:init-ns dog-and-duck.quack.cli}
  :url "http://example.com/FIXME")
