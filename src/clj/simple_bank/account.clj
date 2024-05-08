(ns simple-bank.account
  (:require
   [clojure.set :as set]
   [ring.util.response :as response]
   [simple-bank.audit-log :as audit-log]
   [simple-bank.db :as db]
   [simple-bank.exception :as exception]))

(def Account [:map
              [:account-number int?]
              [:name string?]
              [:balance number?]])

(def PositiveAmount [:map {:closed true}
                     [:amount 'pos?]])

(def Transfer [:map {:closed true}
               [:amount 'pos?]
               [:account-number :int]])

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

(defn change-amount-query-part [modifier amount]
  {:balance [modifier :balance amount]})

(defn get-update-account-query [id updates]
  {:update [:account]
   :set updates
   :where [:= :id id]
   :returning [:*]})

(defn update-account [id updates]
  (-> (get-update-account-query id updates) db/execute! first))

(defn deposit-to-account [id amount]
  (db/execute-in-transaction!
   (audit-log/insert-deposit-log id amount)
   (update-account id (change-amount-query-part :+ amount))))

(defn withdraw-from-account [id amount]
  (db/execute-in-transaction!
   (audit-log/insert-withdraw-log id amount)
   (update-account id (change-amount-query-part :- amount))))

(defn transfer [{:keys [sending-account-id
                        receiving-account-id
                        amount]}]
  (db/execute-in-transaction!
   (let [results [(update-account sending-account-id
                                  (change-amount-query-part :- amount))
                  (update-account receiving-account-id
                                  (change-amount-query-part :+ amount))]]
     (if (some empty? results)
       (db/rollback)
       (audit-log/insert-transfer-log sending-account-id
                                      receiving-account-id
                                      amount))
     results)))

(defn handle-create [{:keys [body-params]}]
  (response/response (<-db (insert-account body-params))))

(defn handle-get [{{:keys [id]} :path-params}]
  (if-let [account (some-> id Integer/parseInt select-account <-db)]
    (response/response account)
    (response/not-found "Account not found")))

(defn handle-deposit [{{:keys [amount]} :body-params
                       {:keys [id]} :path-params}]
  (-> (Integer/parseInt id)
      (deposit-to-account amount)
      <-db
      response/response))

(defn handle-withdraw [{{:keys [amount]} :body-params
                        {:keys [id]} :path-params}]
  (-> (Integer/parseInt id)
      (withdraw-from-account amount)
      <-db
      response/response))

(defn handle-send [{{:keys [amount account-number]} :body-params
                    {:keys [id]} :path-params}]
  (let [sending-account-id (Integer/parseInt id)
        receiving-account-id account-number]
    (if (= sending-account-id receiving-account-id)
      (response/bad-request
       "It is not possible to transfer money from an account to itself")
      (let [[updated-sending-account
             updated-receiving-account]
            (transfer
             {:sending-account-id sending-account-id
              :receiving-account-id receiving-account-id
              :amount amount})]
        (cond
          (nil? updated-sending-account)
          (response/not-found "Sending account not found")

          (nil? updated-receiving-account)
          (response/not-found "Receiving account not found")

          :else (response/response (<-db updated-sending-account)))))))

(defmethod exception/handle-request-coercion-exception PositiveAmount
  [_ _]
  (response/bad-request "Amount must be a positive value"))

(defmethod exception/handle-psql-exception {:constraint "balance_non_negative"
                                            :table "account"}
  [_ _]
  (response/bad-request "Balance must not be a negative value"))
