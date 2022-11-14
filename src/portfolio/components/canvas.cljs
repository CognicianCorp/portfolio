(ns portfolio.components.canvas
  (:require [dumdom.core :as d]
            [portfolio.adapter :as adapter]
            [portfolio.components.code :refer [Code]]
            [portfolio.components.tab-bar :refer [TabBar]]
            [portfolio.components.triangle :refer [TriangleButton]]
            [portfolio.view :as view]
            [portfolio.views.canvas.protocols :as canvas]))

(defn get-iframe [canvas-el]
  (some-> canvas-el .-firstChild))

(defn get-iframe-document [canvas-el]
  (some-> canvas-el get-iframe .-contentWindow .-document))

(defn get-iframe-body [canvas-el]
  (some-> canvas-el get-iframe-document .-body))

(defn render-scene [el {:keys [scene tools opt]}]
  (doseq [tool tools]
    (canvas/prepare-canvas tool el opt))
  (let [canvas (some-> el .-firstChild .-contentDocument (.getElementById "canvas"))]
    (adapter/render-component (:component scene) canvas)))

(defn on-mounted [el f]
  (if (some-> el .-contentDocument (.getElementById "canvas"))
    (f)
    (.addEventListener
     el "load"
     (fn [_]
       (let [doc (->> el .-contentDocument)]
         (when-not (.getElementById doc "canvas")
           (let [el (doc.createElement "div")]
             (set! (.-id el) "canvas")
             (.appendChild (.-body doc) el)))
         (f))))))

(d/defcomponent Canvas
  :on-mount (fn [el data]
              (on-mounted (.-firstChild el)
                          (fn []
                            (doseq [path (:css-paths data)]
                              (let [link (js/document.createElement "link")]
                                (set! (.-rel link) "stylesheet")
                                (set! (.-type link) "text/css")
                                (set! (.-href link) path)
                                (.appendChild (.-head (get-iframe-document el)) link)))
                            (render-scene el data))))
  :on-update (fn [el data]
               (on-mounted (.-firstChild el) #(render-scene el data)))
  [data]
  [:div {:style {:background "#fff"
                 :transition "width 0.25s, height 0.25s"}}
   [:iframe
    {:src (or (:canvas-path data) "/portfolio/canvas.html")
     :style {:border "none"
             :padding 20
             :width "100%"
             :height "100%"}}]])

(d/defcomponent ComponentError [{:keys [component-args error] :as lol}]
  [:div {:style {:background "#fff"
                 :width "100%"
                 :height "100%"
                 :padding 20}}
   [:h1.h1.error "Failed to render component"]
   [:p.mod (:message error)]
   (when component-args
     [:div.vs-s.mod
      [:h2.h3.mod "Component arguments"]
      [:p.mod (Code {:code component-args})]])
   (when-let [data (:ex-data error)]
     [:div.vs-s.mod
      [:h2.h3.mod "ex-data"]
      [:p.mod (Code {:code data})]])
   [:p [:pre (:stack error)]]])

(d/defcomponent Toolbar [{:keys [tools]}]
  [:nav {:style {:background "#f8f8f8"
                 :border-bottom "1px solid #e5e5e5"}}
   (map canvas/render-toolbar-button tools)])

(d/defcomponent CanvasPanel [data]
  [:div {:style {:border-top "1px solid #ccc"
                 :background "#ffffff"
                 :height (if (:minimized? data) "40px" "30%")
                 :transition "height 0.25s"
                 :position "relative"}}
   (when-let [button (:button data)]
     [:div {:style {:position "absolute"
                    :right 20
                    :top 10}}
      (TriangleButton button)])
   (TabBar data)
   (some-> data :content view/render-view)])

(d/defcomponent CanvasHeader
  :keyfn :title
  [{:keys [title url description]}]
  [:div {:style {:margin 20}}
   [:h2.h3 {:style {:margin "0 0 10px"}}
    [:a {:href url} title]]
   (when-not (empty? description)
     [:p description])])

(defn render-canvas [data]
  (->> [(when (:title data)
          (CanvasHeader data))
        (when (:scene data)
          (if (:component (:scene data))
            (Canvas data)
            (ComponentError (:scene data))))
        (when (= :separator (:kind data))
          [:div {:key "separator"
                 :style {:height 20}}])]
       (remove nil?)))

(d/defcomponent CanvasView
  :keyfn :mode
  [data]
  [:div {:style {:background "#eee"
                 :flex-grow 1
                 :display "flex"
                 :flex-direction "column"
                 :overflow "hidden"}}
   (->> (for [row (:rows data)]
          [:div {:style {:display "flex"
                         :flex-direction "row"
                         :flex-grow 1
                         :justify-content "space-evenly"
                         :overflow "hidden"}}
           (->> (for [{:keys [toolbar canvases layout]} row]
                  (if layout
                    (CanvasView layout)
                    [:div {:style {:flex-grow 1
                                   :display "flex"
                                   :flex-direction "column"
                                   :overflow "hidden"}}
                     (some-> toolbar Toolbar)
                     [:div {:style {:overflow "scroll"
                                    :flex-grow "1"}}
                      (->> canvases
                           (interpose {:kind :separator})
                           (mapcat render-canvas))]]))
                (interpose [:div {:style {:border-left "5px solid #ddd"}}]))])
        (interpose [:div {:style {:border-top "5px solid #ddd"}}]))
   (some-> data :panel CanvasPanel)])
