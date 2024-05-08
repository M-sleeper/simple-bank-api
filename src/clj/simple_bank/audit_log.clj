(ns simple-bank.audit-log
  (:require [simple-bank.db :as db]
            [simple-bank.exception :as exception]
            [ring.util.response :as response]))

(def AuditLog [:map
               [:sequence :int]
               [:credit {:optional true} number?]
               [:debit {:optional true} number?]
               [:description :string]])

(defn insert-log [value]
  (db/execute! {:insert-into [:audit-log]
                :values [value]}))

(defn insert-deposit-log [account-id amount]
  (insert-log {:amount amount
               :account-to account-id}))

(defn insert-withdraw-log [account-id amount]
  (insert-log {:amount amount
               :account-from account-id}))

(defn insert-transfer-log [account-from-id account-to-id amount]
  (insert-log {:amount amount
               :account-from account-from-id
               :account-to account-to-id}))

(defn select-logs [where-clause]
  (db/execute! {:select [:*]
                :from [:audit-log]
                :where where-clause}))

(defn marshal-audit-log
  [account-id {:keys [id account-from account-to amount]}]
  (-> (cond
        (and account-from account-to)
        (if (= account-to account-id)
          {:description (str "receive from #" account-from)
           :credit amount}
          {:description (str "send to #" account-to)
           :debit amount})

        account-to
        {:description "deposit"
         :credit amount}

        account-from
        {:description "withdraw"
         :debit amount})
      (assoc :sequence id)))

(defn get-account-audit-logs [account-id]
  (->> (select-logs [:or
                     [:= :account-from account-id]
                     [:= :account-to account-id]])
       (sort-by :date-created)
       reverse
       (map (partial marshal-audit-log account-id))))

(defn handle-account-audit [{{:keys [id]} :path-params}]
  (def a (get-account-audit-logs (Integer/parseInt id)))
  (def b (db/execute! {:select [:*]
                       :from [:audit-log]}))
  (response/response a))

(defmethod exception/handle-psql-exception {:constraint "fk_account_from"
                                            :table "audit_log"}
  [_ _]
  (response/not-found "Account not found"))
