(ns user-management.user
  (:require [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]))

(s/def ::id string?)
(s/def ::first string?)
(s/def ::last string?)
(s/def ::tags coll?)

(s/def ::user (s/keys :req-un [::id ::first ::last ::tags]))
(s/def ::users (s/coll-of ::user))

(defn make-user
  ([first last] (make-user first last (list)))
  ([first last tags]
   {:id (.toString (random-uuid))
    :first first
    :last last
    :tags tags})
  ([id first last tags]
   {:id id
    :first first
    :last last
    :tags tags}))

(defn valid-user? [user]
  (let [valid (s/valid? ::user user)]
    (when (not valid)
      (log/warn (s/explain-str ::user user)))
    valid))

(defn update-user [user first last tags]
  (make-user (:id user) first last tags))