(ns time-tracker.timers.rest-test
  (:require [clojure.test :refer :all]
            [clojure.string :as s]
            [cheshire.core :as json]
            [time-tracker.fixtures :as fixtures]
            [time-tracker.db :as db]
            [time-tracker.timers.db :as timers-db]
            [time-tracker.projects.test-helpers :as projects-helpers]
            [time-tracker.test-helpers :refer [populate-db] :as helpers]
            [time-tracker.util :as util]))

(use-fixtures :once fixtures/init! fixtures/serve-app)
(use-fixtures :each fixtures/isolate-db)

(defn- timers-api
  []
  (s/join [(helpers/settings :api-root) "timers/"]))

(deftest list-all-owned-timers-test
  (let [task-id1 (first (:task-ids (populate-db "gid1")))
        task-id2 (first (:task-ids (populate-db "gid2")))
        url            (timers-api)
        current-time   (util/current-epoch-seconds)
        seconds-in-day (* 60 60 24)
        timer1         (timers-db/create! (db/connection)
                                          task-id1
                                          "gid1"
                                          current-time
                                          "")
        timer2         (timers-db/create! (db/connection)
                                          task-id1
                                          "gid1"
                                          current-time
                                          "")
        timer3         (timers-db/create! (db/connection)
                                          task-id2
                                          "gid2"
                                          current-time
                                          "")
        ;; Create a timer yesterday.
        timer4         (timers-db/create! (db/connection)
                                          task-id1
                                          "gid1"
                                          (- current-time seconds-in-day)
                                          "")
        ;; Create a timer in the future.
        timer5         (timers-db/create! (db/connection)
                                          task-id1
                                          "gid1"
                                          (+ current-time seconds-in-day)
                                          "")]
    (testing "A user should only see the timers they own"
      (let [{:keys [status body]} (helpers/http-request :get url "gid1")]
        (is (= 200 status))
        (is (= (set (map :id [timer1 timer2 timer4 timer5]))
               (set (map #(get % "id") body))))))

    (testing "A user should be able to filter timers between epochs."
      (let [query-string          (str "?start=" (- current-time 5) "&end=" (+ current-time 5))
            {:keys [status body]} (helpers/http-request :get (str url query-string) "gid1")]
        (is (= 200 status))
        (is (= (set (map :id [timer1 timer2]))
               (set (map #(get % "id") body))))))

    (testing "An invalid request should fail."
      (let [query-string          (str "?start=foobar&end=" (+ current-time 5))
            {:keys [status body]} (helpers/http-request :get (str url query-string) "gid1")]
        (is (= 400 status))))))
