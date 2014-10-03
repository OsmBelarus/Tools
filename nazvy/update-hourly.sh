#!/bin/bash
set -x
cd `dirname $0`
PATH=../osmutils:$PATH
TMP=/data/tmp/osm

## Зьмены ад пачатку дня
nice osmupdate -v --keep-tempfiles $TMP/belarus-latest.osm.pbf $TMP/belarus-updated.o5m || exit 1
