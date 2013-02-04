(ns redis
  (:require
            [ring.util.response :refer [redirect]]
            [formative.core :as f]
            [formative.parse :as fp]
            [hiccup.page :as page]
            [web-tools :refer [layout breadcrumb]]
            [clojure.pprint :refer [pprint]]
            [taoensso.carmine :as car]
            [redis-tools :refer [winstance get-instance-by-name get-instances update-instance delete-instance 
                                 setup-in-local-redis setup-host setup-key local-redis-available enable-setup-in-local-redis 
                                 disable-setup-in-local-redis]]
            ))


(defn validate-ip [values]
  (when-not (re-find #"^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$" (:ip values))
    {:keys [:ip] :msg "Valid IP address i required"})
  )

(def redis-form
  {:enctype "multipart/form-data"
   :action "/redis/add"
   :submit-label "Save"
   :cancel-href "/redis"
   :fields [{:name :h1 :type :heading :text "Redis instance"}
            {:name :name}
            {:name :ip}
            {:name :port :datatype :int}
            {:name :db   :datatype :int}
            ]
   :validations [[:required [:name :ip :port]]                ]
   :validator validate-ip
   })

(def instance-form
  {:method "post"
   :renderer :inline
   :submit-label "Delete"
   :fields [{:name :operation :type :hidden }
            ]
   })

(def key-form
  {:method "post"
   :renderer :inline
   :submit-label "Delete"
   :fields [{:name :operation :type :hidden }
            ]
   })

(defn redis-show-instance [name flash]
    (layout
      (breadcrumb name)
      (let [instance  (get-instance-by-name name)
            instance-info (redis-tools/winstance instance (car/info*))
            slaves (map #(last (first (re-seq #"(.*),.*,.*"  (val %1)))) 
                        (filter #(re-seq #"slave[0-9]" (key %1)) instance-info))
            ]
        
        [:div 
         (when flash [:div.alert.alert-success flash])
         [:div.pull-left {:style "width: 55%"}
          [:table.table.table-bordered
           [:thead
            [:tr
             [:th "Key"]
             [:th "Type"]
             [:th "TTL"]
            [:td ]]]
           
           (for [key (sort (winstance instance (car/keys "*")))]
             [:tr
             [:td [:a {:href (str "/redis/" (:name instance) "/" key)} key]]
             [:td (winstance instance (car/type key))]
             [:td (winstance instance (car/ttl key))]
             [:td  
              (f/render-form (assoc key-form :action (str "/redis/" (:name instance) "/" key )
                                     :values{ :operation "delete" })) 
              ]]
             )
           ]]        
         [:div.pull-right {:style "width: 43%"}
           [:h4 "Instance"]
          [:ul
           (for [[k v] instance]
             [:li k ": " v])]
          (for [[name details] (get-instances) :when (and (-> details :ip   (= (get instance-info "master_host")))                                                        )]
            [:div.alert.alert-info  "Slave of " [:a {:href (apply str "/redis/" name )} name]])
          
          (for [[name details] (get-instances) :when (not-empty (filter #(= (-> details :ip) %1) slaves))]
            [:div.alert.alert-info "Has slave " [:a {:href (apply str "/redis/" name )} name]])
          
        
          [:h4 "INFO"]
          [:ul
           (for [[k v]  (sort instance-info)]
             [:li k ": " v])]
          ]])))


    
      
(defn redis-show-form [params & {:keys [problems]}]
  (let [defaults {:port 6379, :db 0} message (:flash params) ]
    (layout 
      [:ul.breadcrumb
       [:li [:a {:href "/"} "Home"] [:span.divider]]
       [:li [:a {:href "/redis"} "Instances"] [:span.divider]]
       [:li.active "New instance"]] 
      [:div.pull-left {:style "width: 55%"}
       (f/render-form (assoc redis-form
                             :values (merge defaults params)
                             :problems problems))]
      [:div.pull-right {:style "width: 43%"}
       [:p {:style "word-wrap: normal; white-space: pre; overflow: scroll; font-size: 12px; border: none; background: #f8f8f8; padding: 10px"}
        "Enter Redis server details"]])))

(defn redis-list [session]
  (let [defaults {:port 6379} message  session ]    
    (layout
      (breadcrumb)
      [:h1 "Redis instances"]
      (when message [:div.alert.alert-success message])
      [:table.table.table-bordered
       [:tr
        [:th "Name"][:th "Host"][:th "Port"][:th ]]
       (for [instance (sort-by :name (vals (get-instances)))]
         [:tr
          [:td [:a {:href (str "/redis/" (:name instance))} (:name instance)]]
          [:td (:ip instance)]
          [:td (:port instance)]
          [:td  
           (f/render-form (assoc instance-form :values {:operation "delete"}  :action (str "/redis/" (:name instance)))) 
           (f/render-form (assoc instance-form :submit-label "Edit" :values {:operation "edit"} :action (str "/redis/" (:name instance))))]]
         )
      ]
      [:a {:href "/redis/add" } [:button.btn.btn-primary "Add instance" ]]
      (when  (local-redis-available)
        (if (setup-in-local-redis)
          [:div.alert.alert-info "Settings are persisted in Redis instance " setup-host " under key " setup-key
           ". Save "  [:a { :onclick "document.do_not_persist_in_redis.submit();"} "settings in volatile memory"] " instead."]
          [:div.alert.alert-info "Settings are currently persisted in volatile memory. " 
           [:a { :onclick "document.persist_in_redis.submit();"} "Persist settings in redis instance"]  " " 
           setup-host " under key " setup-key " instead."
           ] 
          ))
      [:form {:name "persist_in_redis" :action "/redis" :method :post} [:input {:type "hidden" :name "operation" :value "enable_redis_persistance"}]]
      [:form {:name "do_not_persist_in_redis" :action "/redis" :method :post} [:input {:type "hidden" :name "operation" :value "disable_redis_persistance"}]]
      )))

(defn redis-operate[params]
  (let [values params]
     (cond
       (= "enable_redis_persistance" (-> values :operation)) (do 
                                             (enable-setup-in-local-redis)
                                             (assoc (redirect "/redis") :flash "Settings persisted in redis instance."))
       (= "disable_redis_persistance" (-> values :operation)) (do 
                                             (disable-setup-in-local-redis)
                                             (assoc (redirect "/redis") :flash "Settings deleted from redis instance."))
       :else (assoc (redirect (str "/redis")) :flash (str "Unknow operation.")))))
  

(defn redis-operate-on-key[name key params]
  (let [values (fp/parse-params key-form params)]
     (cond
       (= "delete" (-> values :operation)) (do 
                                             (winstance (get-instance-by-name name) (car/del key))
                                             (assoc (redirect (str "/redis/" name)) :flash (str "Key " key " deleted.")))
       :else (assoc (redirect (str "/redis/" name)) :flash (str "Unknow operation.")))))
  
(defn redis-operate-on-instance[name params]
  (let [values (fp/parse-params instance-form params)]
     (cond
       (= "delete" (-> values :operation)) (do 
                                             (delete-instance name)
                                             (assoc (redirect "/redis") :flash (str "Instance  " name " deleted!")))
       (= "edit" (-> values :operation)) (redis-show-form (get-instance-by-name name))
       :else (assoc (redirect (str "/redis")) :flash (str "Unknow operation on instance.")))))




(defn redis-submit [params]
  (fp/with-fallback (partial redis-show-form params :problems)
    (let [values (fp/parse-params redis-form params)]
      (update-instance (-> values :name) values)
      (assoc (redirect "/redis") :flash (str "Instance " (-> values :name) " updated.")))))

