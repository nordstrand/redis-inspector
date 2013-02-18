(ns web-repl-tests
  (:use expectations)
  (:require 
    [web-repl]
  ))

(disable-run-on-shutdown)


;(expect "2" (web-repl/do-eval "(+ 1 1)" "test"))


;expect carmine functions to be refered

; strange that it doesnt work (expect "true" (web-repl/do-eval "(ifn? ping)" "a"))