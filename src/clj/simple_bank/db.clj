(ns simple-bank.db
  (:require
   [hikari-cp.core :as cp]
   [honey.sql :as sql]
   [integrant.core :as ig]
   [migratus.core :as migratus]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]
   [next.jdbc.transaction]))

(defmethod ig/init-key ::db [_ {:keys [migration connection]}]
  (let [datasource (cp/make-datasource connection)
        migratus-config (assoc-in migration [:db :datasource] datasource)]
    (migratus/migrate migratus-config)
    datasource))

(defmethod ig/halt-key! ::db [_ pool]
  (cp/close-datasource pool))

(def ^:dynamic *datasource* nil)

(def db-middleware
  {:name ::middleware
   :wrap (fn [handler datasource]
           (fn [request]
             (binding [*datasource* datasource]
               (handler request))))})

(defn execute! [sqlmap]
  (jdbc/execute! *datasource*
                 (sql/format sqlmap)
                 {:builder-fn rs/as-unqualified-kebab-maps}))

(defmacro execute-in-transaction!
  [& body]
  `(jdbc/with-transaction [transaction-ds# *datasource*]
     (binding [*datasource* transaction-ds#]
       ~@body)))

(defn rollback []
  (.rollback *datasource*))
