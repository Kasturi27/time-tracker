(ns time-tracker.invoices.db
  (:require [clojure.java.jdbc :as jdbc]
            [time-tracker.util :refer [statement-success?]]
            [yesql.core :refer [defqueries]]))

(defqueries "time_tracker/invoices/sql/db.sql")

(defn create!
  [connection invoice]
  (first (jdbc/insert! connection "invoice"
                       {"client" (:client invoice)
                        "address" (:address invoice)
                        "currency" (name (:currency invoice))
                        "utc_offset" (:utc-offset invoice)
                        "notes" (:notes invoice)
                        "items" (pr-str (:items invoice)),
                        "subtotal" (:subtotal invoice)
                        "amount_due" (:amount-due invoice)
                        "from_date" (:from-date invoice)
                        "to_date" (:to-date invoice)
                        "tax_amounts" (pr-str (:tax-amounts invoice)),
                        "paid" false
                        "usable" true})))

(defn retrieve-all
  "Retrieves a list of all the invoices."
  [connection]
  (retrieve-all-invoices-query {} {:connection connection}))

(defn retrieve
  "Retrieves a specific invoice."
  [connection invoice-id]
  (first (retrieve-invoice-query {:invoice_id invoice-id}
                                 {:connection connection})))

(defn mark-invoice-paid!
  "Sets an invoice as paid and returns the updated invoice."
  [connection invoice-id {:keys [paid]}]
  ;; The Postgres RETURNING clause doesn't work,
  ;; so using two queries for now
  (when (statement-success?
         (update-invoice-paid-query! {:paid       paid
                                 :invoice_id invoice-id}
                                {:connection connection}))
    (jdbc/get-by-id connection "invoice" invoice-id)))


(defn mark-invoice-unusable!
  "Sets an invoice as unusable and returns the updated invoice."
  [connection invoice-id {:keys [usable]}]
  ;; The Postgres RETURNING clause doesn't work,
  ;; so using two queries for now
  (when (statement-success?
         (update-invoice-usable-query! {:usable       usable
                                        :invoice_id invoice-id}
                                       {:connection connection}))
    (jdbc/get-by-id connection "invoice" invoice-id)))
