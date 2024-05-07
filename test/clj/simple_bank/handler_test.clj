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
  (testing "Feature 2 - View a bank account, "
    (let [{:keys [account-number] :as new-account}
          (do-request {:method :post
                       :endpoint "/account"
                       :body {:name "Mr. Black"}})]
      (testing "you can retrieve an existing bank account."
        (is (= new-account
               (do-request {:method :get
                            :endpoint (str "/account/" account-number)})))))))

(deftest deposit-test
  (testing "Feature 3 - Deposit money to an account, "
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
        (is (= 400
               (:status
                (do-request {:method :post
                             :endpoint (str "/account/" account-number "/deposit")
                             :body {:amount -30}
                             :return-raw-response true}))))))))

(deftest withdraw-test
  (testing "Feature 4 - Withdraw money from an account"
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
        (is (= 400
               (:status
                (do-request {:method :post
                             :endpoint (str "/account/" account-number "/withdraw")
                             :body {:amount -30}
                             :return-raw-response true})))))
      (testing "the resulting balance should not fall below zero"
        (is (= 400
               (:status
                (do-request {:method :post
                             :endpoint (str "/account/" account-number "/withdraw")
                             :body {:amount 130}
                             :return-raw-response true}))))))))
