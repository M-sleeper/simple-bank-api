(ns simple-bank.account
  (:require [simple-bank.db :as db]
            [clojure.set :as set]))

(def Account [:map
              [:account-number int?]
              [:name string?]
              [:balance number?]])

(def api-db-key-mapping {:name :full-name
                         :account-number :id})

(defn ->db [account]
  (set/rename-keys account api-db-key-mapping))

(defn <-db [account]
  (set/rename-keys account (set/map-invert api-db-key-mapping)))

(defn create! [account]
  (first
   (db/execute! {:insert-into [:account]
                 :values [(->db account)]
                 :returning [:*]})))

(defn handle-create [{:keys [body-params]}]
  {:status 200
   :body (<-db (create! body-params))})
