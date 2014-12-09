#!/bin/bash
set -x
cd `dirname $0`
PATH=../osmutils:$PATH
DIR=/data/tmp/osm-monitor

if [ -f $DIR/stop ]; then
  cat $DIR/stop
  exit 1
fi

rm -rf /data/tmp/osm-cache
rm $DIR/belarus-prev.o5m
mv $DIR/belarus-latest.o5m $DIR/belarus-prev.o5m
if [ $? -ne 0 ]; then
  echo "Impossible to move to prev" > $DIR/stop
  exit 1
fi

## Агульная мапа Беларусі на пачатак дня
rm $DIR/belarus-latest.osm.pbf
#wget -nv -O $TMP/belarus-latest.osm.pbf http://download.geofabrik.de/europe/belarus-latest.osm.pbf || exit 1
wget -nv -O $DIR/belarus-latest.osm.pbf http://be.gis-lab.info/data/osm_dump/dump/latest/BY.osm.pbf
if [ $? -ne 0 ]; then
  echo "Impossible to retrieve snapshot" > $DIR/stop
  exit 1
fi

nice osmconvert $DIR/belarus-latest.osm.pbf --out-o5m > $DIR/belarus-latest.o5m
if [ $? -ne 0 ]; then
  echo "Impossible to convert snapshot" > $DIR/stop
  exit 1
fi
rm $DIR/belarus-latest.osm.pbf

JAVA_CLASSPATH=$(find lib/ -name '*.jar' -printf '%p:')classes/
JAVA_EXEC="/opt/java8/bin/java $JAVA_OPTS -cp $JAVA_CLASSPATH"

time nice ionice -c3 $JAVA_EXEC org.alex73.osm.monitors.export.Export2Initial $DIR/belarus-prev.o5m $DIR/belarus-latest.o5m
if [ $? -ne 0 ]; then
  echo "Impossible generate diff" > $DIR/stop
  exit 1
fi
time nice ionice -c3 $JAVA_EXEC org.alex73.osm.monitors.export.Export2Initial $DIR/belarus-latest.o5m
if [ $? -ne 0 ]; then
  echo "Impossible generate dump" > $DIR/stop
  exit 1
fi

pushd ../../OsmBelarus-Monitoring/ || exit 1
git push
if [ $? -ne 0 ]; then
  echo "Impossible to push" > $DIR/stop
  exit 1
fi
