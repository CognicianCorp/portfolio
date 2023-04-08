(ns portfolio.ui.canvas.viewport
  (:require [portfolio.ui.canvas.addons :as addons]
            [portfolio.ui.components.canvas :as canvas]))

(defn get-width [frame frame-body width & [{:zoom/keys [level]}]]
  (let [level (or level 1)]
    (cond
      (= :auto width)
      (let [style (js/window.getComputedStyle frame)]
        (str (* (+ (js/parseInt (.getPropertyValue style "padding-left") 10)
                   (js/parseInt (.getPropertyValue style "padding-right") 10)
                   (.-scrollWidth frame-body))
                level) "px"))

      (number? width) (str (* width level) "px")

      (nil? width) "100%"

      :else width)))

(defn get-style-px [style prop]
  (if-let [v (some-> style (.getPropertyValue prop) not-empty)]
    (js/parseInt v 10)
    0))

(defn get-height [frame frame-body height & [{:zoom/keys [level]}]]
  (let [level (or level 1)]
    (cond
      (= :auto height)
      (let [style (js/window.getComputedStyle frame)
            root-el-style (some-> frame-body
                                  .-firstElementChild
                                  .-firstElementChild
                                  js/window.getComputedStyle)]
        (str (* (+ (get-style-px style "padding-top")
                   (get-style-px style "padding-bottom")
                   (get-style-px root-el-style "margin-top")
                   (get-style-px root-el-style "margin-bottom")
                   (.-scrollHeight frame-body))
                level) "px"))

      (number? height) (str (* height level) "px")

      (nil? height) "100%"

      :else height)))

(defn get-available-width [el]
  (-> (.-parentNode el)
      .getBoundingClientRect
      .-width))

(defn prepare-canvas [_ el {:viewport/keys [width height] :as opt}]
  (let [frame (canvas/get-iframe el)
        frame-body (canvas/get-iframe-body el)
        w (get-width frame frame-body width opt)]
    (set! (.. el -style -width)
          (cond
            (and (= "100%" w) (not= "100%" (or height "100%")))
            (str "calc(100% - 40px)")

            (when (number? w)
              (<= (get-available-width el) w))
            "100%"

            :default w))))

(defn finalize-canvas [_ el {:viewport/keys [width height] :as opt}]
  (let [frame (canvas/get-iframe el)
        frame-body (canvas/get-iframe-body el)
        w (get-width frame frame-body width opt)
        h (get-height frame frame-body height opt)
        [margin shadow] (if (and (or (not= "100%" w) (not= "100%" h))
                                 (or (not (number? width))
                                     (< (+ 40 width) (get-available-width el))))
                          ["20px" "rgba(0, 0, 0, 0.1) 0px 1px 5px 0px"]
                          ["0" "none"])]
    (set! (.. el -style -height) h)
    (set! (.. el -style -margin) (str "0 auto " margin))
    (set! (.. el -style -boxShadow) shadow)))

(defn get-padding [xs]
  (cond
    (empty? xs)
    [20 20 20 20]

    (number? xs)
    [xs xs xs xs]

    (= 1 (count xs))
    (let [x (first xs)]
      [x x x x])

    (= 2 (count xs))
    (let [[v h] xs]
      [v h v h])

    :default xs))

(defn create-viewport-tool [config]
  (addons/create-toolbar-menu-button
   {:id :canvas/viewport
    :title "Viewport"
    :icon :portfolio.ui.icons/browsers
    :default-value (update (:viewport/defaults config) :viewport/padding get-padding)
    :options (->> (or (:viewport/options config)
                      [{:title "Auto"
                        :value {:viewport/width "100%"
                                :viewport/height "100%"}
                        :type :desktop}
                       {:title "iPhone 12 / 13 Pro"
                        :value {:viewport/width 390
                                :viewport/height 844}
                        :type :mobile}])
                  (map #(update % :viewport/padding get-padding)))
    :prepare-canvas #'prepare-canvas
    :finalize-canvas #'finalize-canvas}))

(defn create-viewport-extension [config]
  (addons/create-canvas-extension
   {:id :canvas/viewport
    :title "Viewport"
    :default-value (:viewport/defaults config)
    :prepare-canvas #'prepare-canvas
    :finalize-canvas #'finalize-canvas}))
