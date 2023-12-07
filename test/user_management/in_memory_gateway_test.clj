(ns user-management.in-memory-gateway-test
  (:require [clojure.set :as set]
            [clojure.test :refer :all]
            [user-management.in-memory-gateway :as gw]
            [user-management.user :as user]))

(deftest get-users-test
  (testing "user list was empty when it should contain data"
    (is (= (empty? (gw/get-users)) false))))

(deftest get-user-test--no-matching-id
  (testing "nil should have been returned"
    (is (= (gw/get-user "abc-123") nil))))

(deftest get-user-test
  (testing "a user should have been returned"
    (let [expected-user (first (gw/get-users))]
      (is (= (gw/get-user (:id expected-user)) expected-user)))))

(deftest create-user-test
  (testing "a new user should have been created"
    (let [original-user-count (count (gw/get-users))
          new-user (gw/create-user "William" "King")]
      (is (= (count (gw/get-users)) (inc original-user-count)))
      (is (= (some? new-user) true))
      (is (= (clojure.string/blank? (:id new-user)) false))
      (is (= (:first new-user) "William"))
      (is (= (:last new-user) "King")))))

(deftest create-user-test--with-tags
  (testing "a new user should have been created with tags"
    (let [original-user-count (count (gw/get-users))
          expected-tags (list "Ontario" "Liberal")
          new-user (gw/create-user "William" "King" expected-tags)]
      (is (= (count (gw/get-users)) (inc original-user-count)))
      (is (= (some? new-user) true))
      (is (= (clojure.string/blank? (:id new-user)) false))
      (is (= (:first new-user) "William"))
      (is (= (:last new-user) "King"))
      (is (= (:tags new-user) expected-tags)))))

(deftest update-user-test
  (testing "the user with matching id should have been updated"
    (let [expected-user-count (count (gw/get-users))
          original-user (first (gw/get-users))
          original-id (:id original-user)
          expected-updated-user (user/make-user original-id "new-first" "new-last" (list "updated"))
          expected-response (gw/update-user expected-updated-user)
          updated-user (gw/get-user original-id)]
      (is (= expected-response true))
      (is (= updated-user expected-updated-user))
      (is (= (count (gw/get-users)) expected-user-count)))))

(deftest update-user-test--wrong-id--no-change-should-be-made
  (testing "no user should have been updated"
    (let [expected-user-count (count (gw/get-users))
          uuid "abc-123"
          expected-updated-user (user/make-user uuid "new-first" "new-last" (list "updated"))
          expected-response (gw/update-user expected-updated-user)]
      (is (= expected-response false))
      (is (= (count (filterv #(= uuid (:id %)) (gw/get-users))) 0))
      (is (= (count (gw/get-users)) expected-user-count)))))

(deftest search-users
  (testing "correct number of users should have been retrieved"
    (let [tag-to-search (list "Conservative")
          expected-user-count (count (filterv #(= (set/subset? (set tag-to-search) (set (:tags %))) true) (gw/get-users)))
          searched-users (gw/search-users tag-to-search)]
      (is (= (count searched-users) expected-user-count))
      (is (= (count (filterv #(not= (set/subset? (set tag-to-search) (set (:tags %))) true) searched-users)) 0)))))

(deftest search-users--multiple-tags
  (testing "correct number of users should have been retrieved"
    (let [tags-to-search (list "Liberal" "Quebec")
          expected-user-count (count (filterv #(= (set/subset? (set tags-to-search) (set (:tags %))) true) (gw/get-users)))
          searched-users (gw/search-users tags-to-search)]
      (is (= (count searched-users) expected-user-count))
      (is (= (count (filterv #(not= (set/subset? (set tags-to-search) (set (:tags %))) true) searched-users)) 0)))))