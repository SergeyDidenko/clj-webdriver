;; ## Core Element-related Functions ##
;;
;; This namespace implements the following protocols:
;;
;;  * IElement
;;  * IFormElement
;;  * ISelectElement
(in-ns 'clj-webdriver.core)

(extend-type Element

  ;; Element action basics
  IElement
  (attribute [element attr]
    (if (= attr :text)
      (text element)
      (let [attr (name attr)
            boolean-attrs ["async", "autofocus", "autoplay", "checked", "compact", "complete",
                           "controls", "declare", "defaultchecked", "defaultselected", "defer",
                           "disabled", "draggable", "ended", "formnovalidate", "hidden",
                           "indeterminate", "iscontenteditable", "ismap", "itemscope", "loop",
                           "multiple", "muted", "nohref", "noresize", "noshade", "novalidate",
                           "nowrap", "open", "paused", "pubdate", "readonly", "required",
                           "reversed", "scoped", "seamless", "seeking", "selected", "spellcheck",
                           "truespeed", "willvalidate"]
            webdriver-result (.getAttribute (:webelement element) (name attr))]
        (if (some #{attr} boolean-attrs)
          (when (= webdriver-result "true")
            attr)
          webdriver-result))))
  
  (click [element]
    (.click (:webelement element))
    (cache/set-status :check)
    nil)

	(css-value [element property]
		(.getCssValue (:webelement element) property))

  (displayed? [element]
    (.isDisplayed (:webelement element)))

  (exists? [element]
    (not (nil? (:webelement element))))

  (flash [element]
    (let [original-color (if (css-value element "background-color")
                           (css-value element "background-color")
                           "transparent")
          orig-colors (repeat original-color)
          change-colors (interleave (repeat "red") (repeat "blue"))]
      (doseq [flash-color (take 12 (interleave change-colors orig-colors))]
        (execute-script* (.getWrappedDriver (:webelement element))
                         (str "arguments[0].style.backgroundColor = '"
                              flash-color "'")
                         (:webelement element))
        (Thread/sleep 80)))
    element)

  (focus [element]
    (execute-script*
     (.getWrappedDriver (:webelement element)) "return arguments[0].focus()" (:webelement element)))

  (html [element]
    (browserbot (.getWrappedDriver (:webelement element)) "getOuterHTML" (:webelement element)))

  (location [element]
    (let [loc (.getLocation (:webelement element))
          x   (.x loc)
          y   (.y loc)]
      {:x x, :y y}))

  (location-once-visible [element]
    (let [loc (.getLocationOnScreenOnceScrolledIntoView (:webelement element))
          x   (.x loc)
          y   (.y loc)]
      {:x x, :y y}))

  (present? [element]
    (and (exists? element) (visible? element)))

  (size [element]
    (let [size-obj (.getSize (:webelement element))
          w (.width size-obj)
          h (.height size-obj)]
      {:width w, :height h}))

  (rectangle [element]
    (let [loc (location element)
          el-size (size element)]
      (java.awt.Rectangle. (:x loc)
                           (:y loc)
                           (:width el-size)
                           (:height el-size))))

  (intersect*? [element-a element-b]
    (let [rect-a (rectangle element-a)
          rect-b (rectangle element-b)]
      (.intersects rect-a rect-b)))
  
  (tag [element]
    (.getTagName (:webelement element)))

  (text [element]
    (.getText (:webelement element)))
  
  (value [element]
    (.getAttribute (:webelement element) "value"))

  (visible? [element]
    (.isDisplayed (:webelement element)))

  (xpath [element]
    (browserbot (.getWrappedDriver (:webelement element)) "getXPath" (:webelement element) []))
  

  IFormElement
  (deselect [element]
    (if (.isSelected (:webelement element))
      (toggle (:webelement element))
      element))
  
  (enabled? [element]
    (.isEnabled (:webelement element)))
  
  (input-text [element s]
    (.sendKeys (:webelement element) (into-array CharSequence (list s)))
    element)
  
  (submit [element]
    (.submit (:webelement element))
    (cache/set-status :flush)
    nil)
  
  (clear [element]
    (.clear (:webelement element))
    element)
  
  (select [element]
    (if-not (.isSelected (:webelement element))
      (.click (:webelement element))
      element))
  
  (selected? [element]
    (.isSelected (:webelement element)))

  (send-keys [element s]
    (.sendKeys (:webelement element) (into-array CharSequence (list s)))
    element)
  
  (toggle [element]
    (.click (:webelement element))
    element)


  ISelectElement
  (all-options [element]
    (let [select-list (Select. (:webelement element))]
      (lazy-seq (init-elements (.getOptions select-list)))))

  (all-selected-options [element]
    (let [select-list (Select. (:webelement element))]
      (lazy-seq (init-elements (.getAllSelectedOptions select-list)))))

  (deselect-option [element attr-val]
    {:pre [(or (= (first (keys attr-val)) :index)
               (= (first (keys attr-val)) :value)
               (= (first (keys attr-val)) :text))]}
    (case (first (keys attr-val))
      :index (deselect-by-index element (:index attr-val))
      :value (deselect-by-value element (:value attr-val))
      :text  (deselect-by-text element (:text attr-val))))

  (deselect-all [element]
    (let [cnt-range (->> (all-options element)
                         count
                         (range 0))]
      (doseq [idx cnt-range]
        (deselect-by-index element idx))
      element))

  (deselect-by-index [element idx]
    (let [select-list (Select. (:webelement element))]
      (.deselectByIndex select-list idx)
      element))

  (deselect-by-text [element text]
    (let [select-list (Select. (:webelement element))]
      (.deselectByVisibleText select-list text)
      element))

  (deselect-by-value [element value]
    (let [select-list (Select. (:webelement element))]
      (.deselectByValue select-list value)
      element))

  (first-selected-option [element]
    (let [select-list (Select. (:webelement element))]
      (init-element (.getFirstSelectedOption select-list))))

  (multiple? [element]
    (let [value (attribute element "multiple")]
      (or (= value "true")
          (= value "multiple"))))

  (select-option [element attr-val]
    {:pre [(or (= (first (keys attr-val)) :index)
               (= (first (keys attr-val)) :value)
               (= (first (keys attr-val)) :text))]}
    (case (first (keys attr-val))
      :index (select-by-index element (:index attr-val))
      :value (select-by-value element (:value attr-val))
      :text  (select-by-text element (:text attr-val))))

  (select-all [element]
    (let [cnt-range (->> (all-options element)
                         count
                         (range 0))]
      (doseq [idx cnt-range]
        (select-by-index element idx))
      element))

  (select-by-index [element idx]
    (let [select-list (Select. (:webelement element))]
      (.selectByIndex select-list idx)
      element))

  (select-by-text [element text]
    (let [select-list (Select. (:webelement element))]
      (.selectByVisibleText select-list text)
      element))

  (select-by-value [element value]
    (let [select-list (Select. (:webelement element))]
      (.selectByValue select-list value)
      element))

  IFind
  (find-element-by [element by]
    (let [by (if (map? by)
               (by-query (build-query by :local))
               by)]
      (init-element (.findElement (:webelement element) by))))
  
  (find-elements-by [element by]
    (let [by (if (map? by)
               (by-query (build-query by :local))
               by)
          els (.findElements (:webelement element) by)]
      (if (seq els)
        (lazy-seq (map init-element els))
        (lazy-seq (map init-element [nil])))))

  (find-element [element by]
    (find-element-by element by))

  (find-elements [element by]
    (find-elements-by element by)))
