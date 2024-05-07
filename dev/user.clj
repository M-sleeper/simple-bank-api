(ns user
  (:require
   [integrant.repl :as ig-repl]
   [integrant.repl.state :as state]
   [simple-bank.system :as system]))

(ig-repl/set-prep! system/load-config)

(def go ig-repl/go)
(def halt ig-repl/halt)
(def reset ig-repl/reset)
(def reset-all ig-repl/reset-all)


(comment
  state/config
  (go)
  (halt)
  (reset)
  (reset-all))
