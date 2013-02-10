(ns web-repl
  (:require clojure.main)
  (:use [clojure.stacktrace :only [root-cause]]))

(defonce repl-sessions (ref {}))

(defn current-bindings []
  (binding [*ns* *ns*
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

(defn do-eval [txt session-key]
  (println ">>>>>>>>>> " txt)
  (with-session session-key
    (let [form (binding [*read-eval* false] (read-string txt))]
      (with-open [writer (java.io.StringWriter.)]
        (binding [*out* writer]
          (try
            (let [r (pr-str (eval form))]
              (str (.toString writer) (str r)))
            (catch Exception e (str (root-cause e)))))))))