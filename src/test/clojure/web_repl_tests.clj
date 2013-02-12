(ns web-repl-tests
  (:use expectations)
  (:require 
    [web-repl]
  ))

(disable-run-on-shutdown)


(expect "2" (web-repl/do-eval "(+ 1 1)" :id))