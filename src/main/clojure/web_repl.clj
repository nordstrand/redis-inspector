(ns web-repl
  (:require clojure.main
            [taoensso.carmine :as car]
            )
  (:use [clojure.stacktrace :only [root-cause]]))

(defonce repl-sessions (ref {}))

;(defmacro redis[& body] `(redis-tools/winstance "a" ~@body))

(defn generate-ns  
     "generates ns for client connection"  
     [] (let [user-ns (create-ns (symbol (str "client-" (Math/abs (.nextInt (java.util.Random. ))))))]  
         (binding [*ns* user-ns]
               (clojure.core/refer-clojure)
               (refer 'taoensso.carmine)
               (use 'clojure.repl)
               )
         user-ns))

(defn current-bindings []
  (binding [*ns* (generate-ns)
            ;*ns*
            *warn-on-reflection* *warn-on-reflection*
            *math-context* *math-context*
            *print-meta* *print-meta*
            *print-length* *print-length*
            *print-level* *print-level*
            *compile-path* (System/getProperty "clojure.compile.path" "classes")
            *command-line-args* *command-line-args*
            *assert* *assert*
            *1 nil
            *2 nil
            *3 nil
            *e nil]
    (get-thread-bindings)))

(defn bindings-for [session-key]
  (when-not (@repl-sessions session-key)
    (dosync
      (commute repl-sessions assoc session-key (current-bindings))))
  (@repl-sessions session-key))

(defn store-bindings-for [session-key]
  (dosync
    (commute repl-sessions assoc session-key (current-bindings))))

(defmacro with-session [session-key & body]
  `(with-bindings (bindings-for ~session-key)
    (let [r# ~@body]
      (store-bindings-for ~session-key)
      r#)))

(defn- do-eval-can-throw-exception [txt session-key2]
  (with-session session-key2
    (let [form (binding [*read-eval* false] (read-string txt))]
      (with-open [writer (java.io.StringWriter.)]
        (binding [*out* writer]
          (let [r (pr-str 
                    (eval (concat (concat '(let) [['inst session-key2]])
                                  [(concat  '(clojure.tools.macro/macrolet [(redis[& body] `(redis-tools/winstance-name  ~'inst  ~@body))])
                                            [form])])))]
            (str (.toString writer) (str r))))))))
    

(defn do-eval [txt session-key]
    (try
      (do-eval-can-throw-exception txt session-key)
      (catch Exception e (str (root-cause e)))))
