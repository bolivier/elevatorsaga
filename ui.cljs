(ns ui
  (:require
   [reagent.core :as r]
   [reagent.dom :as rdom]))

(def default-options   {:floor-height   50
                        :floor-count    4
                        :elevator-count 2
                        :spawn-rate     0.5})

(defn description [users seconds]
  [:span
   "Transport " [:span.emphasis-color users]
   " people in " [:span.emphasis-color (.toFixed seconds 0)]
   " seconds or fewer"])

(defn condition [& {:keys [users seconds]}]
  {:description [description users seconds] })

(def challenges
  [
   {:options {:floor-count 3
              :elevator-count 1
              :spawn-rate 0.3}
    :condition (condition :users 15
                          :seconds 60)}])

(defn challenge-header [num challenge]
  [:div
   [:div.left
    [:h3 (str "Challenge #" num ": ") (-> challenge :condition :description)]]
   [:button.right.startstop.unselectable {:style {:width "110px"}}
    "Start" ;; TODO make dynamic
    ]
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

(defn elevator []
  (r/with-let [pressed-buttons #{1 :up}]
   [:div.elevator.movable {:style {:width "50px"}}
    [up-indicator (:up pressed-buttons)]
    [:span.floorindicator [:span 1]]
    [down-indicator (:down pressed-buttons)]
    [:span.buttonindicator
     (for [n (range 3)]
       [elevator-floor-button n (contains? pressed-buttons n)])]]))

(defn elevators [challenge]
  [elevator])

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

(defn my-component []
  (let [challenge (first challenges)]
    [:div
     [:hr]

     [challenge-header 1 challenge]
     [:div.world
      [:div.feedbackcontainer]
      [:div.innerworld {:style {:height (* (-> challenge :options :floor-count)
                                           (:floor-height default-options))}}
       [floors challenge]
       [elevators challenge]]]

     [:br]]))

(rdom/render [my-component] (.getElementById js/document "reagent"))

