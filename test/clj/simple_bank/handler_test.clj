(ns simple-bank.handler-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [muuntaja.core :as mc]
   [ring.mock.request :as mock]
   [simple-bank.test-fixtures :refer [*handler* with-db with-system]]))

(use-fixtures :each with-db with-system)

(defn <-json [m] (mc/decode "application/json" m))

(defn do-request [{:keys [method endpoint body return-raw-response]}]
  (cond-> (mock/request method endpoint)
    true (mock/content-type "application/json")
    body (mock/json-body body)
    true *handler*
    (not return-raw-response) :body
    (not return-raw-response) <-json))

(deftest create-account-test
  (testing "Feature 1 - Create a bank account,"
    (testing "new bank accounts start with a balance of 0"
      (is (= {:name "Mr. Black"
              :balance 0
              :account-number 1}
             (do-request {:method :post
                          :endpoint "/account"
                          :body {:name "Mr. Black"}}))))
    (testing "each bank account has a unique automatically generated account number"
      (is (= {:name "Mr. White"
              :balance 0
              :account-number 2}
             (do-request {:method :post
                          :endpoint "/account"
                          :body {:name "Mr. White"}}))))))

(deftest view-account-test
  (testing "Feature 2 - View a bank account,"
    (let [{:keys [account-number] :as new-account}
          (do-request {:method :post
                       :endpoint "/account"
                       :body {:name "Mr. Black"}})]
      (testing "you can retrieve an existing bank account."
        (is (= new-account
               (do-request {:method :get
                            :endpoint (str "/account/" account-number)})))))))

(deftest deposit-test
  (testing "Feature 3 - Deposit money to an account,"
    (let [{:keys [account-number] :as new-account}
          (do-request {:method :post
                       :endpoint "/account"
                       :body {:name "Mr. Black"}})]
      (testing "you can deposit money to an existing bank account."
        (is (= (assoc new-account :balance 100)
               (do-request {:method :post
                            :endpoint (str "/account/" account-number "/deposit")
                            :body {:amount 100}}))))
      (testing "deposit will increase existing amount"
        (is (= (assoc new-account :balance 120.6)
               (do-request {:method :post
                            :endpoint (str "/account/" account-number "/deposit")
                            :body {:amount 20.6}}))))
      (testing "you can only deposit a positive amount of money."
        (is (= {:status 400
                :headers {}
                :body "Amount must be a positive value"}
               (do-request {:method :post
                            :endpoint (str "/account/" account-number "/deposit")
                            :body {:amount -30}
                            :return-raw-response true})))))))

(deftest withdraw-test
  (testing "Feature 4 - Withdraw money from an account,"
    (let [{:keys [account-number] :as new-account}
          (do-request {:method :post
                       :endpoint "/account"
                       :body {:name "Mr. Black"}})]
      (do-request {:method :post
                   :endpoint (str "/account/" account-number "/deposit")
                   :body {:amount 100}})
      (testing "you can withdraw money from an existing bank account."
        (is (= (assoc new-account :balance 50)
               (do-request {:method :post
                            :endpoint (str "/account/" account-number "/withdraw")
                            :body {:amount 50}}))))
      (testing "you can only withdraw a positive amount of money."
        (is (= {:status 400
                :headers {}
                :body "Amount must be a positive value"}
               (do-request {:method :post
                            :endpoint (str "/account/" account-number "/withdraw")
                            :body {:amount -30}
                            :return-raw-response true}))))
      (testing "the resulting balance should not fall below zero"
        (is (= {:status 400
                :headers {}
                :body "Balance must not be a negative value"}
               (do-request {:method :post
                            :endpoint (str "/account/" account-number "/withdraw")
                            :body {:amount 130}
                            :return-raw-response true}))))
      (testing "you cannot withdraw from a non-existing account"
        (is (= {:status 404
                :headers {}
                :body "Account not found"}
               (do-request {:method :post
                            :endpoint (str "/account/" 12345678 "/withdraw")
                            :body {:amount 30}
                            :return-raw-response true})))))))

(deftest transfer-test
  (testing "Feature 5 - Transfer money between accounts,"
    (let [{account-number1 :account-number :as new-account1}
          (do-request {:method :post
                       :endpoint "/account"
                       :body {:name "Mr. Black"}})
          {account-number2 :account-number :as new-account2}
          (do-request {:method :post
                       :endpoint "/account"
                       :body {:name "Mr. White"}})]
      (do-request {:method :post
                   :endpoint (str "/account/" account-number1 "/deposit")
                   :body {:amount 100}})
      (testing "you can transfer money from one existing account to another existing account."
        (is (= (assoc new-account1 :balance 60)
               (do-request {:method :post
                            :endpoint (str "/account/" account-number1 "/send")
                            :body {:amount 40
                                   :account-number account-number2}})))
        (is (= (assoc new-account2 :balance 40)
               (do-request {:method :get
                            :endpoint (str "/account/" account-number2)}))))
      (testing "you cannot transfer money from an account to itself"
        (is (= {:status 400
                :headers {}
                :body "It is not possible to transfer money from an account to itself"}
               (do-request {:method :post
                            :endpoint (str "/account/" account-number1 "/send")
                            :body {:amount 40
                                   :account-number account-number1}
                            :return-raw-response true}))))
      (testing "the resulting balance of the sending account should not fall below zero"
        (is (= {:status 400
                :headers {}
                :body "Balance must not be a negative value"}
               (do-request {:method :post
                            :endpoint (str "/account/" account-number1 "/send")
                            :body {:amount 80
                                   :account-number account-number2}
                            :return-raw-response true}))))
      (testing "you cannot transfer money to a non-existing account"
        (is (= {:status 404
                :headers {}
                :body "Receiving account not found"}
               (do-request {:method :post
                            :endpoint (str "/account/" account-number1 "/send")
                            :body {:amount 10
                                   :account-number 12345678}
                            :return-raw-response true})))
        (is (= (assoc new-account1 :balance 60)
               (do-request {:method :get
                            :endpoint (str "/account/" account-number1)}))))
      (testing "you cannot transfer money from a non-existing account"
        (is (= {:status 404
                :headers {}
                :body "Sending account not found"}
               (do-request {:method :post
                            :endpoint (str "/account/" 12345678 "/send")
                            :body {:amount 10
                                   :account-number account-number1}
                            :return-raw-response true})))
        (is (= (assoc new-account1 :balance 60)
               (do-request {:method :get
                            :endpoint (str "/account/" account-number1)})))))))

(deftest audit-log-test
  (testing "Feature 6 - Retrieve account audit log"
    (let [{account-number1 :account-number}
          (do-request {:method :post
                       :endpoint "/account"
                       :body {:name "Mr. Black"}})
          {account-number2 :account-number}
          (do-request {:method :post
                       :endpoint "/account"
                       :body {:name "Mr. White"}})]
      (do-request {:method :post
                   :endpoint (str "/account/" account-number1 "/deposit")
                   :body {:amount 100}})
      (do-request {:method :post
                   :endpoint (str "/account/" account-number1 "/send")
                   :body {:amount 40
                          :account-number account-number2}})
      (do-request {:method :post
                   :endpoint (str "/account/" account-number1 "/withdraw")
                   :body {:amount 20}})
      (do-request {:method :post
                   :endpoint (str "/account/" account-number2 "/withdraw")
                   :body {:amount 10}})
      (testing "you can retrieve the audit log of an account"
        (is (= [{:sequence 3
                 :debit 20
                 :description "withdraw"}
                {:sequence 2
                 :debit 40
                 :description (str "send to #" account-number2)}
                {:sequence 1
                 :credit 100
                 :description "deposit"}]
               (do-request {:method :get
                            :endpoint (str "/account/" account-number1 "/audit")})))
        (is (= [{:sequence 4
                 :debit 10
                 :description "withdraw"}
                {:sequence 2
                 :credit 40
                 :description (str "receive from #" account-number1)}]
               (do-request {:method :get
                            :endpoint (str "/account/" account-number2 "/audit")})))))))

(deftest transfer-concurrency-test
  (testing "10000 concurrent transfer request"
    (let [account-numbers
          (pmap
           #(:account-number
             (do-request {:method :post
                          :endpoint "/account"
                          :body {:name (str "account-" %)}}))
           (range 10000))

          _deposit-results
          (doall
           (pmap #(do-request {:method :post
                               :endpoint (str "/account/" % "/deposit")
                               :body {:amount %}})
                 account-numbers))

          max-account-number (apply max account-numbers)
          transferring-accounts (remove #(= max-account-number %)
                                        account-numbers)

          _transfer-amounts-to-next-account-results
          (doall
           (pmap #(do-request {:method :post
                               :endpoint (str "/account/" % "/send")
                               :body {:amount %
                                      :account-number (inc %)}})
                 transferring-accounts))
          final-accounts (->> transferring-accounts
                              (pmap #(do-request {:method :get
                                                  :endpoint (str "/account/" %)}))
                              doall)]

      (is (every?
           (fn [{:keys [account-number balance]}]
             (= balance (dec account-number)))
           final-accounts)))))
