(ns simple-bank.handler
  (:require
   [malli.util :as mu]
   [muuntaja.core :as m]
   [muuntaja.middleware]
   [reitit.coercion.malli]
   [reitit.dev.pretty :as pretty]
   [reitit.ring :as ring]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.malli]
   [reitit.ring.middleware.exception :as exception]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [simple-bank.account :as account]
   [simple-bank.db :as db]))

(def swagger-routes
  ["/swagger.json"
   {:get
    {:info {:title "Simple Bank API"}
     :no-doc true
     :handler (swagger/create-swagger-handler)
     :tags [{:name "account"}]}}])

(def account-routes
  ["/account"
   {:tags #{"account"}}
   [""
    {:post
     {:parameters {:body (mu/select-keys account/Account [:name])}
      :responses  {200 {:body account/Account}}
      :handler    #'account/handle-create}}]
   ["/:id"
    [""
     {:get
      {:parameters {:path [:map [:id int?]]}
       :responses {200 {:body account/Account}}
       :handler #'account/handle-get}}]
    ["/deposit"
     {:post
      {:parameters {:path [:map [:id int?]]
                    :body [:map [:amount pos?]]}
       :responses {200 {:body account/Account}}
       :handler #'account/handle-deposit}}]]])

(defn handler
  [{:keys [datasource]}]
  (ring/ring-handler
   (ring/router
    [swagger-routes
     account-routes]
    {:exception pretty/exception
     :data {:coercion (reitit.coercion.malli/create
                       {:compile mu/closed-schema
                        :strip-extra-keys true
                        :default-values false})
            :muuntaja m/instance
            :middleware [swagger/swagger-feature
                         parameters/parameters-middleware
                         muuntaja/format-negotiate-middleware
                         muuntaja/format-response-middleware
                         [db/db-middleware datasource]
                         exception/exception-middleware
                         muuntaja/format-request-middleware
                         coercion/coerce-response-middleware
                         coercion/coerce-request-middleware]}})

   (ring/routes
    (swagger-ui/create-swagger-ui-handler
     {:path "/"})
    (ring/create-default-handler
     {:not-found (constantly {:status 404 :body "Not found"})}))))
