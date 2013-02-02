(ns redis-object
  (:require   
    [formative.core :as f]
    [formative.parse :as fp]
    [hiccup.page :as page]
    [web-tools :refer [layout paginate breadcrumb]]
    [clojure.pprint :refer [pprint]]
    [taoensso.carmine :as car]
    [redis-tools :refer [winstance get-instance-by-name]]
    ))

(def zset-form
  { 
   :submit-label "OK"
   :method :get
   :fields [{:name :h4 :type :heading :text "Show elements"}
            {:name :from :datatype :int}
            {:name :to :datatype :int}
            {:name :reverse :label "Reverse order" :type :checkbox }
            ]
   :validations [[:required [:from :to]]
                 [:int [:from :to]]
                ; [:boolean [:reverse]]
                 ]
 ;  :renderer :bootstrap-stacked
   })

(def list-form
  { 
   :submit-label "OK"
   :method :get
   :fields [{:name :h4 :type :heading :text "Show elements"}
            {:name :from :datatype :int}
            {:name :to :datatype :int}
            ]
   :validations [[:required [:from :to]]
                 [:int [:from :to]]
                ; [:boolean [:reverse]]
                 ]
   })
(def hash-form
  { 
   :submit-label "OK"
   :method :get
   :fields [{:name :h4 :type :heading :text "Lookup value"}
            {:name :hkey :label "Hash field" }
            ]
   :validations [[:required [:field]]
                 ]
     })

(def string-form
  { 
   :submit-label "OK"
   :method :get
   :fields [{:name :h4 :type :heading :text "Show string"}
            {:name :show :type :submit  :value "value" }
            ]
   :validations [[:required [:field]]
                 ]
     })



(declare redis-show-zset redis-show-hash redis-show-string redis-show-list redis-show-set)

(defn redis-show-object [name key & params]
  (let [type (redis-tools/winstance (get-instance-by-name name) (car/type key))]
    (cond
    (= type "zset" ) (redis-show-zset name key (first params))
    (= type "hash" ) (redis-show-hash name key (first params))
    (= type "string" ) (redis-show-string name key (first params))
    (= type "list" ) (redis-show-list name key (first params))
    (= type "set" ) (redis-show-set name key (first params))
    true (str "Object " key " of type " type " not yet supported."))))


(defn redis-show-hash [name key & params]
  (let [defaults {}
        values (merge defaults (first params))
        base-url (format "/redis/%s/%s" name key)
        instance (get-instance-by-name name)
        size (redis-tools/winstance instance (car/hlen key))
        hkey (-> values :hkey)]
    (layout
      (breadcrumb name key hkey)     
      [:div.pull-left {:style "width: 55%"}
       [:table.table.table-bordered
        [:tr
         [:th "Key"]
         [:th "Value"]] 
        [:tr
         [:td (:hkey values)]
         [:td (redis-tools/winstance instance (when hkey (car/hget key hkey)))]]
        ]

       ]
      [:div.pull-right {:style "width: 43%"}
           [:h4 key]          
          [:ul
           [:li "Type: " (redis-tools/winstance instance (car/type key))]
           [:li "Size: " size]
          ]]
      (f/render-form (assoc hash-form    
                            :action base-url
                            :values values)
                             ))))


(defn redis-show-set [name key & params]
  (let [defaults {}
        values (merge defaults (first params))
        base-url (format "/redis/%s/%s" name key)
        instance (get-instance-by-name name)
        size (redis-tools/winstance instance (car/scard key))
        hkey (-> values :hkey)]
    (layout
      (breadcrumb name key hkey)     
      [:div.pull-left {:style "width: 55%"}
       ]
      [:div.pull-right {:style "width: 43%"}
           [:h4 key]          
          [:ul
           [:li "Type: " (redis-tools/winstance instance (car/type key))]
           [:li "Size: " size]
          ]]
      )))

(defn redis-show-hash [name key & params]
  (let [defaults {}
        values (merge defaults (first params))
        size (redis-tools/winstance (get-instance-by-name name) (car/hlen key))
        base-url (format "/redis/%s/%s" name key)
        instance (get-instance-by-name name)
        hkey (-> values :hkey)]
    (layout
      (breadcrumb name key hkey)     
      [:div.pull-left {:style "width: 55%"}
       [:table.table.table-bordered
        [:tr
         [:th "Key"]
         [:th "Value"]] 
        [:tr
         [:td (:hkey values)]
         [:td (redis-tools/winstance instance (when hkey (car/hget key hkey)))]]
        ]

       ]
      [:div.pull-right {:style "width: 43%"}
           [:h4 key]          
          [:ul
           [:li "Type: " (redis-tools/winstance instance (car/type key))]
           [:li "Size: " size]
          ]]
      (f/render-form (assoc hash-form    
                            :action base-url
                            :values values)
                             ))))

(defn redis-show-zset [name key & params]
  (let [defaults {:from 0, :to 10, :reverse false}
        values (merge defaults (first params))
        from (bigint (:from values)) 
        to (bigint (:to values)) 
        base-url (format "/redis/%s/%s" name key)
        instance (get-instance-by-name name)
        size (redis-tools/winstance instance (car/zcard key))
        ]
    (layout
      (breadcrumb name key)
      [:div.pull-left {:style "width: 55%"}
       (paginate base-url from to size) 
       [:table.table.table-bordered
        [:tr
         [:th "Score"]
         [:th "Value"]]
        (for [[score value] (partition 2 (redis-tools/winstance instance (if (-> values :reverse Boolean/valueOf) ;TODO investigate why required
                                                                               (car/zrevrange key (:from values) (:to values) "withscores")
                                                                               (car/zrange key (:from values) (:to values) "withscores"))))]
          [:tr
           [:td value]
           [:td score]]
          )
        ]
       (paginate base-url from to size (str "reverse=" (-> values :reverse))) 
       ]
      [:div.pull-right {:style "width: 43%"}
           [:h4 key]
           
          [:ul
           [:li "Type: " (redis-tools/winstance instance (car/type key))]
           [:li "Size: " (redis-tools/winstance instance (car/zcard key))]
           [:li "From: " (:from values) " To: " (:to values)]
          ]]
      (f/render-form (assoc zset-form    
                            :action base-url
                            :values values)
                             ))))
  


(defn redis-show-list [name key & params]
  (let [
        defaults {:from 0, :to 10}
        values (merge defaults (first params))
        from (bigint (:from values)) 
        to (bigint (:to values)) 
        base-url (format "/redis/%s/%s" name key)
        instance (get-instance-by-name name)
        size (redis-tools/winstance instance (car/llen key))
        ]
    (layout
      (breadcrumb name key)
      [:div.pull-left {:style "width: 55%"}
       (paginate base-url from to size) 
       [:table.table.table-bordered
        [:tr
         [:th "Value"]]
        (for [value  (redis-tools/winstance instance (car/lrange key (:from values) (:to values)))]
          [:tr
           [:td value]]
          )
        ]
       (paginate base-url from to size) 
       ]
      [:div.pull-right {:style "width: 43%"}
           [:h4 key]
           
          [:ul
           [:li "Type: " (redis-tools/winstance instance (car/type key))]
           [:li "Size: " size]
           [:li "From: " (:from values) " To: " (:to values)]
          ]]
      (f/render-form (assoc list-form    
                            :action base-url
                            :values values)
                             ))))
  



