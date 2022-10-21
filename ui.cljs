(ns ui
  (:require
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [promesa.core :as p]))

(defprotocol ITick
  (tick [this]))

(defrecord World [elevators floor-count]
  ITick
  (tick [world]
    (-> world
        (update :elevators (fn [elevators]
                             (mapv tick elevators)))
        (update :duration + 50))))

(defrecord Elevator [current-floor speed travel-progress destination-floor
                     percent-change x-offset width pressed-buttons]

  ITick
  (tick [{:keys [travel-progress current-floor destination-floor]
          :as elevator}]
    (if (not= current-floor destination-floor)
      (as-> elevator elevator
        (update elevator :travel-progress #(+ 10 %))
        (if (<= 100 (:travel-progress elevator))
          (-> elevator
              (assoc :travel-progress 0)
              (update :current-floor (if (< current-floor destination-floor) inc dec)))

          elevator))
      elevator)))

(defn create-new-elevator []
  (map->Elevator
   {:current-floor     0
    :speed             10 ;; ticks required to travel between floors
    :travel-progress   0
    :destination-floor 0
    :percent-change    0
    :x-offset          200
    :width             40 ;; proxy for size
    :pressed-buttons   #{}}))

(defn description [users seconds]
  [:span
   "Transport " [:span.emphasis-color users]
   " people in " [:span.emphasis-color (.toFixed seconds 0)]
   " seconds or fewer"])

(defn condition [& {:keys [users seconds]}]
  {:description [description users seconds] })

(def default-options   {:floor-height   50
                        :floor-count    4
                        :elevator-count 2
                        :spawn-rate     0.5})

(def challenges [{:options {:floor-count 3
                            :elevator-count 1
                            :spawn-rate 0.3}
                  :condition (condition :users 15
                                        :seconds 60)}])

(def world (r/atom (map->World
                    {:time-running 0
                     :floor-height   50
                     :elevator-count 1
                     :spawn-rate     0.5
                     :floor-count    3
                     :elevators      [(create-new-elevator)]})))

(def started? (r/atom false))
(defn start-ticking []
  (reset! started? true)
  (p/loop []
    (swap! world tick)
    (p/delay 50)
    (when @started? (p/recur))))
(defn pause-ticking []
  (reset! started? false))

(comment
  @started?
  (start-ticking)
  (stop-ticking)
  )

(def floor-height 50)

(defn travel-progress->y-pos [{:keys [travel-progress current-floor]}]
  (- (* (dec (:floor-count @world)) floor-height)
     (+ (* current-floor floor-height)
        (/ travel-progress 2))))

(comment
  (dotimes [n 10]
   (swap! world tick))
         nil)

(defn challenge-header [num challenge]
  [:div
   [:div.left
    [:h3 (str "Challenge #" num ": ") (-> challenge :condition :description)]]
   [:button.right.startstop {:style {:width "110px"}
                                          :onClick (if @started?
                                                     pause-ticking
                                                     start-ticking)}
    (if @started?
      "Pause"
      "Start")]
   [:h3.right
    [:i.fa.fa-minus-square.timescale_decrease.unselectable]
    [:span.emphasis-color {:style {:display :inline-block
                                   :width "22px"
                                   :text-align "center"}}
     "2x"]
    [:i.fa.fa-plus-square.timescale_increase.unselectable]]])

(defn up-indicator [active?]
  [:span.directionindicator.directionindicatorup [:i.fa.fa-arrow-circle-up.up {:className (when active? "activated")}]])

(defn down-indicator [active?]
  [:span.directionindicator.directionindicatordown [:i.fa.fa-arrow-circle-down.down {:className (when active? "activated")}]])

(defn elevator-floor-button [floor pressed?]
  [:span.buttonpress {:className (when pressed? "activated")}
      floor])

(defn elevator [{:keys [pressed-buttons width x-offset current-floor travel-progress]
                 :as elevator}]
  [:div.elevator.movable {:style {:width (str width "px")
                                  :transform (str "translate(" x-offset "px, " (travel-progress->y-pos elevator) "px)")}}
   [up-indicator (:up pressed-buttons)]
   [:span.floorindicator [:span current-floor]]
   [down-indicator (:down pressed-buttons)]
   [:span.buttonindicator
    (for [n (range 3)]
      [elevator-floor-button n (contains? pressed-buttons n)])]])

(defn elevators [challenge]
  [elevator (-> world deref :elevators first)])

(defn floor [{:keys [num offset top?]}]
  [:div.floor {:style {:top (str offset "px")}}
   [:span.floornumber num]
   [:span.buttonindicator
    [:i.fa.fa-arrow-circle-up.up {:style {:visibility (when top? "hidden")
                                          :margin-right "0.25rem"}}]
    (when-not (zero? num) [:i.fa.fa-arrow-circle-down.down])]])

(defn floors [challenge]
  (let [total-floors (-> challenge :options :floor-count)]
   [:<>
    (for [n (reverse (range total-floors))
          :let [height (* (- (dec total-floors) n) (:floor-height default-options))]]
      [floor {:num n
              :offset height
              :top? (= total-floors (inc n))}])]))

(defn stats-container []
  [:div.statscontainer
   [:div {:style {:top "20px"}}
    [:span.key  "Transported"]
    [:span.value.transportedcounter 0]]

   [:div {:style {:top "40px"}}
    [:span.key "Elapsed"] [:span.value.elapsedtime (int (/ (:duration @world)
                                                           1000)) "s"]]

   [:div {:style {:top "60px"}}
    [:span.key "Transported/s"] [:span.value.transportedpersec 0]]

   [:div {:style {:top "80px"}}
    [:span.key "Avg waiting time"] [:span.value.avgwaittime 0]]

   [:div {:style {:top "100px"}}
    [:span.key "Max waiting time"] [:span.value.maxwaittime 0]]

   [:div {:style {:top "120px"}}
    [:span.key {:title "Number of floors that have been travelled by elevators"}
     "Moves"] [:span.value.movecount 0]]])

(defn my-component []
  (let [challenge (first challenges)]
    [:div {:style {:width "100%"}}
     [:hr]

     [challenge-header 1 challenge]
     [:div.world
      [:div.feedbackcontainer]
      [:div.innerworld {:style {:height (* (-> challenge :options :floor-count)
                                           (:floor-height default-options))}}
       [floors challenge]
       [elevators challenge]]
      [stats-container]]

     [:div.codestatus]

     [:div.code
      [:textarea {:name "Code"
                  :id "code"}]]
     [:div {:style {:display :flex
                    :justify-content "space-between"
                    :width "100%"}}
      [:div
       [:button#button_reset "Reset"]
       [:button#button_resetundo "Undo reset"]]
      [:div {:style {:display :flex
                     :justify-content :end}}
       [:span#save_message]
       [:span#fitness_message]
       [:button#button_apply "Apply"]
       [:button#button_save "Save"]
       ]]

     [:br]]))

(defn create-editor []
  (js/createEditor))


(rdom/render [my-component] (.getElementById js/document "reagent"))
(create-editor)

(comment

  @interval
  (start-interval)
  (stop-interval))
