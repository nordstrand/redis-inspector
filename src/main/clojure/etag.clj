(ns etag)


(defn- to-hex-string [bytes]
  (clojure.string/join "" (map #(Integer/toHexString (bit-and % 0xff))
                              bytes)))

(defn sha1 [obj]
   (let [bytes (.getBytes (with-out-str (pr obj)))] 
     (to-hex-string (.digest (java.security.MessageDigest/getInstance "SHA1") bytes))))

(defn sha1-of-is[^java.io.InputStream is]
  (let [baos (java.io.ByteArrayOutputStream. )] 
    (clojure.java.io/copy is baos)
    (println (sha1 (.toString baos)))
    {:sha1 (sha1 (.toString baos))
     :is (java.io.BufferedInputStream. (java.io.ByteArrayInputStream. (.toByteArray baos)))
     }))


(defn wrap-etag [handler] 
  (fn [request]
    (let [response (handler request)]    
      (if (re-find '#"text/javascript|text/css" (get-in response [:headers "Content-Type"]))
        (let [old-etag (get-in request [:headers "if-none-match"])
              {:keys [sha1 is]} (sha1-of-is (-> :body response))]
          (if (= old-etag sha1)
            {:status 304 :headers {} :body ""}
            (-> response
              (assoc-in [:headers "ETag"] sha1)
              (assoc :body is))))
        response))))
