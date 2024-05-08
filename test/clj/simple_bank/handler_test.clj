(ns simple-bank.handler-test
  (:require
   [clojure.test :refer [is deftest use-fixtures testing]]
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
                :body "The resulting balance of the sending account should not fall below zero"}
               (do-request {:method :post
                            :endpoint (str "/account/" account-number1 "/send")
                            :body {:amount 80
                                   :account-number account-number2}
                            :return-raw-response true})))))))
