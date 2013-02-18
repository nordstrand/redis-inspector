(ns redis-tools
   (:require 
     [taoensso.carmine :as car]
     )
  )

(def connection-pool (car/make-conn-pool))


(defn conn-spec[instance]
   {:pre  [(not (nil? instance))]}
  (car/make-conn-spec :host (:ip instance)))
  
(declare get-instance-by-name)
(defmacro winstance-name [instance-name & body] 
  `(car/with-conn connection-pool (conn-spec (get-instance-by-name ~instance-name))  ~@body))
(defmacro winstance [instance & body] `(car/with-conn connection-pool (conn-spec ~instance)  ~@body))
(defmacro wsetup [& body] `(car/with-conn (car/make-conn-pool) (car/make-conn-spec :host setup-host)  ~@body))

(def redises (atom {}))

(defonce setup-host "127.0.0.1")
(defonce setup-key "redis_inspector_data")


(defn local-redis-available[] 
  (try (java.net.Socket. setup-host  6379) (catch Exception _ false)))
(defn setup-in-local-redis[]
  (when (local-redis-available)  (wsetup (car/get setup-key)))) 

(defn disable-setup-in-local-redis[]
  (let [s (wsetup (car/get setup-key))]
    (wsetup (car/del setup-key))
    (reset! redises s)))

(defn enable-setup-in-local-redis[]
    (wsetup (car/set setup-key @redises)))

                  
 
(defn get-instances[]
  (or (setup-in-local-redis)
      @redises))

(defn get-instance-by-name[name]
  (get (get-instances) name))

(defn update-instance[name instance]
  (or
    (when-let [setup (setup-in-local-redis)] 
      (wsetup (car/set setup-key (assoc setup name instance))))
    (reset! redises (assoc @redises name instance))))

(defn delete-instance[name]
  (or
    (when-let [setup (setup-in-local-redis)] 
      (wsetup (car/set setup-key (dissoc setup name))))
    (reset! redises (dissoc @redises name))))
    
