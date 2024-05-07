(ns simple-bank.handler-test
  (:require
   [clojure.test :refer [is deftest use-fixtures testing]]
   [muuntaja.core :as mc]
   [ring.mock.request :as mock]
   [simple-bank.test-fixtures :refer [*handler* with-db with-system]]))

(use-fixtures :each with-db with-system)

(defn <-json [m] (mc/decode "application/json" m))

(defn do-request [{:keys [method endpoint body]}]
  (*handler* (-> (mock/request method endpoint)
                 (mock/content-type "application/json")
                 (mock/json-body body))))

(deftest account-test
  (testing "Feature 1 - Create a bank account,"
    (testing "create a bank account"
      (let [result (do-request {:method :post
                                :endpoint "/account"
                                :body {:name "Mr. Black"}})]
        (def a result)))))
