(ns simple-bank.test-fixtures
  (:require
   [clj-test-containers.core :as tc]
   [simple-bank.system :as system]))

(def ^:dynamic *datasource* nil)
(def ^:dynamic *handler* nil)

(defn with-db [f]
  (let [container
        (-> (tc/create {:image-name    "postgres"
                        :exposed-ports [5432]
                        :env-vars      {"POSTGRES_PASSWORD" "password"
                                        "POSTGRES_USER" "simple_bank"
                                        "POSTGRES_DB" "simple_bank"}})
            (tc/start!))]
    (f)
    (tc/stop! container)))

(defn with-system [f]
  (let [system (system/start-system!)]
    (binding [*handler* (:simple-bank.system/handler system)
              *datasource* (:simple-bank.db/db system)]
      (f))
    (system/stop-system! system)))
