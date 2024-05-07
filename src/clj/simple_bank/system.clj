(ns simple-bank.system
  (:require
   [clojure.java.io :as io]
   [hikari-cp.core :as cp]
   [integrant.core :as ig]
   [migratus.core :as migratus]
   [next.jdbc.transaction]
   [ring.adapter.jetty :as jetty]
   [simple-bank.handler :as handler])
  (:gen-class))

(defmethod ig/init-key ::app [_ deps]
  (handler/app deps))

(defmethod ig/init-key ::db [_ {:keys [migration] :as db-spec}]
  (let [datasource (cp/make-datasource db-spec)
        migratus-config (assoc-in migration [:db :datasource] datasource)]
    (migratus/init migratus-config)
    (migratus/migrate migratus-config)
    datasource))

(defmethod ig/halt-key! ::db [_ pool]
  (cp/close-datasource pool))

(defmethod ig/init-key ::server [_ {:keys [app port]}]
  (jetty/run-jetty app {:port port :join? false}))

(defmethod ig/halt-key! ::server [_ server]
  (.stop server))

(defn load-config []
  (-> "config/config.edn" io/resource slurp ig/read-string))

(defn start-system! []
  (ig/init (load-config)))

(defn stop-system! [system]
  (ig/halt! system))

(defn -main [& _args]
  (start-system!))
