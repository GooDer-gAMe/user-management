(ns user-management.handler
  (:use ring.util.response)
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.json :as json]
            [ring.util.response :as resp]
            [user-management.in-memory-gateway :as gw]))

(defn home-handler [] "<b>GET</b><br>/user<br>/user/[id]<br><br><b>POST</b><br>/user<br><br><b>PUT</b><br>/user<br>")

(defn get-users-handler [tags get-users search-users]
  (let [users (if (empty? tags)
                (get-users)
                (search-users (str/split tags #",")))
        resp (-> (response users)
                 (resp/header "Content-Type" "application/json"))]
    resp))

(defn get-user-handler [id get-user]
  (let [user (get-user id)
        resp (-> (response user)
                 (resp/header "Content-Type" "application/json"))]
    resp))

(defn create-user-handler [user]
  (let [tags (:tags user)
        tags (if (nil? tags) (list) tags)
        new-user (gw/create-user (:first user) (:last user) tags)
        resp (-> (response new-user)
                 (resp/status 201)
                 (resp/header "Content-Type" "application/json"))]
    resp))

(defn update-user-handler [user]
  (let [user-was-updated (gw/update-user user)
        resp (-> (resp/status (if (= user-was-updated true) 204 400)))]
    resp))

(defroutes rest-routes
           (GET "/" [] (home-handler))
           (GET "/user" [tags] (get-users-handler tags gw/get-users gw/search-users))
           (POST "/user" {body :body} (let [user (walk/keywordize-keys body)] (create-user-handler user)))
           (PUT "/user" {body :body} (let [user (walk/keywordize-keys body)] (update-user-handler user)))
           (GET "/user/:id" [id] (get-user-handler id gw/get-user))
           (route/not-found "Not Found"))

(def rest-api
  (-> (wrap-defaults rest-routes (assoc-in site-defaults [:security :anti-forgery] false))
      (json/wrap-json-body)
      (json/wrap-json-response)))
