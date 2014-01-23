#!/bin/bash
set -x
cd `dirname $0`

JAVA_CLASSPATH=$(find lib/ -name '*.jar' -printf '%p:')classes/
JAVA_EXEC="java $JAVA_OPTS -cp $JAVA_CLASSPATH"

pushd ../../OsmBelarus-Databases/ || exit 1
git pull || exit 1
popd

time $JAVA_EXEC org.alex73.osm.validators.harady.CheckCities2 || exit 1
time $JAVA_EXEC -DdisableAddrStreet org.alex73.osm.validators.vulicy.CheckStreets2 || exit 1
