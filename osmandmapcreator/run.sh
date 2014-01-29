#!/bin/sh
set -x
cd `dirname $0`

JAVA_CLASSPATH=OsmAndMapCreator.jar:$(find lib/ -name '*.jar' -printf '%p:')../nazvy/classes/
JAVA_EXEC="java $JAVA_OPTS -cp $JAVA_CLASSPATH"

rm -rf tmp
mkdir tmp
wget -O tmp/belarus-latest.osm.bz2 http://be.gis-lab.info/data/osm_dump/dump/latest/BY.osm.bz2

$JAVA_EXEC org.alex73.osm.converters.bel.Convert || exit 1
rm osmand-tmp/belarus-latest.osm.bz2
bzip2 -1 osmand-tmp/*.osm || exit 1

$JAVA_EXEC -Xms1024M -Xmx3000M net.osmand.data.index.IndexBatchCreator batch.xml || exit 1

mv tmp/*.obf /var/www/osm/osmand/ || exit 1
rm -rf tmp/
