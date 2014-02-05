#!/bin/sh
set -x
cd `dirname $0`

JAVA_CLASSPATH=OsmAndMapCreator.jar:$(find ../nazvy/lib/ -name '*.jar' -printf '%p:')../nazvy/classes/
JAVA_EXEC="java $JAVA_OPTS -cp $JAVA_CLASSPATH"

rm -rf tmp
mkdir tmp
wget -q -O tmp/belarus-latest.osm.bz2 http://be.gis-lab.info/data/osm_dump/dump/latest/BY.osm.bz2

$JAVA_EXEC org.alex73.osm.converters.bel.Convert || exit 1
rm tmp/belarus-latest.osm.bz2
bzip2 -1 tmp/*.osm || exit 1

JAVA_CLASSPATH=OsmAndMapCreator.jar:$(find lib/ -name '*.jar' -printf '%p:')../nazvy/classes/:../nazvy/lib/commons-compress-1.5.jar
JAVA_EXEC="java $JAVA_OPTS -cp $JAVA_CLASSPATH"

$JAVA_EXEC -Xms1024M -Xmx3000M net.osmand.data.index.IndexBatchCreator batch.xml || exit 1

mv tmp/*.obf /var/www/osm/osmand/ || exit 1
rm -rf tmp/
