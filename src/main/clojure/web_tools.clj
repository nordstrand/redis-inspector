(ns web-tools
  (:require
    [formative.core :as f]
    [formative.parse :as fp]
    [hiccup.page :as page]
    [hiccup.bootstrap.page :refer [include-bootstrap]]
    )
  )

(defn layout [& body]
  (page/html5
    [:head
     [:title "Redis Inspector"]
     (page/include-js "/js/jquery-1.9.1.min.js")
     (include-bootstrap)
     (page/include-css "/css/web-repl.css")
      [:style
      "body { margin: 2em; }"
      ".form-table { width: 100%; }"
      ".form-table th { text-align: left; }"
      ".form-table h3 { border-bottom: 1px solid #ddd; }"
      ".form-table .label-cell { vertical-align: top; text-align: right; padding-right: 10px; padding-top: 10px; }"
      ".form-table td { vertical-align: top; padding-top: 5px; }"
      ".form-table .checkbox-row label { display: inline; margin-left: 5px; }"
      ".form-table .checkbox-row .input-shell { margin-bottom: 10px; }"
      ".form-table .submit-row th, .form-table .submit-row td { padding: 30px 0; }"
      ".form-table .problem th, .form-table .problem td { color: #b94a48; background: #fee; }"
      ".table td  {vertical-align: middle;}" ]]
    [:body
     body]
    ))

(defn- build-paths[drilldown]
  (->>
    drilldown
    (map-indexed (fn [idx i]  [(take idx drilldown) i]))
    (map flatten)))
                             


(defn breadcrumb[ & drilldown]
  [:ul.breadcrumb
       [:li [:a {:href "/"} "Home"] [:span.divider]]
       [:li [:a {:href "/redis"} "Instances"] [:span.divider]]
       (for [p (drop-last (build-paths drilldown))]
         [:li [:a {:href (apply str "/redis/" (interpose "/" p)) } (last p)] [:span.divider]])
         [:li.active [:a {:href (apply str "/redis/" (interpose "/" (last (build-paths drilldown))))} (last (last (build-paths drilldown)))]] 
   ])                                                                                                   
   

(defn- page[from to]
  {:pre  [(< from to)] }
 (let [page-size (- to from)]
    (inc (/  from  page-size)))) 
;(ring.util.codec/form-encode {:foo 1 :bar 2 :baz 3})
(defn- page-url 
  ([base-url from to] 
    (format "%s?from=%s&to=%s" base-url from to))
  ([base-url url-params from to] 
    (format "%s&%s" (page-url base-url from to) url-params)))

(defn start[from to size] 
  (let [page-size (- to from)
        total-pages (/ size page-size)
        p (- (page from to) 2)]
    (cond
      (< p 1) 1
      (> (+ 5 p) total-pages) (- total-pages 4)
      :else p)))

(defn paginate[base-url from to size & url-params]
  (let [page-url (if (empty? url-params)
                   (partial page-url base-url)
                   (partial page-url base-url (first url-params)))
        li (partial (fn [f from to] 
             (when (and (>= from 0) (<= to size)) (> (page from to) 0)  
               [(if (= f from) :li.active :li) [:a {:href (page-url from to)} (page  from to)]])) from)
        page-size (- to from)]
    [:div.pagination.pagination-large
     [:ul
      [:li [:a {:href (page-url  0  page-size) } "«"]]
      (for [p (range (start from to size) (+ 5 (start from to size)))]
        (li (* (dec p) page-size) (* p page-size)))
      [:li [:a {:href (page-url  (- size page-size) size)} "»"]]
      ]]))

