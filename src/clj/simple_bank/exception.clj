(ns simple-bank.exception
  (:require
   [malli.core :as malli]
   [reitit.ring.middleware.exception :as exception]
   [taoensso.timbre :as log]))

(def sql-exception-dispatch-columns
  #{:constraint :table :column})

(defmulti handle-psql-exception
  (fn [exception _request]
    (->> (bean (.getServerErrorMessage exception))
         (filter #(and (sql-exception-dispatch-columns (key %))
                       (val %)))
         (into {}))))

(defmethod handle-psql-exception :default
  [exception request]
  (exception/default-handler exception request))

(defmulti handle-request-coercion-exception
  (fn [exception _request]
    (-> exception ex-data :schema malli/form)))

(defmethod handle-request-coercion-exception :default
  [exception request]
  ((:reitit.coercion/request-coercion exception/default-handlers)
   exception
   request))

(def exception-middleware
  (exception/create-exception-middleware
   (merge
    exception/default-handlers
    {:reitit.coercion/request-coercion handle-request-coercion-exception

     org.postgresql.util.PSQLException handle-psql-exception

     ::exception/wrap (fn [handler exception request]
                        (log/error exception)
                        (handler exception request))})))
