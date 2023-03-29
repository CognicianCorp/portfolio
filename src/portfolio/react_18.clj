(ns portfolio.react-18
  (:require [portfolio.scene :as scene]))

(defmacro defscene [id & opts]
  (when (scene/portfolio-active?)
    `(portfolio.data/register-scene!
      (portfolio.react-18/create-scene
       ~(scene/get-options-map id (:line &env) opts)))))

(defmacro defns [title & opts]
  `(portfolio.data/register-namespace!
    ~(scene/get-namespace-options title opts)))
