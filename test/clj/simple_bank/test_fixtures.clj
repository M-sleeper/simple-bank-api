(ns simple-bank.test-fixtures
  (:require
   [simple-bank.system :as system]))

(def system (system/start-system!))

(def handler (:simple-bank/app system))

(defn with-db [f]
  ;; TODO
  (f))
