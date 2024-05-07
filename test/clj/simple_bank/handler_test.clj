(ns simple-bank.handler-test
  (:require
   [clojure.test :refer [is deftest use-fixtures]]
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
  ;; TODO
  (is (= 1 0))
  )
