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
     (include-bootstrap)
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
      ".form-table .problem th, .form-table .problem td { color: #b94a48; background: #fee; }"]]
    [:body
     body]
    ))


(defn- page[from to]
  {:pre  [(< from to)] }
 (let [page-size (- to from)]
    (inc (/  from  page-size)))) 

(defn- page-url 
  ([base-url from to] 
    (format "%s?from=%s&to=%s" base-url from to))
  ([base-url url-params from to] 
    (format "%s&%s" (page-url base-url from to) url-params)))

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
      (li (- from page-size page-size) (- from page-size))
      (li (- from page-size) from)
      (li from to)
      (li to (+ to page-size))
      (li (+ to page-size) (+ to page-size page-size))
      [:li [:a {:href (page-url  (- size page-size) size)} "»"]]
      ]]))


(comment
  
(def renderer-form
  {:method "get"
   :renderer :inline
   :submit-label nil
   :fields [{:name :renderer
             :type :select
             :options ["bootstrap-horizontal"
                       "bootstrap-stacked"
                       "table"]
             :onchange "this.form.submit()"}]}))