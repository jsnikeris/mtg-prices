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

(defn extract-price [price-node]
  (->> price-node html/text (re-find #"\d+\.\d+") Double.))

(defn fetch-set [name]
  (-> (str guide-url (URLEncoder/encode name "utf-8")) URL. html/html-resource
      (html/select [[:table (html/nth-of-type 3)] :tr])
      (html/let-select [[rarity]    [[:td (html/nth-of-type 5)]]
                        [avg-price] [[:td (html/nth-of-type 7)]]
                        [low-price] [[:td (html/nth-of-type 8)]]]
        {:rarity
         (case (->> rarity html/text (re-find #"[LCURM]"))
           "L" :common
           "C" :common
           "U" :uncommon
           "R" :rare
           "M" :mythic)
         :avg-price (extract-price avg-price)
         :low-price (extract-price low-price)})))
