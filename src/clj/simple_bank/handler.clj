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
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]))

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
     {:parameters {:form [:map [:name string?]]}
      :responses  {200 {:body [:map
                               [:account-number int?]
                               [:name string?]
                               [:balance number?]]}}
      :handler
      (fn [{{:keys [db]} :env}]
        {:status 200
         :body "asdads"})}}]])

(defn app
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
            :datasource datasource
            :muuntaja m/instance
            :middleware [swagger/swagger-feature
                         parameters/parameters-middleware
                         muuntaja/format-request-middleware
                         coercion/coerce-request-middleware
                         muuntaja/format-response-middleware
                         coercion/coerce-response-middleware]}})

   (ring/routes
    (swagger-ui/create-swagger-ui-handler
     {:path "/"})
    (ring/create-default-handler
     {:not-found (constantly {:status 404 :body "Not found"})}))))
