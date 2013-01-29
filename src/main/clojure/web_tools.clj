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