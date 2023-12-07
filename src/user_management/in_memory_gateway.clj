(ns user-management.in-memory-gateway
  (:require [clojure.set :as set]
            [user-management.user :as user]))

(def full-user-list
  (atom
    (list (user/make-user "John" "Diefenbaker" (list "Ontario" "Conservative"))
          (user/make-user "Jean" "Chrétien" (list "Quebec" "Liberal"))
          (user/make-user "Lester" "Pearson" (list "Ontario" "Liberal"))
          (user/make-user "Wilfred" "Laurier" (list "Quebec" "Liberal"))
          (user/make-user "Richard" "Bennett" (list "New Brunswick" "Conservative")))))

(defn get-users [] (deref full-user-list))

(defn get-user [expected-id]
  (first
    (for [{:keys [id] :as user} (deref full-user-list)
          :when (= id expected-id)]
      user)))

(defn create-user
  ([first last] (create-user first last (list)))
  ([first last tags]
   (let [new-user (user/make-user first last tags)]
     (swap! full-user-list conj new-user)
     new-user)))

(defn update-user [user]
  (let [resp (if (= (count (filter #(= (:id user) (:id %)) (deref full-user-list))) 1)
               (reset! full-user-list
                       (conj (filter #(not= (:id user) (:id %)) (deref full-user-list)) user))
               nil)]
    (if (nil? resp) false true)))

(defn search-users [tags]
  (filterv #(= (set/subset? (set tags) (set (:tags %))) true)
           (deref full-user-list)))