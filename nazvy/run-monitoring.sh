#!/bin/bash
set -x
cd `dirname $0`

JAVA_CLASSPATH=$(find lib/ -name '*.jar' -printf '%p:')classes/
JAVA_EXEC="java $JAVA_OPTS -cp $JAVA_CLASSPATH"

pushd ../../OsmBelarus-Monitoring/ || exit 1
git reset --hard || exit 1
git clean -df || exit 1
git pull || exit 1
rm *.txt
popd

time nice $JAVA_EXEC org.alex73.osm.monitors.export.ExportObjects || exit 1

pushd ../../OsmBelarus-Monitoring/ || exit 1
git add *.txt
echo Зьмены на `date '+%d.%m.%Y %H:%M'` | git commit --file=-
git push
popd
