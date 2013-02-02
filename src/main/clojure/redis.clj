(ns redis
  (:require
            [ring.util.response :refer [redirect]]
            [formative.core :as f]
            [formative.parse :as fp]
            [hiccup.page :as page]
            [web-tools :refer [layout breadcrumb]]
            [clojure.pprint :refer [pprint]]
            [taoensso.carmine :as car]
            [redis-tools :refer [winstance get-instance-by-name]]            
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

(def delete-form
  {:method "post"
   :renderer :inline
   :submit-label "Delete"
   })
(def edit-form
  {:method "get"
   :renderer :inline
   :submit-label "Edit"
   })

(defn redis-show-instance [name]
    (layout
      (breadcrumb name)
      (let [instance  (get-instance-by-name name)]
        [:div 
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
              (f/render-form (assoc delete-form :action (str "/redis/" (:name instance) "/" key "/delete"))) 
              ]]
             )
           ]]        
         [:div.pull-right {:style "width: 43%"}
           [:h4 "Instance"]
          [:ul
           (for [[k v] (get-instance-by-name name)]
             [:li k ": " v])]
          [:h4 "INFO"]
          [:ul
           (for [[k v] (sort (redis-tools/winstance instance (car/info*)))]
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
       (for [instance (sort-by :name (vals @redis-tools/redises))]
         [:tr
          [:td [:a {:href (str "/redis/" (:name instance))} (:name instance)]]
          [:td (:ip instance)]
          [:td (:port instance)]
          [:td  
           (f/render-form (assoc delete-form :action (str "/redis/" (:name instance) "/delete"))) 
           (f/render-form (assoc edit-form :action (str "/redis/" (:name instance) "/edit")))]]
         )
      ]
      [:a {:href "/redis/add" } [:button.btn.btn-primary "Add instance" ]]
      )))

(defn redis-show-edit-form [name]
  (redis-show-form (get @redis-tools/redises name)))

(defn redis-delete-instance[name]
   (reset! redis-tools/redises (dissoc @redis-tools/redises name))
    (assoc (redirect "/redis") :flash (str "Instance  " name " deleted.")))


(defn redis-submit [params]
  (fp/with-fallback (partial redis-show-form params :problems)
    (let [values (fp/parse-params redis-form params)]
      (reset! redis-tools/redises (assoc @redis-tools/redises (:name values) values))
      (assoc (redirect "/redis") :flash (str "Instance " (-> values :name) " updated.")))))

