(ns redis-tools
   (:require 
     [taoensso.carmine :as car]
     )
  )

(def connection-pool (car/make-conn-pool))

(defn conn-spec[instance]
  (car/make-conn-spec :host (:ip instance)))
  
(defmacro winstance [instance & body] `(car/with-conn connection-pool (conn-spec ~instance)  ~@body))

