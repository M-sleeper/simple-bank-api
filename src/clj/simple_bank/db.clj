(ns simple-bank.db
  (:require
   [hikari-cp.core :as cp]
   [integrant.core :as ig]
   [migratus.core :as migratus]
   [next.jdbc.transaction]))

(defmethod ig/init-key ::db [_ {:keys [migration] :as db-spec}]
  (let [datasource (cp/make-datasource db-spec)
        migratus-config (assoc-in migration [:db :datasource] datasource)]
    (migratus/init migratus-config)
    (migratus/migrate migratus-config)
    datasource))

(defmethod ig/halt-key! ::db [_ pool]
  (cp/close-datasource pool))
