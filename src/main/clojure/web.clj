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
  (GET "/redis/:name" [name :as {flash :flash}] (redis-page/redis-show-instance name flash))
  (POST "/redis/:name/:key" [name key & params] (redis-page/redis-operate-on-key name key params))
  (POST "/redis/:name/delete" [name] (redis-page/redis-delete-instance name))
  (GET "/redis/:name/:key" [name key & params] (redis-object-page/redis-show-object name key params))
  (GET "/debug/:x" [x & p] (str "uri: " x " params:" p))
  (ANY "*" [] "Not found!"))


(defn format-request [name request]
  (with-out-str
    (println "-------------------------------")
    (println name)
    (clojure.pprint/pprint request)
    (println "-------------------------------")))

(defn wrap-spy [handler spyname include-body]
  (fn [request]
    (let [incoming (format-request (str spyname ":\n Incoming Request:") request)]
      (println incoming)
      (let [response (handler request)]
        (let [r (if include-body response (assoc response :body "#<?>"))
              outgoing (format-request (str spyname ":\n Outgoing Response Map:") r)]
          (println outgoing)
          response
          )))))

(def app
  (-> #'routes 
    (wrap-spy "start" true)
    wrap-bootstrap-resources
    trace/wrap-stacktrace 
    site
    ring.middleware.content-type/wrap-content-type
    ))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 8080))]
    (jetty/run-jetty #'app
                     {:port port :join? false})))
