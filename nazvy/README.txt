1. ant - кампіляваньне валідатараў
2. updatedb-daily.sh - каб выцягнуць усе зьвесткі і запісаць у базу
3. updatedb-hourly.sh - абнаўленьні раз на гадзіну
4. run2.sh - валідатары
5. org.alex73.osm.validators.vioski.Export, vioski - старонка для пошуку населеных пунктаў

####### Падрыхтоўка базы #######
Дадаць першым радком у /etc/posgres/.../pg_hba.conf:
    local osm osm trust
    host  osm osm 127.0.0.1/32 trust
## Ствараем карыстальніка osm і базу osm
sudo -u postgres createuser osm
sudo -u postgres createdb --encoding=UTF8 --owner=osm osm

## PostGIS для базы osm
sudo -u postgres psql --dbname=osm --file=/usr/share/postgresql/9.1/contrib/postgis-1.5/postgis.sql
sudo -u postgres psql --dbname=osm --file=/usr/share/postgresql/9.1/contrib/postgis-1.5/spatial_ref_sys.sql
sudo -u postgres psql --dbname=osm --command="ALTER TABLE geometry_columns OWNER TO osm"
sudo -u postgres psql --dbname=osm --command="ALTER TABLE spatial_ref_sys OWNER TO osm"
## hstore для базы osm
sudo -u postgres psql --dbname=osm --command="CREATE EXTENSION hstore"

## Павінна паказаць табліцы geometry_columns і spatial_ref_sys
sudo -u postgres psql --dbname=osm --command="\d"
## Павінна паказаць hstore
sudo -u postgres psql --dbname=osm --command="\dx"
