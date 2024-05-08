(ns simple-bank.account
  (:require [simple-bank.db :as db]
            [ring.util.response :as response]
            [clojure.set :as set]
            [simple-bank.exception :as exception]))

(def Account [:map
              [:account-number int?]
              [:name string?]
              [:balance number?]])

(def PositiveAmount [:map {:closed true} [:amount 'pos?]])

(def api-db-key-mapping {:name :full-name
                         :account-number :id})

(defn ->db [account]
  (set/rename-keys account api-db-key-mapping))

(defn <-db [account]
  (set/rename-keys account (set/map-invert api-db-key-mapping)))

(defn insert-account [account]
  (first
   (db/execute! {:insert-into [:account]
                 :values [(->db account)]
                 :returning [:*]})))

(defn select-account [id]
  (first
   (db/execute! {:select [:*]
                 :from [:account]
                 :where [:= :id id]})))

(defn update-account [id updates]
  (first
   (db/execute! {:update [:account]
                 :set updates
                 :where [:= :id id]
                 :returning [:*]})))

(defn handle-create [{:keys [body-params]}]
  (response/response (<-db (insert-account body-params))))

(defn handle-get [{{:keys [id]} :path-params}]
  (if-let [account (some-> id Integer/parseInt select-account <-db)]
    (response/response account)
    (response/not-found "Account not found")))

(defn handle-deposit [{{:keys [amount]} :body-params
                       {:keys [id]} :path-params}]
  (if-let [updated-account (update-account
                            (Integer/parseInt id)
                            {:balance [:+ :balance amount]})]
    (response/response (<-db updated-account))
    (response/not-found "Account not found")))

(defn handle-withdraw [{{:keys [amount]} :body-params
                        {:keys [id]} :path-params}]
  (if-let [updated-account (update-account
                            (Integer/parseInt id)
                            {:balance [:- :balance amount]})]
    (response/response (<-db updated-account))
    (response/not-found "Account not found")))


(defmethod exception/handle-request-coercion-exception PositiveAmount
  [_ _]
  (response/bad-request "Amount must be a positive value"))

(defmethod exception/handle-psql-exception {:constraint "balance_non_negative"
                                           :table "account"}
  [_ _]
  (response/bad-request "Balance must not be a negative value"))
