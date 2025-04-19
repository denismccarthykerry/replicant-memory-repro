(ns core
  (:require
    [clojure.walk :as walk]
    [taoensso.telemere :as t]
    [replicant.dom :as r]
    ["intl-tel-input" :as intl-tel-input :refer [intlTelInput]]
    [datascript.core :as ds]
    ["intl-tel-input/build/js/utils.js" :as intl-tel-input-utils]))

(defonce conn (ds/create-conn))

(defn load-utils-fn []
  ;; This must return a promise resolving to the utils module
  (js/Promise.resolve intl-tel-input-utils))

(defonce attach-utils-once
         (delay
           (-> (. intl-tel-input attachUtils load-utils-fn))))


(defn mount-intl-tel-input [{:replicant/keys [node remember]}]
  ;; Ensure utils are attached when the first phone input is mounted
  @attach-utils-once

  ;; Initialize the phone input with options
  (let [iti  (intl-tel-input node
                             #js {:countryOrder     #js ["ie" "gb"]
                                  :initialCountry   "ie"
                                  :separateDialCode true
                                  :autoPlaceholder  "aggressive"})]
    (prn "iti= " iti)
    ;; Return the instance for potential use outside
    (remember iti)))

(defn render-intl-tel-input [db]
  (let [field (ds/entity db [:db/ident :mobile-number])
        field-value (:field/value field)]
    [:div
     [:div.container.mx-auto.p-4
      [:h1.text-2xl.font-bold.mb-4 "Mobile Number Input"]
      [:div.mb-4
       [:label.block.text-gray-700.text-sm.font-bold.mb-2 {:for "phone"}
        "Phone Number"]
       [:input#phone.shadow.appearance-none.border.rounded.w-full.py-2.px-3.text-gray-700.leading-tight.focus:outline-none.focus:shadow-outline
        {:replicant/on-mount  mount-intl-tel-input
         :replicant/on-update (fn [{:replicant/keys [memory] :as params}]
                                (prn "Updating node, params = " params))
         :type                "tel"
         ;(ds/entity @conn [:db/ident :current-influencer])
         :on                  {:input [[:db/transact [{:db/ident    :mobile-number
                                                       :field/value :event/target.value}]]]}
         :value               field-value}]]]]))

(defn interpolate-actions
  "Replaces any event-related placeholders in the action with actual values from the event.
   This allows actions to reference event properties like target value."
  [event action]
  (try
    (let [result (walk/postwalk
                   (fn [x]
                     (case x
                       :event/target.value
                       (.. event -target -value)

                       x))
                   action)]
      (t/log! :info ["Interpolation result:" result])
      result)
    (catch js/Error e
      (t/log! :error ["Error in interpolate-action:" (.-message e)])
      action)))

(defn convert-command-to-operation
  "Convert command to operation"
  [event-data conn command]
  (t/log! :info ["Converting command to operation, command =" command])

  (try
    (let [cmd-type (if (coll? command) (first command) command)]
      (case cmd-type
        :event/prevent-default
        (let [e (:replicant/dom-event event-data)]
          (t/log! :info ["Preventing default event behavior" "Event=" (when e (.-type e))])
          (when e (.preventDefault e)))

        :db/transact
        (let [[_ tx-data] command]
          (t/log! :info ["Transacting data:" tx-data])
          (js/console.log "Transacting...")
          (try

            (ds/transact! conn tx-data)

            (catch js/Error e
              (t/log! :error ["Transaction failed:" (.-message e)]))))

        ;; Add more cases here for other commands
        (t/log! :warn ["Unknown command" command])))
    (catch js/Error e
      ;; Log to console for better stack trace in browser dev tools
      (js/console.error "Error in command operation:" e "Command:" command)
      (t/log! :error ["Error in convert-command-to-operation:" (.-message e)
                      "Command was:" command
                      "Command type:" (type command)
                      "Is collection?" (coll? command)]))))


(defn process-command
  "Process a command recursively, handling nested command vectors"
  [event-data conn command]
  (cond
    ;; If it's a vector of vectors (multiple commands), process each one
    (and (vector? command) (not-empty command) (vector? (first command)))
    (doseq [cmd command]
      (process-command event-data conn cmd))
    ;; Otherwise, execute the command directly
    :else
    (convert-command-to-operation event-data conn command)))

(defn execute-actions
  "execute actions"
  [event-data conn command]
  (js/console.log "Execute actions...")
  (t/log! :info ["Creating event handler function for command:" (pr-str command)])
  (try
    (when command                                           ;; Only process if command exists
      (process-command event-data conn command)
      )
    (catch js/Error e
      (t/log! :error ["Error in event handler:" (.-message e)])
      (t/log! :error ["Stack trace:" (.-stack e)])
      (t/log! :error ["Command was:" (pr-str command)
                      "Command type:" (type command)]))))

(defn init
  "Initializes the application, sets up DB watcher, and renders the UI"
  []
  (let [el (js/document.getElementById "app")]
    (when el                                                ;; Only attempt to render if the element exists
      ;; Set up watcher to re-render when DB changes
      (add-watch
        conn ::render
        (fn [_ _ _old-db new-db]
          ;; Render the UI when DB changes
          (r/render el (render-intl-tel-input new-db)))))

    (r/set-dispatch!
      (fn [event-data actions]
        (js/console.log "Dispatched...")
        (->> actions
             (interpolate-actions
               (:replicant/dom-event event-data))
             (execute-actions event-data conn))))

    ;; Set initial location on startup
    (ds/transact! conn [{:db/ident       :system/app
                            :app/started-at (js/Date.)}])))
