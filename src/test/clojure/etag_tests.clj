(ns etag-test
  
  (:use expectations)
  (:require 
            
            [etag]
            )
  )


(expect "6143a88837f035afb02e458e74ae474f55a1b98" 
        (etag/sha1 "test123"))

(expect {:sha1 "6143a88837f035afb02e458e74ae474f55a1b98"} 
        (in 
          (etag/sha1-of-is (java.io.ByteArrayInputStream. (.getBytes "test123")))))

(expect {:sha1 "6143a88837f035afb02e458e74ae474f55a1b98"} 
        (in 
          (etag/sha1-of-is (:is (etag/sha1-of-is (java.io.ByteArrayInputStream. (.getBytes "test123")))))))

;should set etag header
(expect "6143a88837f035afb02e458e74ae474f55a1b98" 
        (->>  [:headers "ETag"]
          (get-in
            ((etag/wrap-etag identity) 
              {:status 200,
               :headers {"Content-Type" "text/javascript"},
               :body (java.io.ByteArrayInputStream. (.getBytes "test123"))}))))

;should return 304 on correct if-none-match
(expect 304
          (:status  
            ((etag/wrap-etag (fn [_] 
                               {:status 200,
                                :headers {"Content-Type" "text/javascript"},
                                :body (java.io.ByteArrayInputStream. (.getBytes "test123"))}))
              {:headers {"if-none-match" "6143a88837f035afb02e458e74ae474f55a1b98"}})))