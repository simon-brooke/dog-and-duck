(ns dog-and-duck.scratch.scratch
  "Scratchpad where I try to understand how to do this stuff."
  (:require [clj-activitypub.core :as activitypub]
            [clj-activitypub.webfinger :as webfinger]
            [clj-pgp.generate :as pgp-gen]
            [clojure.walk :refer [keywordize-keys]]))

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
(:inbox account)

;; (def rsa (pgp-gen/rsa-keypair-generator 2048))
;; (def kp (pgp-gen/generate-keypair rsa :rsa-general))

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

