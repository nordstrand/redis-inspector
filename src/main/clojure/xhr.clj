(ns xhr
  (:require [cheshire.core :refer :all]
            [redis-tools :refer [get-instance-by-name]]
            [taoensso.carmine :as car]
             ))
 
(defn get-instance-stats [name]
  (let [instance  (get-instance-by-name name)
        instance-info (redis-tools/winstance instance (car/info*))
        ]
    (generate-string instance-info)))


  
