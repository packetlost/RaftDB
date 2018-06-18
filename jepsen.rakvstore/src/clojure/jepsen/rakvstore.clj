;; Copyright (c) 2018 Pivotal Software Inc, All Rights Reserved.
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;       http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.
;;

(ns jepsen.rakvstore
    (:require [clojure.tools.logging :refer :all]
      [clojure.string :as str]
      [jepsen [cli :as cli]
       [control :as c]
       [db :as db]
       [tests :as tests]]
      [jepsen.control.util :as cu]
      [jepsen.os.debian :as debian]))


(def dir "/opt/rakvstore")
(def logDir "/opt/rakvstore/log")
(def configurationFile "/opt/rakvstore/releases/1/sys.config")
(def vmArgsFile "/opt/rakvstore/releases/1/vm.args")
(def binary "/opt/rakvstore/bin/ra_kv_store_release")


(defn db
      "RA KV Store."
      []
      (reify db/DB
             (setup! [_ test node]
                     (info node "installing RA KV Store")
                     (c/su
                       (let [url (str "file:///vagrant/ra_kv_store_release-1.tar.gz")]
                            (cu/install-archive! url dir))
                       (let [configuration (com.rabbitmq.jepsen.Utils/configuration test node)]
                            (c/exec :echo configuration :| :tee configurationFile)
                            )
                       (let [vmArgs (com.rabbitmq.jepsen.Utils/vmArgs)]
                            (c/exec :echo vmArgs :| :tee vmArgsFile)
                            )
                       (c/exec :mkdir logDir)
                       (c/exec binary "start")
                       )
                     )
             (teardown! [_ test node]
                        (info node "tearing down RA KV Store"))))

(defn rakvstore-test
      "Given an options map from the command line runner (e.g. :nodes, :ssh,
      :concurrency ...), constructs a test map."
      [opts]
      (merge tests/noop-test
             opts
             {:name "rakvstore"
              :os   debian/os
              :db   (db)}))

(defn -main
  "Handles command line arguments. Can either run a test, or a web server for
  browsing results."
  [& args]
  (cli/run! (cli/single-test-cmd {:test-fn rakvstore-test})
            args))