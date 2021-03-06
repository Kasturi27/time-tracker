(ns time-tracker.migration
  (:require [ragtime.repl]
            [ragtime.jdbc]
            [time-tracker.config :as config]
            [taoensso.timbre :as log]))

(defn db-spec [] {:connection-uri (config/get-config :db-connection-string)})

(defn migration-config []
  {:datastore  (ragtime.jdbc/sql-database (db-spec))
   :migrations (ragtime.jdbc/load-resources "migrations")})

(defn migrate-db []
  (try
    (let [out (with-out-str
                (ragtime.repl/migrate (migration-config)))]
      (log/info {:event          ::applied-migrations
                 :ragtime-output out}))
    (catch Exception ex
      (log/error ex {:event ::migration-failed})
      (System/exit 1))))

(defn rollback-db []
  (try
    (let [out (with-out-str
                (ragtime.repl/rollback (migration-config)))]
      (log/info {:event          ::rolled-back-migration
                 :ragtime-output out}))
    (catch Exception ex
      (log/error ex {:event ::migration-rollback-failed})
      (System/exit 1))))

;; These are called by `lein migrate` and `lein rollback`.

(defn lein-migrate-db []
  (migrate-db))

(defn lein-rollback-db []
  (rollback-db))
