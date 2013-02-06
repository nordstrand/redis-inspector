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
               [:form {:action (str "/redis/" (:name instance) "/" key) :method :post :style "margin-bottom: 0px;"} 
                [:div.btn-group
                 [:input.btn {:type "submit" :name "operation" :value "Delete"}]]]]]
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
      [:div.row
       [:div.span9
       (f/render-form (assoc redis-form
                             :values (merge defaults params)
                             :problems problems))]
      [:div.span3
       
       [:h3 "Enter Redis instance details." ]
        [:p "The default port is " (-> :port defaults) 
         " and the default database is " (-> :db defaults) "."]
        [:p "The name of instance can be freely choosen but needs to be unique."]]])))

(defn redis-list [session]
  (let [defaults {:port 6379} message  session ]    
    (layout
      [:div.row-fluid 
       [:div.span12
        (breadcrumb)]]
      [:div.row
       [:div.span12
        (when message [:div.alert.alert-success message])]]
      [:div.row 
       [:div.span10
        [:table.table.table-bordered
         [:tr [:th.span4 "Name"][:th.span2 "Host"][:th.span1 "Port"][:th.span3 ]]
         (for [instance (sort-by :name (vals (get-instances)))]
           [:tr 
            [:td [:a {:href (str "/redis/" (:name instance))} (:name instance)]]
            [:td (:ip instance)]
            [:td (:port instance)]
            [:td 
             [:form {:action (str "/redis/" (:name instance)) :method :post :style "margin-bottom: 0px;"} 
               [:div.btn-group
                (for [op ["Browse" "Ping" "Edit" "Delete"]]   [:input.btn {:type "submit" :name "operation" :value op}])]]]]
           )
         ]
        [:a {:href "/redis/add" } [:button.btn.btn-primary "Add instance" ]]
        ]
       [:div.span2
        (when  true
        (if (setup-in-local-redis)
          [:div.alert.alert-info "Settings are persisted in Redis instance " setup-host " under key " setup-key
           ". Save "  [:a { :onclick "document.do_not_persist_in_redis.submit();"} "settings in volatile memory"] " instead."]
          [:div.alert.alert-info "Settings are currently persisted in volatile memory. " 
           [:a { :onclick "document.persist_in_redis.submit();"} "Persist settings in redis instance"]  " " 
           setup-host " under key " setup-key " instead."
           ] 
          ))      
        ]
       ]
      [:div.row
       [:div.span12   
        [:form {:name "persist_in_redis" :action "/redis" :method :post} [:input {:type "hidden" :name "operation" :value "enable_redis_persistance"}]]
        [:form {:name "do_not_persist_in_redis" :action "/redis" :method :post} [:input {:type "hidden" :name "operation" :value "disable_redis_persistance"}]]
        ]]
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
     (cond
       (= "delete" (clojure.string/lower-case (-> params :operation))) (do 
                                             (winstance (get-instance-by-name name) (car/del key))
                                             (assoc (redirect (str "/redis/" name)) :flash (str "Key " key " deleted.")))
       :else (assoc (redirect (str "/redis/" name)) :flash (str "Unknow operation."))))
 

(defmulti redis-operate-on-instance (fn [name params] (-> params :operation clojure.string/lower-case keyword)))
(defmethod redis-operate-on-instance :ping [name _] 
  (assoc (redirect "/redis")  
         :flash (str name  ": " (winstance (get-instance-by-name name) (car/ping)))))
(defmethod redis-operate-on-instance :edit [name _] (redis-show-form (get-instance-by-name name)))
(defmethod redis-operate-on-instance :browse [name _] (redirect (str "/redis/" name)))
(defmethod redis-operate-on-instance :delete [name _] 
  (delete-instance name)
  (assoc (redirect "/redis") :flash (str "Instance  " name " deleted.")))


(defn redis-submit [params]
  (fp/with-fallback (partial redis-show-form params :problems)
    (let [values (fp/parse-params redis-form params)]
      (update-instance (-> values :name) values)
      (assoc (redirect "/redis") :flash (str "Instance " (-> values :name) " updated.")))))

