(ns redis-tools
   (:require 
     [taoensso.carmine :as car]
     )
  )

(def connection-pool (car/make-conn-pool))

(def redises (atom {}))

(defonce setup-host "192.168.0.118")
(defonce setup-key "redis_inspector_data")

(defn setup-in-local-redis[]
  (let [listner-available (try (java.net.Socket. setup-host  6379) (catch Exception _ false))]
    (when listner-available  (car/with-conn (car/make-conn-pool)  (car/make-conn-spec  :host setup-host)  (car/get setup-key))))) 
 

(defn get-instances[]
  (or (setup-in-local-redis) @redises))

(defn get-instance-by-name[name]
  (get (get-instances) name))

(defn update-instance[name instance]
  (or
    (when-let [setup (setup-in-local-redis)] 
      (car/with-conn (car/make-conn-pool)  (car/make-conn-spec :host setup-host) (car/set setup-key (assoc setup name instance))))
    (reset! redises (assoc @redises name instance))))

(defn delete-instance[name]
  (or
    (when-let [setup (setup-in-local-redis)] 
      (car/with-conn (car/make-conn-pool)  (car/make-conn-spec :host setup-host) (car/set setup-key (dissoc setup name))))
    (reset! redises (dissoc @redises name))))
    

(defn conn-spec[instance]
  (car/make-conn-spec :host (:ip instance)))
  
(defmacro winstance [instance & body] `(car/with-conn connection-pool (conn-spec ~instance)  ~@body))
