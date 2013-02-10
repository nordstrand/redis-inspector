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
            [xhr :as xhr]                                    
            ))

(defroutes routes
  ;static routes
  (GET "/js/:j" [j] (resource-response j {:root "public/js"}))
  (GET "/favicon.ico" [] (resource-response "favicon.ico" {:root "public"}))
  (GET "/html/:h" [h] (resource-response h {:root "public/html"}))
  (GET "/css/:c" [c] (resource-response c {:root "public/css"}))
  
  ;xhr routes
  (POST "/xhr/repl" [& params] (web-repl/do-eval (:text params) :doh))
  (GET "/xhr/:name" [name] (xhr/get-instance-stats name)) 
  ;dynamic routes
  (GET "/" [ ] (redirect  "/redis"))
  (GET "/redis" {flash :flash} (redis-page/redis-list flash))
  (POST "/redis" [& params] (redis-page/redis-operate params))
  (GET "/redis/add" [& params] (redis-page/redis-show-form params))
  (POST "/redis/add" [& params] (redis-page/redis-submit params))
  (GET "/redis/:name" [name :as {flash :flash}] (redis-page/redis-show-instance name flash))
  (POST "/redis/:name/:key" [name key & params] (redis-page/redis-operate-on-key name key params))
  (POST "/redis/:name" [name & params] (redis-page/redis-operate-on-instance name params))
  (GET "/redis/:name/:key" [name key & params] (redis-object-page/redis-show-object name key params))
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
    wrap-bootstrap-resources
    (wrap-spy "start" true)
    trace/wrap-stacktrace 
    site
    ring.middleware.content-type/wrap-content-type
    etag/wrap-etag
    ))


(defn -main [& [port]]
  (let [port 8080]
    (jetty/run-jetty #'app
                     {:port port :join? false})))
