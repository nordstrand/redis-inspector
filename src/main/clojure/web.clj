(ns web
  (:gen-class :main true) 
  (:require [compojure.core :refer [defroutes GET POST ANY]]
            [compojure.handler :refer [site]]
            [ring.middleware.stacktrace :as trace]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :refer [response redirect resource-response]]
            [redis :as redis-page]
            [hiccup.bootstrap.middleware :refer [wrap-bootstrap-resources]]
            [redis-object :as redis-object-page]
            ))

(defroutes routes
  (GET "/favicon.ico" [] (resource-response "favicon.ico" {:root "public"}))
  (GET "/" [ ] (redirect  "/redis"))
  (GET "/redis" {flash :flash} (redis-page/redis-list flash))
  (GET "/redis/add" [& params] (redis-page/redis-show-form params))
  (GET "/redis/:name/edit" [name] (redis-page/redis-show-edit-form name))
  (POST "/redis/add" [& params] (redis-page/redis-submit params))
  (GET "/redis/:name" [name] (redis-page/redis-show-instance name))
  (POST "/redis/:name/delete" [name] (redis-page/redis-delete-instance name))
  (GET "/redis/:name/:key" [name key & params] (redis-object-page/redis-show-object name key params))
  (GET "/debug/:x" [x & p] (str "uri: " x " params:" p))
  (ANY "*" [] "Not found!"))

(def app
  (-> #'routes 
    wrap-bootstrap-resources
    trace/wrap-stacktrace 
    site
    ring.middleware.content-type/wrap-content-type
    ))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 8080))]
    (jetty/run-jetty #'app
                     {:port port :join? false})))
