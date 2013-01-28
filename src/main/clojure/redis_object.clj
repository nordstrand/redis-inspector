(ns redis-object
  (:require   
    [formative.core :as f]
    [formative.parse :as fp]
    [hiccup.page :as page]
    [web-tools :refer [layout]]
    [clojure.pprint :refer [pprint]]
    [taoensso.carmine :as car]
    [redis-tools :refer [winstance]]
    ))

(def paging-form
  { 
   :submit-label "OK"
   :method :get
   :fields [{:name :h1 :type :heading :text "Redis instance"}
            {:name :from :datatype :int}
            {:name :to :datatype :int}
            ]
   :validations [[:required [:from :to]]]
 ;  :renderer :bootstrap-stacked
   })



(declare redis-show-zset)

(defn redis-show-object [name key & params]
  (let [type (redis-tools/winstance (get @redis-tools/redises name) (car/type key))]
    (cond
    (= type "zset" ) (redis-show-zset name key (first params))
    true (str "Object " key " of type " type "not yet supported."))))

(defn page[from to]
  {:pre  [(< from to)] }
 (let [page-size (- to from)]
    (inc (/  from  page-size)))) 


(defn page-url [name key from to] (format "/redis/%s/%s?from=%s&to=%s" name key from to))

(defn paginate[from to size name key]
  (let [page-url (partial page-url name key)
        li (fn [from to] (when (and (>= from 0) (<= to size)) (> (page from to) 0)  [:li [:a {:href (page-url from to)} (page  from to)]])) 
        page-size (- to from)]
    [:div.pagination.pagination-large
     [:ul
      [:li [:a {:href (page-url  0  page-size) } "«"]]
      (li (- from page-size page-size) (- from page-size))
      (li (- from page-size) from)
      [:li.active [:a {:href (page-url from to)} (page from to)]]
      (li to (+ to page-size))
      (li (+ to page-size) (+ to page-size page-size))
      [:li [:a {:href (page-url  (- size page-size) size)} "»"]]
      ]]))

(defn redis-show-zset [name key & params]
  (println "params=" params)
  (let [defaults {:from 0, :to 3}
        values (merge defaults (first params))
        from (bigint (:from values)) 
        to (bigint (:to values)) 
        
        size (redis-tools/winstance (get @redis-tools/redises name) (car/zcard key))
        instance (get @redis-tools/redises name)]
    (layout
      [:ul.breadcrumb
       [:li [:a {:href "/"} "Home"] [:span.divider]]
       [:li [:a {:href "/redis"} "Instances"] [:span.divider]]
       [:li [:a {:href (str "/redis/" name)} name] [:span.divider]]
       [:li.active key]]
      [:div.pull-left {:style "width: 55%"}
       [:table.table.table-bordered
        [:tr
         [:th "Score"]
         [:th "Value"]]
        (for [[score value] (apply hash-map  (redis-tools/winstance instance (car/zrange key (:from values) (:to values) "withscores")))]
          [:tr
           [:td value]
           [:td score]]
          )
        ]
       (paginate from to size name key)
       ]
      [:div.pull-right {:style "width: 43%"}
           [:h4 key]
           
          [:ul
           [:li "Type: " (redis-tools/winstance instance (car/type key))]
           [:li "Size: " (redis-tools/winstance instance (car/zcard key))]
           [:li "From: " (:from values) " To: " (:to values)]
          ]]
      (f/render-form (assoc paging-form    
                            :action (str "/redis/" name "/" key)
                            :values values)
                             ))))
  



