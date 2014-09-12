#!/bin/bash
set -x
cd `dirname $0`
PATH=../osmutils:$PATH

#rm -rf tmp/ osmupdate_temp/
#mkdir -p tmp/

## Агульная мапа Беларусі на пачатак дня
#wget -nv -O tmp/belarus-latest.osm.pbf http://download.geofabrik.de/europe/belarus-latest.osm.pbf || exit 1
## Зьмены ад пачатку дня
osmupdate -v --keep-tempfiles --hour tmp/belarus-latest.osm.pbf tmp/belarus-updated.osm.pbf || exit 1
