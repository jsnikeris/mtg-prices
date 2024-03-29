(ns mtg-prices.core
  (:import [java.net URL URLEncoder])
  (:require [net.cgrand.enlive-html :as html]
            [clojure.string :as str]))

(def guide-url
  "http://magic.tcgplayer.com/db/price_guide.asp?setname=")

(def sets
  ["Scars of Mirrodin"
   "Mirrodin Besieged"
   "New Phyrexia"
   "Magic 2012 (M12)"
   "Innistrad"])

(def booster-dist
  {:token 1/2 :land 1 :common 10 :uncommon 3 :rare 7/8 :mythic 1/8})

(defn extract-price [price-node]
  (->> price-node html/text (re-find #"\d+\.\d+") Double.))

(defn fetch-set [name]
  (-> (str guide-url (URLEncoder/encode name "utf-8")) URL. html/html-resource
      (html/select [[:table (html/nth-of-type 3)] :tr])
      (html/let-select [[name]      [[:td (html/nth-of-type 1)]]
                        [rarity]    [[:td (html/nth-of-type 5)]]
                        [avg-price] [[:td (html/nth-of-type 7)]]
                        [low-price] [[:td (html/nth-of-type 8)]]]
        (let [card-name (-> name html/text (str/replace " " ""))
              land-re #"Swamp|Island|Forest|Mountain|Plains"]
          {:name card-name
           :rarity (if (re-find land-re card-name)
                     :land
                     (case (->> rarity html/text (re-find #"[CURMT]"))
                       "C" :common
                       "U" :uncommon
                       "R" :rare
                       "M" :mythic
                       "T" :token))
           :avg-price (extract-price avg-price)
           :low-price (extract-price low-price)}))))

(defn rarity-avg [set rarity price-metric]
  (let [prices (for [card set :when (= (:rarity card) rarity)]
                 (price-metric card))]
    (if (seq prices)
      (/ (reduce + prices) (count prices))
      0)))

(defn booster-value [set-name price-metric]
  (let [set (fetch-set set-name)]
    (reduce +
      (for [[rarity count] booster-dist]
        (* count (rarity-avg set rarity price-metric))))))
