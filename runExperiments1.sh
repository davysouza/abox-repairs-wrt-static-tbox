#! /bin/bash

# the first line is necessary to make this script runnable via ./runExperiment1.sh

for ont in `cat Ontologies_ORE/pool_sample/el/instantiation/fileorder.txt`
do
    echo time timeout 600 java -cp repair-0.0.1-SNAPSHOT-jar-with-dependencies.jar de.tu_dresden.lat.abox_repairs.experiments.RunExperiment1 Ontologies_ORE/pool_sample/files/$ont IQ 0.1 0.1
    time timeout 600 java -cp repair-0.0.1-SNAPSHOT-jar-with-dependencies.jar de.tu_dresden.lat.abox_repairs.experiments.RunExperiment1 Ontologies_ORE/pool_sample/files/$ont IQ 0.1 0.1
done
