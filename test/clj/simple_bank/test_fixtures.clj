(ns simple-bank.test-fixtures
  (:require
   [clj-test-containers.core :as tc]
   [simple-bank.system :as system]))

(def ^:dynamic *datasource* nil)
(def ^:dynamic *handler* nil)
(def ^:dynamic *db-container* nil)

(def db-container-options
  {:image-name    "postgres"
   :exposed-ports [5432]
   :env-vars      {"POSTGRES_PASSWORD" "password"
                   "POSTGRES_USER"     "simple_bank"
                   "POSTGRES_DB"       "simple_bank"}
   :wait-for      {:wait-strategy   :log
                   :message         "accept connections"
                   :times 2
                   :startup-timeout 10}})

(defn with-db [f]
  (let [container (-> (tc/create db-container-options)
                      (tc/start!))]
    (binding [*db-container* container]
      (f))))

(defn with-system [f]
  (let [port (get-in *db-container* [:mapped-ports 5432])
        config (assoc-in (system/load-config)
                         [:simple-bank.db/db :connection :port-number] port)
        system (system/start-system! config)]
    (binding [*handler* (:simple-bank.system/handler system)
              *datasource* (:simple-bank.db/db system)]
      (f))
    (system/stop-system! system)))
