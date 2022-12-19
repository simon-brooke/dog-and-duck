# Using ActivityPub

```clojure
user=> (require '[clj-activitypub.core :as activitypub])
nil
user=> (require '[clj-activitypub.webfinger :as webfinger])
nil
user=> (require '[clojure.walk :refer [keywordize-keys]])
nil
user=> (require '[clojure.pprint :refer [pprint]])
nil
user=> (def base-domain "mastodon.scot")
#'user/base-domain
user=> (def account-handle "@simon_brooke@mastodon.scot")
#'user/account-handle
user=> (in-ns 'user)
#object[clojure.lang.Namespace 0x525575 "user"]
user=> (activitypub/parse-account account-handle )
{:domain "mastodon.scot", :username "simon_brooke"}
user=> (map *1 [:domain :username])
("mastodon.scot" "simon_brooke")
user=> (apply webfinger/fetch-user-id *1)
"https://mastodon.scot/users/simon_brooke"
user=> (activitypub/fetch-user *1)
{"followers" "https://mastodon.scot/users/simon_brooke/followers", "inbox" "https://mastodon.scot/users/simon_brooke/inbox", "url" "https://mastodon.scot/@simon_brooke", "@context" ["https://www.w3.org/ns/activitystreams" "https://w3id.org/security/v1" {"identityKey" {"@type" "@id", "@id" "toot:identityKey"}, "EncryptedMessage" "toot:EncryptedMessage", "Ed25519Key" "toot:Ed25519Key", "devices" {"@type" "@id", "@id" "toot:devices"}, "manuallyApprovesFollowers" "as:manuallyApprovesFollowers", "schema" "http://schema.org#", "PropertyValue" "schema:PropertyValue", "Curve25519Key" "toot:Curve25519Key", "claim" {"@type" "@id", "@id" "toot:claim"}, "value" "schema:value", "Hashtag" "as:Hashtag", "movedTo" {"@id" "as:movedTo", "@type" "@id"}, "discoverable" "toot:discoverable", "messageType" "toot:messageType", "messageFranking" "toot:messageFranking", "cipherText" "toot:cipherText", "toot" "http://joinmastodon.org/ns#", "alsoKnownAs" {"@id" "as:alsoKnownAs", "@type" "@id"}, "featured" {"@id" "toot:featured", "@type" "@id"}, "featuredTags" {"@id" "toot:featuredTags", "@type" "@id"}, "Ed25519Signature" "toot:Ed25519Signature", "focalPoint" {"@container" "@list", "@id" "toot:focalPoint"}, "fingerprintKey" {"@type" "@id", "@id" "toot:fingerprintKey"}, "Device" "toot:Device", "publicKeyBase64" "toot:publicKeyBase64", "deviceId" "toot:deviceId", "suspended" "toot:suspended"}], "devices" "https://mastodon.scot/users/simon_brooke/collections/devices", "manuallyApprovesFollowers" false, "image" {"type" "Image", "mediaType" "image/jpeg", "url" "https://media.mastodon.scot/mastodon-scot-public/accounts/headers/109/252/274/874/045/781/original/e1f1823c4361fa27.jpg"}, "endpoints" {"sharedInbox" "https://mastodon.scot/inbox"}, "id" "https://mastodon.scot/users/simon_brooke", "publicKey" {"id" "https://mastodon.scot/users/simon_brooke#main-key", "owner" "https://mastodon.scot/users/simon_brooke", "publicKeyPem" "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2/6GgLJgJlPhhqFm1tUQ\noSLnWxhDwq4HlZIHrBsVjkSvUAnHKqq42Q/hta+fkWB8rmTFpmjLXDj/Fi0uejvT\nBc+KrLwfX/yR8+G87afGCRS3CaumoLJ7zkBIlsFzIKMoIke1D3QuHX95yGGXs+hp\nmyxt/+CXRyZjK7u9NG7SMRUlpwvOlpD12Aei35Nb8NSr03JvY8/WVMIbWrecyI0b\nAlwj6axxHx7J15Yo+aEtKzZ2OFKXf+sh0QF9BEnYcmVKYlR6kiOglLFHKdCBUSYi\ni9Flv00TydqlGvR5fpShBqORiy0M/FVtNXlz2sNBEsGB2meipkjh+cRLzTbYo4KL\nJwIDAQAB\n-----END PUBLIC KEY-----\n"}, "summary" "<p>Anarcho-syndicalist, autistic, crofter, cyclist, depressive, entrepreneur, geek, Zapatista. Politics &amp; environment, especially <a href=\"https://mastodon.scot/tags/LandReform\" class=\"mention hashtag\" rel=\"tag\">#<span>LandReform</span></a>. he/him.</p><p>Twitter: <span class=\"h-card\"><a href=\"https://mastodon.scot/@simon_brooke\" class=\"u-url mention\">@<span>simon_brooke</span></a></span><br />GitHub: simon-brooke<br />FetLife: Simon_Brooke</p><p>Credo: Life is harsh. What we can do - and what we should do - is strive to make it less harsh for the people around us.</p>", "attachment" [{"type" "PropertyValue", "name" "Home Page", "value" "<a href=\"https://www.journeyman.cc/~simon/\" target=\"_blank\" rel=\"nofollow noopener noreferrer me\"><span class=\"invisible\">https://www.</span><span class=\"\">journeyman.cc/~simon/</span><span class=\"invisible\"></span></a>"}], "name" "Simon Brooke", "tag" [{"type" "Hashtag", "href" "https://mastodon.scot/tags/landreform", "name" "#landreform"}], "published" "2022-10-29T00:00:00Z", "preferredUsername" "simon_brooke", "discoverable" true, "alsoKnownAs" ["https://mastodon.social/users/simon_brooke"], "featured" "https://mastodon.scot/users/simon_brooke/collections/featured", "featuredTags" "https://mastodon.scot/users/simon_brooke/collections/tags", "type" "Person", "outbox" "https://mastodon.scot/users/simon_brooke/outbox", "following" "https://mastodon.scot/users/simon_brooke/following", "icon" {"type" "Image", "mediaType" "image/png", "url" "https://media.mastodon.scot/mastodon-scot-public/accounts/avatars/109/252/274/874/045/781/original/172e8f7530627e87.png"}}
user=> (def sb (keywordize-keys *1))
#'user/sb
user=> (:outbox sb)
"https://mastodon.scot/users/simon_brooke/outbox"
user=> (require '[clojure.data.json :as json])
nil
user=> (slurp (:outbox sb))
Execution error (IOException) at sun.net.www.protocol.http.HttpURLConnection/getInputStream0 (HttpURLConnection.java:1894).
Server returned HTTP response code: 403 for URL: https://mastodon.scot/users/simon_brooke/outbox
user=> (pprint sb)
{:inbox "https://mastodon.scot/users/simon_brooke/inbox",
 :name "Simon Brooke",
 :@context
 ["https://www.w3.org/ns/activitystreams"
  "https://w3id.org/security/v1"
  {:schema "http://schema.org#",
   :messageType "toot:messageType",
   :messageFranking "toot:messageFranking",
   :identityKey {:@type "@id", :@id "toot:identityKey"},
   :Hashtag "as:Hashtag",
   :deviceId "toot:deviceId",
   :publicKeyBase64 "toot:publicKeyBase64",
   :value "schema:value",
   :Ed25519Key "toot:Ed25519Key",
   :featured {:@id "toot:featured", :@type "@id"},
   :Curve25519Key "toot:Curve25519Key",
   :discoverable "toot:discoverable",
   :focalPoint {:@container "@list", :@id "toot:focalPoint"},
   :suspended "toot:suspended",
   :fingerprintKey {:@type "@id", :@id "toot:fingerprintKey"},
   :Ed25519Signature "toot:Ed25519Signature",
   :cipherText "toot:cipherText",
   :EncryptedMessage "toot:EncryptedMessage",
   :alsoKnownAs {:@id "as:alsoKnownAs", :@type "@id"},
   :featuredTags {:@id "toot:featuredTags", :@type "@id"},
   :devices {:@type "@id", :@id "toot:devices"},
   :toot "http://joinmastodon.org/ns#",
   :movedTo {:@id "as:movedTo", :@type "@id"},
   :Device "toot:Device",
   :PropertyValue "schema:PropertyValue",
   :manuallyApprovesFollowers "as:manuallyApprovesFollowers",
   :claim {:@type "@id", :@id "toot:claim"}}],
 :featured
 "https://mastodon.scot/users/simon_brooke/collections/featured",
 :type "Person",
 :discoverable true,
 :icon
 {:type "Image",
  :mediaType "image/png",
  :url
  "https://media.mastodon.scot/mastodon-scot-public/accounts/avatars/109/252/274/874/045/781/original/172e8f7530627e87.png"},
 :following "https://mastodon.scot/users/simon_brooke/following",
 :summary
 "<p>Anarcho-syndicalist, autistic, crofter, cyclist, depressive, entrepreneur, geek, Zapatista. Politics &amp; environment, especially <a href=\"https://mastodon.scot/tags/LandReform\" class=\"mention hashtag\" rel=\"tag\">#<span>LandReform</span></a>. he/him.</p><p>Twitter: <span class=\"h-card\"><a href=\"https://mastodon.scot/@simon_brooke\" class=\"u-url mention\">@<span>simon_brooke</span></a></span><br />GitHub: simon-brooke<br />FetLife: Simon_Brooke</p><p>Credo: Life is harsh. What we can do - and what we should do - is strive to make it less harsh for the people around us.</p>",
 :publicKey
 {:id "https://mastodon.scot/users/simon_brooke#main-key",
  :owner "https://mastodon.scot/users/simon_brooke",
  :publicKeyPem
  "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2/6GgLJgJlPhhqFm1tUQ\noSLnWxhDwq4HlZIHrBsVjkSvUAnHKqq42Q/hta+fkWB8rmTFpmjLXDj/Fi0uejvT\nBc+KrLwfX/yR8+G87afGCRS3CaumoLJ7zkBIlsFzIKMoIke1D3QuHX95yGGXs+hp\nmyxt/+CXRyZjK7u9NG7SMRUlpwvOlpD12Aei35Nb8NSr03JvY8/WVMIbWrecyI0b\nAlwj6axxHx7J15Yo+aEtKzZ2OFKXf+sh0QF9BEnYcmVKYlR6kiOglLFHKdCBUSYi\ni9Flv00TydqlGvR5fpShBqORiy0M/FVtNXlz2sNBEsGB2meipkjh+cRLzTbYo4KL\nJwIDAQAB\n-----END PUBLIC KEY-----\n"},
 :endpoints {:sharedInbox "https://mastodon.scot/inbox"},
 :preferredUsername "simon_brooke",
 :id "https://mastodon.scot/users/simon_brooke",
 :alsoKnownAs ["https://mastodon.social/users/simon_brooke"],
 :outbox "https://mastodon.scot/users/simon_brooke/outbox",
 :url "https://mastodon.scot/@simon_brooke",
 :featuredTags
 "https://mastodon.scot/users/simon_brooke/collections/tags",
 :devices
 "https://mastodon.scot/users/simon_brooke/collections/devices",
 :image
 {:type "Image",
  :mediaType "image/jpeg",
  :url
  "https://media.mastodon.scot/mastodon-scot-public/accounts/headers/109/252/274/874/045/781/original/e1f1823c4361fa27.jpg"},
 :tag
 [{:type "Hashtag",
   :href "https://mastodon.scot/tags/landreform",
   :name "#landreform"}],
 :followers "https://mastodon.scot/users/simon_brooke/followers",
 :published "2022-10-29T00:00:00Z",
 :manuallyApprovesFollowers false,
 :attachment
 [{:type "PropertyValue",
   :name "Home Page",
   :value
   "<a href=\"https://www.journeyman.cc/~simon/\" target=\"_blank\" rel=\"nofollow noopener noreferrer me\"><span class=\"invisible\">https://www.</span><span class=\"\">journeyman.cc/~simon/</span><span class=\"invisible\"></span></a>"}]}
```
