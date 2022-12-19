(ns dog-and-duck.scratch.scratch
  "Scratchpad where I try to understand how to do this stuff."
  (:require [clj-activitypub.core :as activitypub]
            [clj-activitypub.webfinger :as webfinger]
            [clj-pgp.core :as pgp]
            [clj-pgp.keyring :as keyring]
            [clj-pgp.generate :as pgp-gen]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.pprint :refer [pprint]]))

;;; Use any ActivityPub account handle you like - for example, your own
(def account-handle "@simon_brooke@mastodon.scot")

(def handle (activitypub/parse-account account-handle))
(webfinger/fetch-user-id "mastodon.scot" "simon_brooke")
(apply webfinger/fetch-user-id (map handle [:domain :username]))

;;; Retrieve the account details from its home server
;;; (`keywordize-keys` is not necessary here but produces a more idiomatic clojure
;;; data structure)
(def account
  "Fetch my account to mess with"
  (let [handle (activitypub/parse-account account-handle)]
    (keywordize-keys
     (activitypub/fetch-user
      (apply webfinger/fetch-user-id (map handle [:domain :username]))))))

;;; examine what you got back!
(:outbox account)


(def rsa (pgp-gen/rsa-keypair-generator 2048))
(def kp (pgp-gen/generate-keypair rsa :rsa-general))

;; how we make a public/private key pair. But this key pair is not the one 
;; known to mastodon.scot as my key pair, so that doesn't get us very far...
;; I think.
(let [rsa (pgp-gen/rsa-keypair-generator 2048)
      kp (pgp-gen/generate-keypair rsa :rsa-general)
      public (-> kp .getPublicKey .getEncoded)
      private (-> kp .getPrivateKey .getPrivateKeyDataPacket .getEncoded)]
  (println (str "Public key:  " public))
  (println (str "Private key: " private))
  )