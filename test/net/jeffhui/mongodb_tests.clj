(ns net.jeffhui.mongodb-tests
  "Tests of mongodb wrapper. This relies on a connection to a mongodb server."
  (:use net.jeffhui.mongodb
	clojure.test))

(def config {:db "TEST_DB", :host "localhost", :port 27017})
(def coll "MyTestCollection")
(def conn (mongo-connect (config :host) (config :port)))

(use-fixtures :each
	      (fn [f]
		(with-mongo conn (config :db)
		  (try (f) (finally (drop-coll coll) (drop-db))))))

(deftest doc-map-test
  (let [d {:a "hello" :b "world" :_id "YES" :_ns "NO"}]
    (is (= (doc-map (create-doc d)) d))))

(deftest no-docs-exist
  (is (empty? (get-colls))))

(deftest save-and-find-one-doc
  (let [d {:a "apples", :b "bananas"}
	coll (get-coll coll)
	fil #(dissoc % :_id :_ns)]
    (save-docs coll d)
    (are [x y] (= x y)
	 1 (count-docs coll :all)
	 d (fil (get-docs coll :first))
	 d (fil (first (get-docs coll :all)))
	 d (fil (get-docs coll :first :where {:a "apples"}))
	 [d] (map fil (get-docs coll :all :where {:a "apples"}))
	 d (fil (with-mongo-coll coll (get-docs :first))))))

(deftest save-and-find-multiple-docs
  (with-collection coll
    (let [imap (fn [x] {:i x})
	  d (map imap (range 50))]
      (save-docs d)
      (are [x y] (= x y)
	   50 (count-docs :all)
	   0 (:i (get-docs :first))
	   2 (:i (get-docs :first :where {:i 2}))
	   (range 10) (map :i (get-docs :all :where {:i {:$lt 10}}))
	   (range 10) (map :i (get-docs :all :limit 10))
	   (range 1 10) (map :i (get-docs :all
					  :where {:i {:$lt 10}}
					  :skip 1))
	   (range 9 -1 -1) (map :i (get-docs :all
					    :where {:i {:$lt 10}}
					    :sort {:i -1}))))))