#!/bin/bash
set -x
cd `dirname $0`
PATH=../osmutils:$PATH
TMP=/data/tmp/osm

rm -rf $TMP osmupdate_temp/
mkdir -p $TMP

## Агульная мапа Беларусі на пачатак дня
wget -nv -O $TMP/belarus-latest.osm.pbf http://download.geofabrik.de/europe/belarus-latest.osm.pbf || exit 1
## Зьмены ад пачатку дня
nice osmupdate -v --keep-tempfiles $TMP/belarus-latest.osm.pbf $TMP/belarus-updated.o5m || exit 1
nice osmconvert $TMP/belarus-latest.osm.pbf --out-o5m > $TMP/belarus-latest.o5m || exit 1
