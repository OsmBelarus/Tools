#!/bin/bash
set -x
cd `dirname $0`
PATH=../osmutils:$PATH

rm -rf tmp
mkdir -p tmp/

## Агульная мапа Беларусі на пачатак дня
wget -nv -O tmp/belarus-latest.osm.pbf http://download.geofabrik.de/europe/belarus-latest.osm.pbf || exit 1
## Зьмены ад пачатку дня
osmupdate --hour --keep-tempfiles `date +%Y-%m-%d`T00:00:00Z tmp/day.osc.gz

## Выдаленьне ўсіх зьвестак з базы - стварэньне схемы наноў
time psql --dbname=osm --username=osm --file=../osmosis/script/pgsnapshot_schema_0.6.sql
## Дадаем геамэтрыю для ways
time psql --dbname=osm --username=osm --file=../osmosis/script/pgsnapshot_schema_0.6_linestring.sql

## Канвэртуем pbf у базу. Фільтраваць па палігоне краіны нельга, бо выкідае некаторыя кропкі !
time ../osmosis/bin/osmosis --read-pbf file=tmp/belarus-latest.osm.pbf --write-pgsql host=localhost database=osm user=osm || exit 1
time ../osmosis/bin/osmosis --read-xml-change file=tmp/day.osc.gz --write-pgsql-change host=localhost database=osm user=osm || exit 1

time psql --dbname=osm --user osm --file=scripts/after_create.sql || exit 1
time psql --dbname=osm --user osm --file=scripts/after_update.sql || exit 1
