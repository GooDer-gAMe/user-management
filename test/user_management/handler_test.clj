(ns user-management.handler-test
  (:require [clojure.data.json :as json]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [user-management.in-memory-gateway :as gw]
            [user-management.handler :as handler]
            [user-management.user :as user]))

(def new-user-uuid "abcd1234")

(def default-list-of-mock-users
  (list (user/make-user "Mitch" "Marner" (list "RW" "CAN"))
        (user/make-user "Auston" "Matthews" (list "C" "USA"))
        (user/make-user "William" "Nylander" (list "LW" "SWE"))
        (user/make-user "Nick" "Robertson" (list "LW" "USA"))
        (user/make-user "Max" "Domi" (list "RW" "CAN"))))

(def full-list-of-mock-users
  (atom default-list-of-mock-users))

(defn mock-get-users [] (deref full-list-of-mock-users))

(defn mock-get-user [expected-id]
  (first
    (for [{:keys [id] :as user} (deref full-list-of-mock-users)
          :when (= id expected-id)]
      user)))

(defn mock-search-user [tags]
  (filterv #(= (set/subset? (set tags) (set (:tags %))) true)
           (deref full-list-of-mock-users)))

(defn mock-create-user
  ([first last] (mock-create-user first last (list)))
  ([first last tags]
   (let [new-user (user/make-user new-user-uuid first last tags)]
     (swap! full-list-of-mock-users conj new-user)
     new-user)))

(defn mock-update-user [user]
  (let [resp (if (= (count (filter #(= (:id user) (:id %)) (deref full-list-of-mock-users))) 1)
               (reset! full-list-of-mock-users
                       (conj (filter #(not= (:id user) (:id %)) (deref full-list-of-mock-users)) user))
               nil)]
    (if (nil? resp) false true)))

(defn test-setup [f]
  (with-redefs [gw/get-users mock-get-users
                gw/get-user mock-get-user
                gw/search-users mock-search-user
                gw/create-user mock-create-user
                gw/update-user mock-update-user]
    (reset! full-list-of-mock-users default-list-of-mock-users)
    (f)))

(use-fixtures :each test-setup)

(deftest rest-api-default-routes-test
  (testing "main route"
    (let [response (handler/rest-api (mock/request :get "/"))]
      (is (= (:status response) 200))
      (is (= (:body response) "<b>GET</b><br>/user<br>/user/[id]<br><br><b>POST</b><br>/user<br><br><b>PUT</b><br>/user<br>"))))

  (testing "not-found route"
    (let [response (handler/rest-api (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))

(deftest rest-api-get-users
  (testing "get users route"
    (let [response (handler/rest-api (mock/request :get "/user"))]
      (is (= (:status response) 200))
      (is (= (:body response) (json/write-str (deref full-list-of-mock-users))))
      (is (= (str/includes? (json/write-str (:headers response)) "application\\/json") true)))))

(deftest rest-api-get-user
  (testing "get user route"
    (let [expected-user (first (deref full-list-of-mock-users))
          response (handler/rest-api (mock/request :get (str "/user/" (:id expected-user))))]
      (is (= (:status response) 200))
      (is (= (:body response) (json/write-str expected-user)))
      (is (= (str/includes? (json/write-str (:headers response)) "application\\/json") true)))))

(deftest rest-api-search-user
  (testing "search users route"
    (let [tag-to-search "LW"
          expected-users (mock-search-user (str/split tag-to-search #","))
          response (handler/rest-api (mock/request :get (str "/user?tags=" tag-to-search)))]
      (is (= (:status response) 200))
      (is (= (:body response) (json/write-str expected-users)))
      (is (= (str/includes? (json/write-str (:headers response)) "application\\/json") true)))))

(deftest rest-api-search-user--multiple-tags
  (testing "search users route with multiple tags"
    (let [tags-to-search "CAN,RW"
          expected-users (filterv #(= (set/subset? (set (str/split tags-to-search #",")) (set (:tags %))) true) (deref full-list-of-mock-users))
          response (handler/rest-api (mock/request :get (str "/user?tags=" tags-to-search)))]
      (is (= (:status response) 200))
      (is (= (:body response) (json/write-str expected-users)))
      (is (= (str/includes? (json/write-str (:headers response)) "application\\/json") true)))))

(deftest rest-api-new-user
  (testing "create new user route"
    (let [expected-first "first"
          expected-last "last"
          expected-tags (list "tag1" "tag2")
          expected-new-user (user/make-user new-user-uuid expected-first expected-last expected-tags)
          response (handler/rest-api (-> (mock/request :post "/user")
                                         (mock/json-body {:first expected-first :last expected-last :tags expected-tags})))]
      (is (= (:status response) 201))
      (is (= (:body response) (json/write-str expected-new-user)))
      (is (= (str/includes? (json/write-str (:headers response)) "application\\/json") true))
      (is (= (first (filter #(= new-user-uuid (:id %)) (deref full-list-of-mock-users))) expected-new-user)))))

(deftest rest-api-new-user--without-tags
  (testing "create new user route"
    (let [expected-first "first"
          expected-last "last"
          expected-new-user (user/make-user new-user-uuid expected-first expected-last (list))
          response (handler/rest-api (-> (mock/request :post "/user")
                                         (mock/json-body {:first expected-first :last expected-last})))]
      (is (= (:status response) 201))
      (is (= (:body response) (json/write-str expected-new-user)))
      (is (= (str/includes? (json/write-str (:headers response)) "application\\/json") true))
      (is (= (first (filter #(= new-user-uuid (:id %)) (deref full-list-of-mock-users))) expected-new-user)))))

(deftest rest-api-update-user
  (testing "update user route"
    (let [id-to-update (:id (last (deref full-list-of-mock-users)))
          expected-user (user/make-user id-to-update "new-first" "new-last" (list "new-tag-1" "new-tag-2"))
          response (handler/rest-api (-> (mock/request :put "/user")
                                         (mock/json-body expected-user)))]
      (is (= (:status response) 204))
      (is (= (first (filter #(= id-to-update (:id %)) (deref full-list-of-mock-users))) expected-user)))))

(deftest rest-api-update-user--without-tags
  (testing "update user route"
    (let [expected-user (user/make-user "fake-id-1234" "new-first" "new-last" (list "new-tag-1" "new-tag-2"))
          response (handler/rest-api (-> (mock/request :put "/user")
                                         (mock/json-body expected-user)))]
      (is (= (:status response) 400))
      (is (= (count (filter #(= "new-first" (:first %)) (deref full-list-of-mock-users))) 0)))))