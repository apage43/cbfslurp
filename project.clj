(defproject cbfslurp "0.1.0-SNAPSHOT"
  :description "slurp files and folders off cbfs"
  :url "http://github.com/apage43/cbfslurp"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/tools.cli "0.2.2"]
                 [apage43/cbdrawer "0.1.0-SNAPSHOT"]]
  :main cbfslurp.core)
