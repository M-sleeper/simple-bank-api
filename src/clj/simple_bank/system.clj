(ns simple-bank.system
  (:require
   [clojure.java.io :as io]
   [integrant.core :as ig]
   [next.jdbc.transaction]
   [ring.adapter.jetty :as jetty]
   [simple-bank.handler :as handler])
  (:gen-class))

(defmethod ig/init-key ::handler [_ deps]
  (handler/handler deps))

(defmethod ig/init-key ::server [_ {:keys [handler port]}]
  (jetty/run-jetty handler {:port port :join? false}))

(defmethod ig/halt-key! ::server [_ server]
  (.stop server))

(defn load-config []
  (-> "config/config.edn" io/resource slurp ig/read-string))

(defn start-system!
  ([] (start-system! (load-config)))
  ([config] (ig/init config)))

(defn stop-system! [system]
  (ig/halt! system))

(defn -main [& _args]
  (start-system!))
