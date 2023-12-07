(ns user-management.user-test
  (:require [clojure.test :refer :all]
            [user-management.user :as user]))

(deftest valid-user?-test
  (testing "user was not valid when it should be"
    (let [user (user/make-user "first" "last")]
      (is (= (user/valid-user? user) true))
      (is (= (clojure.string/blank? (:id user)) false)))
    ))

(deftest valid-user?-test--validation-should-fail
  (testing "user was valid when it should not be"
    (is (= (user/valid-user? {:id "abc" :first "firstName"}) false))))

(deftest update-user-test
  (testing "updated user should contain new values"
    (let [user (user/make-user "first" "last" (list "employee" "support level 1"))
          expected-first "new first name"
          expected-last "new last name"
          expected-tags (list "employee" "support level 2" "manager")
          expected-user (user/make-user (:id user) expected-first expected-last expected-tags)]
      (is (= (user/update-user user expected-first expected-last expected-tags)
             expected-user)))))