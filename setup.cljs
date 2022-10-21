(ns setup
  (:require [clojure.string :as str]))

(defmulti elevator-event :event-type)

(defmethod elevator-event :idle [{:keys [elevator]}]
  (println elevator))

(.addEventListener
 js/window
 "elevator-event"
 (fn [custom-event]

   (let [{:keys [event arg1 elevator]} (js->clj (.-detail custom-event) :keywordize-keys true)]
     (set! (.-ev js/window) elevator)
     (prn `(elevator-event
       ~{:event-type (keyword (str/replace event #"_" "-"))
        :elevator (js->clj elevator)})))))
