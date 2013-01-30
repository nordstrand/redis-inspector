(ns web
  (:gen-class :main true) 
  (:require [compojure.core :refer [defroutes GET POST ANY]]
            [compojure.handler :refer [site]]
            [ring.middleware.stacktrace :as trace]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :refer [response]]
            [environ.core :refer [env]]
            [formative.core :as f]
            [formative.parse :as fp]
            [hiccup.page :as page]
            [redis :as redis-page]
            [hiccup.bootstrap.middleware :refer [wrap-bootstrap-resources]]
            [redis-object :as redis-object-page]
            [clojure.pprint :refer [pprint]]))


(defroutes routes
  (GET "/" [& params]  (ring.util.response/redirect  "/redis"))
  ;(GET "/favicon.ico" [& params] (ring.util.response/resource-response "favicon.ico" {:root "public"}))
  ;(GET "/f" [& params] (str "hej" (ring.util.response/resource-response "favicon.ico" {:root "public"})))
   ;(POST "/" [& params] (submit-demo-form params))
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
    ))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty #'app
                     {:port port :join? false})))

;; For interactive development:
;; (.stop server)
;; (def server (-main))