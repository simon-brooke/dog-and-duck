(ns clj-activitypub.internal.http-util
  "copied from [Jahfer's clj-activitypub library](https://github.com/jahfer/clj-activitypub). 
   If and when Jahfer issues a release of that library, this directory will be deleted and a 
   dependency on that library will be added to the project."
  (:require [clj-activitypub.internal.crypto :as crypto])
  (:import (java.net URLEncoder)
           (java.time OffsetDateTime ZoneOffset)
           (java.time.format DateTimeFormatter)))

(defn encode-url-params [params]
  (->> params
       (reduce-kv
        (fn [coll k v]
          (conj coll
                (str (URLEncoder/encode (name k)) "=" (URLEncoder/encode (str v)))))
        [])
       (interpose "&")
       (apply str)))

(defn date []
  (-> (OffsetDateTime/now (ZoneOffset/UTC))
      (.format DateTimeFormatter/RFC_1123_DATE_TIME)))

(defn digest
  "Accepts body from HTTP request and generates string
   for use in HTTP `Digest` request header."
  [body]
  (str "sha-256=" (crypto/sha256-base64 body)))