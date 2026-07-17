#!/usr/bin/env bb
(require '[babashka.classpath :as cp]
         '[babashka.fs :as fs]
         '[clojure.test :as t])

(cp/add-classpath (str (fs/parent (fs/absolutize *file*))))

(def suites '[kiyome.cells.surface-cleaning.test-state-machine
              kiyome.methods.test-charter-gates])
(apply require suites)

(let [{:keys [fail error]} (apply t/run-tests suites)]
  (System/exit (if (zero? (+ fail error)) 0 1)))
