;; Namespace with implementations of test cases
(ns clj-webdriver.test.common
  (:use clojure.test
        [clj-webdriver core util window-handle wait options form-helpers]
        [clj-webdriver.test.util :only [thrown?]]
        [clj-webdriver.test.config :only [test-base-url]])
  (:require [clj-webdriver.cache :as cache]
            [clojure.tools.logging :as log])
  (:import [clj_webdriver.driver.Driver]
           [org.openqa.selenium TimeoutException]))

;;;;;;;;;;;;;;;;;;;;;;;;
;;;                  ;;;
;;; Test Definitions ;;;
;;;                  ;;;
;;;;;;;;;;;;;;;;;;;;;;;;
(defn test-browser-basics
  [driver]
  (is (= clj_webdriver.driver.Driver (class driver)))
  (is (= test-base-url (current-url driver)))
  (is (= "Ministache" (title driver)))
  (is (boolean (re-find #"(?i)html>" (page-source driver)))))

(defn back-forward-should-traverse-browser-history
  [driver]
  (-> driver
      (find-element {:tag :a, :text "example form"})
      click)
  (Thread/sleep 500) ;; race condition issue with OperaDriver (on my machine, at least)
  (is (= (str test-base-url "example-form") (current-url driver)))
  (back driver)
  (is (= test-base-url (current-url driver)))
  (forward driver)
  (is (= (str test-base-url "example-form") (current-url driver))))

(defn to-should-open-given-url-in-browser
  [driver]
  (to driver (str test-base-url "example-form"))
  (is (= (str test-base-url "example-form") (current-url driver)))
  (is (= "Ministache" (title driver))))

(defn should-be-able-to-find-element-bys-using-low-level-by-wrappers
  [driver]
  (-> driver
      (find-element {:tag :a, :text "example form"})
      click)
  (is (= "first_name"
         (attribute (find-element-by driver (by-id "first_name")) :id)))
  (is (= "home"
         (text (find-element-by driver (by-link-text "home")))))
  (is (= "example form"
         (text (find-element-by driver (by-partial-link-text "example")))))
  (is (= "first_name"
         (attribute (find-element-by driver (by-name "first_name")) :id)))
  (is (= "home"
         (text (find-element-by driver (by-tag "a")))))
  (is (= "home"
         (text (find-element-by driver (by-xpath "//a[text()='home']")))))
  (is (= "home"
         (text (find-element-by driver (by-class-name "menu-item")))))
  (is (= "home"
         (text (find-element-by driver (by-css-selector "#footer a.menu-item")))))
  (is (= "social_media"
         (attribute (find-element-by driver (by-attr-contains :option :value "cial_")) :value)))
  (is (= "social_media"
         (attribute (find-element-by driver (by-attr-starts :option :value "social_")) :value)))
  (is (= "social_media"
         (attribute (find-element-by driver (by-attr-ends :option :value "_media")) :value)))
  (is (= "france"
         (attribute (find-element-by driver (by-has-attr :option :value)) :value)))
  (to driver test-base-url)
  (is (= "first odd"
         (attribute (find-element-by driver (by-class-name "first odd")) :class)))
  ;; (is (= "http://clojure.blip.tv/file/4824610/"
  ;;        (attribute (find-element-by (find-element driver {:tag :li, :text #"simple"}) (by-tag :a)) :href)))
  ;; (is (= "http://clojure.blip.tv/file/4824610/"
  ;;        (attribute (find-element-by (find-element driver {:tag :li, :text #"simple"}) {:tag :a}) :href)))
  )

(defn find-element-should-support-basic-attr-val-map
  [driver]
  (is (= "Moustache"
         (text (nth (find-elements driver {:tag :a}) 1))))
  (is (= "Moustache"
         (text (find-element driver {:class "external"}))))
  (is (= "first odd"
         (attribute (find-element driver {:class "first odd"}) :class)))
  (is (= "first odd"
         (attribute (find-element driver {:tag :li, :class "first odd"}) :class)))
  (is (= "https://github.com/cgrand/moustache"
         (attribute (find-element driver {:text "Moustache"}) "href")))
  (is (= 8
         (count (find-elements driver {:tag :a}))))
  (-> driver
      (find-element {:tag :a, :text "example form"})
      click)
  (is (= "first_name"
         (attribute (find-element driver {:type "text"}) "id")))
  (is (= "first_name"
         (attribute (find-element driver {:tag :input, :type "text"}) "id")))
  (is (= "first_name"
         (attribute (find-element driver {:tag :input, :type "text", :name "first_name"}) "id")))
  ;; (is (= "first_name"
  ;;        (attribute (find-element driver {:tag :input, :type "text", :name #"first_"}) "id")))
  ;; (is (= "last_name"
  ;;        (attribute (find-element driver {:tag :input, :type "text", :name #"last_"}) "id")))n
  ;; (is (= "Smith"
  ;;        (attribute (find-element driver {:tag :input, :type "text", :name #"last_"}) "value")))
  )

;; (defn find-element-should-support-regexes-in-attr-val-map
;;   [driver]
;;   (is (= "Moustache"
;;          (text (find-element driver {:tag :a, :class #"exter"}))))
;;   (is (= "Moustache"
;;          (text (find-element driver {:tag :a, :text #"Mous"}))))
;;   (is (= "Moustache"
;;          (text (find-element driver {:tag :a, :class "external", :href #"github"}))))
;;   (is (= "Moustache"
;;          (text (find-element driver {:tag :a, :class #"exter", :href #"github"}))))
;;   (is (= 3
;;          (count (find-elements driver {:class #"-item"}))))
;;   (is (= 3
;;          (count (find-elements driver {:tag :a, :class #"-item"}))))
;;   (is (= 1
;;          (count (find-elements driver {:tag :a, :text #"hom"}))))
;;   (is (= 1
;;          (count (find-elements driver {:tag :a, :text #"(?i)HOM"}))))
;;   (is (= 2
;;          (count (find-elements driver {:tag :a, :class #"exter", :href #"github"})))))


(defn find-element-should-support-hierarchical-querying
  [driver]
  (is (= "Moustache"
         (text (find-element driver [{:tag :div, :id "content"}, {:tag :a, :class "external"}]))))
  ;; (is (= "Moustache"
  ;;        (text (find-element driver [{:tag :div, :id "content"}, {:tag :a, :class #"exter"}]))))
  ;; (is (= "Moustache"
  ;;        (text (find-element driver [{:tag :div, :id "content"}, {:tag :a, :href #"github"}]))))
  (is (= "home"
         (text (find-element driver [{:tag :*, :id "footer"}, {:tag :a}]))))
  (is (= 3
         (count (find-elements driver [{:tag :*, :id "footer"}, {:tag :a}]))))
  ;; (is (= 2
  ;;        (count (find-elements driver [{:tag :div, :id "content"}, {:tag :a, :class #"exter"}]))))
  )

(defn hierarchical-querying-should-not-support-css-or-xpath-attrs
  [driver]
  (is (thrown? IllegalArgumentException
               (find-element driver [{:tag :div, :id "content", :css "div#content"}, {:tag :a, :class "external"}])))
  (is (thrown? IllegalArgumentException
               (find-element driver [{:tag :div, :id "content", :xpath "//div[@id='content']"}, {:tag :a, :class "external"}]))))

(defn exists-should-return-truthy-falsey-and-should-not-throw-an-exception
  [driver]
  (is (-> driver
          (find-element {:tag :a})
          exists?))
  (is (not
       (-> driver
           (find-element {:tag :area})
           exists?))))

(defn visible-should-return-truthy-falsey-when-visible
  [driver]
  (is (-> driver
          (find-element {:tag :a, :text "Moustache"})
          visible?))
  (is (not
       (-> driver
           (find-element {:tag :a, :href "#pages"})
           visible?)))
  (is (-> driver
          (find-element {:tag :a, :text "Moustache"})
          displayed?))
  (is (not
       (-> driver
           (find-element {:tag :a, :href "#pages"})
           displayed?))))

(defn present-should-return-truthy-falsey-when-exists-and-visible
  [driver]
  (is (-> driver
          (find-element {:tag :a, :text "Moustache"})
          present?))
  (is (not
       (-> driver
           (find-element {:tag :a, :href "#pages"})
           present?))))

(defn should-be-able-to-determine-if-elements-intersect-each-other
  [driver]
  (click (find-element driver {:tag :a, :text "example form"}))
  (is (intersect? (find-element driver {:id "first_name"})
                  (find-element driver {:id "personal-info-wrapper"})))
  (is (not
       (intersect? (find-element driver {:id "first_name"})
                   (find-element driver {:id "last_name"})))))

;; Default wrap for strings is double quotes
(defn generated-xpath-should-wrap-strings-in-double-quotes
  [driver]
  (is (find-element driver {:text "File's Name"})))

(defn xpath-function-should-return-string-xpath-of-element
  [driver]
  (is (= (xpath (find-element driver {:tag :a, :text "Moustache"})) "/html/body/div[2]/div/p/a")))

(defn html-function-should-return-string-html-of-element
  [driver]
  (is (re-find #"href=\"https://github\.com/cgrand/moustache\"" (html (find-element driver {:tag :a, :text "Moustache"})))))

(defn find-table-cell-should-find-cell-with-coords
  [driver]
  (is (= "th"
         (.toLowerCase (tag (find-table-cell driver
                                             (find-element driver {:id "pages-table"})
                                             [0 0])))))
  (is (= "th"
         (.toLowerCase (tag (find-table-cell driver
                                             (find-element driver {:id "pages-table"})
                                             [0 1])))))
  (is (= "td"
         (.toLowerCase (tag (find-table-cell driver
                                             (find-element driver {:id "pages-table"})
                                             [1 0])))))
  (is (= "td"
         (.toLowerCase (tag (find-table-cell driver
                                             (find-element driver {:id "pages-table"})
                                             [1 1]))))))

(defn find-table-row-should-find-all-cells-for-row
  [driver]
  (is (= 2
         (count (find-table-row driver
                                (find-element driver {:id "pages-table"})
                                0))))
  (is (= "th"
         (.toLowerCase (tag (first (find-table-row driver
                                                   (find-element driver {:id "pages-table"})
                                                   0))))))
  (is (= "td"
         (.toLowerCase (tag (first (find-table-row driver
                                                   (find-element driver {:id "pages-table"})
                                                   1)))))))

(defn test-form-elements
  [driver]
  (to driver (str test-base-url "example-form"))
  ;; Clear element
  ;; (-> driver
  ;;     (find-element [{:tag :form, :id "example_form"}, {:tag :input, :name #"last_"}])
  ;;     clear)
  ;; (is (= ""
  ;;        (value (find-element driver [{:tag :form, :id "example_form"}, {:tag :input, :name #"last_"}]))))
  ;; Radio buttons
  (is (= true
         (selected? (find-element driver {:tag :input, :type "radio", :value "male"}))))
  (-> driver
      (find-element {:tag :input, :type "radio", :value "female"})
      select)
  (is (= true
         (selected? (find-element driver {:tag :input, :type "radio", :value "female"}))))
  (-> driver
      (find-element {:tag :radio, :value "male"})
      select)
  (is (= true
         (selected? (find-element driver {:tag :input, :type "radio", :value "male"}))))
  ;; Checkboxes
  ;; (is (= false
  ;;        (selected? (find-element driver {:tag :input, :type "checkbox", :name #"(?i)clojure"}))))
  ;; (-> driver
  ;;     (find-element {:tag :input, :type "checkbox", :name #"(?i)clojure"})
  ;;     toggle)
  ;; (is (= true
  ;;        (selected? (find-element driver {:tag :input, :type "checkbox", :name #"(?i)clojure"}))))
  ;; (-> driver
  ;;     (find-element {:tag :checkbox, :name #"(?i)clojure"})
  ;;     click)
  ;; (is (= false
  ;;        (selected? (find-element driver {:tag :input, :type "checkbox", :name #"(?i)clojure"}))))
  ;; (-> driver
  ;;     (find-element {:tag :checkbox, :type "checkbox", :name #"(?i)clojure"})
  ;;     select)
  ;; (is (= true
  ;;        (selected? (find-element driver {:tag :input, :type "checkbox", :name #"(?i)clojure"}))))
  ;; Text fields
  (println (current-url driver))
  (-> driver
      (find-element {:tag :input, :id "first_name"})
      (input-text "foobar"))
  (is (= "foobar"
         (value (find-element driver {:tag :input, :id "first_name"}))))
  (-> driver
      (find-element {:tag :textfield, :id "first_name"})
      clear
      (input-text "clojurian"))
  (is (= "clojurian"
         (value (find-element driver {:tag :textfield, :id "first_name"}))))
  ;; Boolean attributes (disabled, readonly, etc)
  (is (= "disabled"
         (attribute (find-element driver {:id "disabled_field"}) :disabled)))
  (is (= "readonly"
         (attribute (find-element driver {:id "purpose_here"}) :readonly)))
  (is (nil?
       (attribute (find-element driver {:id "disabled_field"}) :readonly)))
  (is (nil?
       (attribute (find-element driver {:id "purpose_here"}) :disabled)))
  ;; Buttons
  ;; (is (= 4
  ;;        (count (find-elements driver {:tag :button*}))))
  ;; (is (= 1
  ;;        (count (find-elements driver {:tag :button*, :class "button-button"}))))
  ;; (is (= 1
  ;;        (count (find-elements driver {:tag :button*, :id "input-input-button"}))))
  ;; (is (= 1
  ;;        (count (find-elements driver {:tag :button*, :class "input-submit-button"}))))
  ;; (is (= 1
  ;;        (count (find-elements driver {:tag :button*, :class "input-reset-button"}))))
  )

(defn select-element-functions-should-behave-as-expected
  [driver]
  (to driver (str test-base-url "example-form"))
  (let [select-el (find-element driver {:tag "select", :id "countries"})]
    (is (= 4
           (count (all-options select-el))))
    (is (= 1
           (count (all-selected-options select-el))))
    (is (= "bharat"
           (attribute (first-selected-option select-el) :value)))
    (is (= "bharat"
           (attribute (first (all-selected-options select-el)) :value)))
    (is (false?
         (multiple? select-el)))
    (select-option select-el
                   {:value "deutschland"})
    (is (= 1
           (count (all-selected-options select-el))))
    (is (= "deutschland"
           (attribute (first-selected-option select-el) :value)))
    (is (= "deutschland"
           (attribute (first (all-selected-options select-el)) :value)))
    (select-by-index select-el
                     0)
    (is (= 1
           (count (all-selected-options select-el))))
    (is (= "france"
           (attribute (first-selected-option select-el) :value)))
    (is (= "france"
           (attribute (first (all-selected-options select-el)) :value)))
    (select-by-text select-el
                    "Haiti")
    (is (= 1
           (count (all-selected-options select-el))))
    (is (= "ayiti"
           (attribute (first-selected-option select-el) :value)))
    (is (= "ayiti"
           (attribute (first (all-selected-options select-el)) :value)))
    (select-by-value select-el
                     "bharat")
    (is (= 1
           (count (all-selected-options select-el))))
    (is (= "bharat"
           (attribute (first-selected-option select-el) :value)))
    (is (= "bharat"
           (attribute (first (all-selected-options select-el)) :value))))
  (let [select-el (find-element driver {:tag "select", :id "site_types"})]
    (is (true?
         (multiple? select-el)))
    (is (= 4
           (count (all-options select-el))))
    (is (zero?
         (count (all-selected-options select-el))))
    (select-option select-el {:index 0})
    (is (= 1
           (count (all-selected-options select-el))))
    (is (= "blog"
           (attribute (first-selected-option select-el) :value)))
    (is (= "blog"
           (attribute (first (all-selected-options select-el)) :value)))
    (select-option select-el {:value "social_media"})
    (is (= 2
           (count (all-selected-options select-el))))
    (is (= "social_media"
           (attribute (second (all-selected-options select-el)) :value)))
    (deselect-option select-el {:index 0})
    (is (= 1
           (count (all-selected-options select-el))))
    (is (= "social_media"
           (attribute (first-selected-option select-el) :value)))
    (is (= "social_media"
           (attribute (first (all-selected-options select-el)) :value)))
    (select-option select-el {:value "search_engine"})
    (is (= 2
           (count (all-selected-options select-el))))
    (is (= "search_engine"
           (attribute (second (all-selected-options select-el)) :value)))
    (deselect-by-index select-el 1)
    (is (= 1
           (count (all-selected-options select-el))))
    (is (= "search_engine"
           (attribute (first-selected-option select-el) :value)))
    (is (= "search_engine"
           (attribute (first (all-selected-options select-el)) :value)))
    (select-option select-el {:value "code"})
    (is (= 2
           (count (all-selected-options select-el))))
    (is (= "code"
           (attribute (last (all-selected-options select-el)) :value)))
    (deselect-by-text select-el "Search Engine")
    (is (= 1
           (count (all-selected-options select-el))))
    (is (= "code"
           (attribute (first-selected-option select-el) :value)))
    (select-all select-el)
    (is (= 4
           (count (all-selected-options select-el))))
    (deselect-all select-el)
    (is (zero?
         (count (all-selected-options select-el))))))

(defn quick-fill-should-accept-special-seq-and-perform-batch-actions-on-form
  [driver]
  (to driver (str test-base-url "example-form"))
  (quick-fill driver
              [{"first_name" clear}
               {"first_name" "Richard"}
               {{:id "last_name"} clear}
               {{:id "last_name"} "Hickey"}
               {{:name "bio"} clear}
               {{:name "bio"} #(input-text % "Creator of Clojure")}
               {{:tag "input", :type "radio", :value "female"} click}
               {{:css "select#countries"} #(select-by-value % "france")}])
  (is (= "Richard"
         (value (find-element driver {:tag :input, :id "first_name"}))))
  (is (= "Hickey"
         (value (find-element driver {:tag :input, :id "last_name"}))))
  (is (= "Creator of Clojure"
         (value (find-element driver {:tag :textarea, :name "bio"}))))
  (is (selected?
       (find-element driver {:tag :input, :type "radio", :value "female"})))
  (is (selected?
       (find-element driver {:tag :option, :value "france"}))))

(defn quick-fill-submit-should-always-return-nil
  [driver]
  (to driver (str test-base-url "example-form"))
  (is (nil?
       (quick-fill-submit driver
                          [{"first_name" clear}
                           {"first_name" "Richard"}
                           {{:id "last_name"} clear}
                           {{:id "last_name"} "Hickey"}
                           {{:name "bio"} clear}
                           {{:name "bio"} #(input-text % "Creator of Clojure")}
                           {{:tag "input", :type "radio", :value "female"} click}
                           {{:css "select#countries"} #(select-by-value % "france")}]))))

(defn should-be-able-to-toggle-between-open-windows
  [driver]
  (is (= 1
         (count (window-handles driver))))
  (is (= "Ministache"
         (:title (window-handle driver))))
  (-> driver
      (find-element {:tag :a, :text "is amazing!"})
      click)
  (is (= "Ministache"
         (:title (window-handle driver))))
  (is (= 2
         (count (window-handles driver))))
  (switch-to-window driver (second (window-handles driver)))
  (is (= (str test-base-url "clojure")
         (:url (window-handle driver))))
  (switch-to-other-window driver)
  (is (= test-base-url
         (:url (window-handle driver))))
  (-> driver
      (switch-to-window (find-window driver {:url (str test-base-url "clojure")})))
  (close driver)
  (is (= test-base-url
         (:url (window-handle driver)))))

(defn wait-until-should-wait-for-condition
  [driver]
  (is (= "Ministache" (title driver)))
  (execute-script driver "setTimeout(function () { window.document.title = \"asdf\"}, 2000)")
  (wait-until driver (fn [d] (= (title d) "asdf")))
  (is (= (title driver) "asdf")))

(defn wait-until-should-throw-on-timeout
  [driver]
  (is (thrown? TimeoutException
               (do
                 (execute-script driver "setTimeout(function () { window.document.title = \"test\"}, 6000)")
                 (wait-until driver (fn [d] (= "test" (title d))))))))

(defn wait-until-should-allow-timeout-argument
  [driver]
  (is (thrown? TimeoutException
               (do
                 (execute-script driver "setTimeout(function () { window.document.title = \"test\"}, 10000)")
                 (wait-until driver (fn [d#] (= (title d#) "test")) 1000)))))

(defn implicit-wait-should-cause-find-to-wait
  [driver]
  (implicit-wait driver 3000)
  (execute-script driver "setTimeout(function () { window.document.body.innerHTML = \"<div id='test'>hi!</div>\"}, 1000)")
  (is (= "test"
         (attribute (find-element-by driver (by-id "test")) :id))))

;; Not sure how we'll test that flash in fact flashes,
;; but at least this will catch changing API's
(defn test-flash-helper
  [driver]
  (-> driver
      (find-element {:tag :a, :text "Moustache"})
      flash))

;;; Fixture fn's ;;;
(defn reset-driver
  [driver]
  (to driver test-base-url))

(defn seed-driver-cache
  [driver]
  (cache/seed driver {:url (current-url driver), {:query [:foo]} "bar"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;                       ;;;
;;; RUN ACTUAL TESTS HERE ;;;
;;;                       ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn run-common-tests
  [driver]
  (doseq [common-test [test-browser-basics
                       back-forward-should-traverse-browser-history
                       to-should-open-given-url-in-browser
                       should-be-able-to-find-element-bys-using-low-level-by-wrappers
                       find-element-should-support-basic-attr-val-map
                       ;; find-element-should-support-regexes-in-attr-val-map
                       find-element-should-support-hierarchical-querying
                       hierarchical-querying-should-not-support-css-or-xpath-attrs
                       exists-should-return-truthy-falsey-and-should-not-throw-an-exception
                       visible-should-return-truthy-falsey-when-visible
                       present-should-return-truthy-falsey-when-exists-and-visible
                       should-be-able-to-determine-if-elements-intersect-each-other
                       generated-xpath-should-wrap-strings-in-double-quotes
                       xpath-function-should-return-string-xpath-of-element
                       html-function-should-return-string-html-of-element
                       find-table-cell-should-find-cell-with-coords
                       find-table-row-should-find-all-cells-for-row
                       test-form-elements
                       select-element-functions-should-behave-as-expected
                       quick-fill-should-accept-special-seq-and-perform-batch-actions-on-form
                       quick-fill-submit-should-always-return-nil
                       should-be-able-to-toggle-between-open-windows
                       wait-until-should-wait-for-condition
                       wait-until-should-throw-on-timeout
                       wait-until-should-allow-timeout-argument
                       implicit-wait-should-cause-find-to-wait
                       test-flash-helper]]
    (reset-driver driver)
    (seed-driver-cache driver)
    (common-test driver)))