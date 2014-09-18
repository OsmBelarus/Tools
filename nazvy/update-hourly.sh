#!/bin/bash
set -x
cd `dirname $0`
PATH=../osmutils:$PATH

## Зьмены ад пачатку дня
osmupdate -v --keep-tempfiles tmp/belarus-latest.osm.pbf tmp/belarus-updated.o5m || exit 1
