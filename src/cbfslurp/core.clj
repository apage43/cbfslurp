(ns cbfslurp.core
  (:gen-class)
  (:use [clojure.tools.cli :only [cli]])
  (:require [clj-http.client :as http]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [cbdrawer.view :as view]
            [cbdrawer.ops :as cb]
            [cbdrawer.client :as cbcli]))

(defn cbfs-files [factory & [start end]]
  (->> (view/view-seq
        (view/view-url (cbcli/capi-bases factory) "cbfs" "file_browse")
        {:startkey start :endkey (or end {}) :include_docs true})
       (map (juxt :id (comp :oid :json :doc)))))

(defn do-fetch [cbconn oid destname]
  (cb/with-json-encoding
    (let [blob (cb/get cbconn (str "/" oid))
          node (rand-nth (mapv first (:nodes blob)))
          nobj (cb/get cbconn (str "/" (name node)))
          blobaddr (str "http://" (:addr nobj) (:bindaddr nobj) "/.cbfs/blob/" oid)
          destfile (io/file destname)]
      (io/make-parents destfile)
      (with-open [body (:body (http/get blobaddr {:as :stream}))]
        (io/copy body destfile))
      (println destname))))

(defn grab-stuff [{cburl :couchbase cbbucket :bucket cbpass :password} paths]
  (let [fact (cbcli/factory cbbucket cbpass cburl)
        cbconn (cbcli/client fact)]
    (try 
      (let [files (mapcat (fn [path]
                            (let [splitpath (string/split path #"/")]
                              (cbfs-files fact splitpath (conj splitpath {})))) paths)] 
        (doseq [fu (doall (map (fn [f] (future (do-fetch cbconn (second f) (first f)))) files))]
          @fu) 
        (println "Done!")) 
      (finally (cbcli/shutdown cbconn)))))

(defn -main
  [& args]
  (let [[config trailers usage] (cli args
                                     ["-c" "--couchbase" "URL of Couchbase cluster" :default "http://localhost:8091/"]
                                     ["-b" "--bucket" "CBFS bucket name on Couchbase" :default "cbfs"]
                                     ["-p" "--password" "Password for the Couchbase bucket" :default ""]
                                     ["-h" "--help" "Show help" :default false :flag true])]
    (when (:help config)
      (println usage)
      (System/exit 0))
    (println "Config: " (pr-str config))
    (try (grab-stuff config trailers)
      (finally (shutdown-agents)))))
