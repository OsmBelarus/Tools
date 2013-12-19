#!/bin/bash
set -x

JAVA_OPTS="-Xmx800m"
JAVA_CLASSPATH=$(find lib/ -name '*.jar' -printf '%p:')classes/
JAVA_EXEC="java $JAVA_OPTS -cp $JAVA_CLASSPATH"

ant || exit 1

rm -rf tmp
mkdir -p tmp/

wget -O tmp/belarus-latest.osm.pbf http://download.geofabrik.de/europe/belarus-latest.osm.pbf || exit 1

pushd $HOME/OsmBelarus-Databases/ || exit 1
git pull || exit 1
popd

time $JAVA_EXEC org.alex73.osm.validators.harady.CheckCities \
    --pbf=tmp/belarus-latest.osm.pbf \
    --dav=$HOME/OsmBelarus-Databases/Nazvy_nasielenych_punktau/list.csv \
    --out=$HOME/public_html/nazvy.html \
  || exit 1

time $JAVA_EXEC -DdisableAddrStreet org.alex73.osm.validators.vulicy.CheckStreets \
    --pbf=tmp/belarus-latest.osm.pbf \
    --dav=$HOME/OsmBelarus-Databases/Nazvy_nasielenych_punktau/list.csv \
    --po-dir=$HOME/OsmBelarus-Databases/Nazvy_vulic.OmegaT/target/ \
    --out-dir=$HOME/public_html/ \
  || exit 1
