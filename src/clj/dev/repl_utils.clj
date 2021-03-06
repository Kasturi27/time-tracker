(ns dev.repl-utils
  (:require [time-tracker.core :as core]
            [time-tracker.web.service :as web-service]))

(defn start-app! []
  (core/mount-init! {:options {:config-file "config/config.dev.edn"}})
  (web-service/start-server!))

(defn restart-server! []
  (web-service/stop-server!)
  (web-service/start-server!))
