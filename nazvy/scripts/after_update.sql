\set ON_ERROR_STOP on

DROP TABLE IF EXISTS geo_roads;
DROP TABLE IF EXISTS geo_houses;
DROP TABLE IF EXISTS geo_cities;
DROP TABLE IF EXISTS geo_address_streets;

DELETE FROM NODES_ERROR;
DELETE FROM WAYS_ERROR;
DELETE FROM RELATIONS_ERROR;

-- Абнаўляем сёньняшнія кропкі
DELETE 
  FROM nodes_in_Belarus i
 WHERE EXISTS (SELECT 1 FROM nodes n WHERE i.id=n.id AND n.tstamp > (current_date - interval '1 day'));

DELETE 
  FROM nodes_in_Belarus i
 WHERE NOT EXISTS (SELECT 1 FROM nodes n WHERE i.id=n.id);

INSERT INTO nodes_in_Belarus(id)
SELECT i.id
  FROM (
	SELECT n.id, n.geom
	  FROM nodes n
	 WHERE n.tstamp > (current_date - interval '1 day')
       ) i, geo_Belarus b
 WHERE ST_Intersects(b.geom, i.geom);

-- Вуліцы і дарогі
CREATE TABLE geo_roads(
  ID int8 NOT NULL PRIMARY KEY,
  tags hstore
);
SELECT AddGeometryColumn('geo_roads', 'geom', 4326, 'GEOMETRY', 2);

INSERT INTO geo_roads(id,tags,geom)
SELECT id,tags,makeline(id)
  FROM ways w
 WHERE exist_key(tags, 'highway');

-- Дамы
CREATE TABLE geo_houses(
  ID int8 NOT NULL PRIMARY KEY,
  tags hstore
);
SELECT AddGeometryColumn('geo_houses', 'geom', 4326, 'GEOMETRY', 2);

INSERT INTO geo_houses(id,tags,geom)
SELECT id,tags,makepolygonway(id)
  FROM ways w
 WHERE exist_key(tags, 'building');

-- Адрасы
CREATE TABLE geo_address_streets(
  ID int8 NOT NULL PRIMARY KEY,
  tags hstore
);
SELECT AddGeometryColumn('geo_address_streets', 'geom', 4326, 'GEOMETRY', 2);

INSERT INTO geo_address_streets(id,tags,geom)
SELECT r.id,r.tags,makepolygonrelhouse(id)
  FROM relations r
 WHERE r.tags->'type' = 'address' AND
       r.tags->'address:type' = 'a6';

-- Межы гарадоў: ways
CREATE TABLE geo_cities(
  TYPE VARCHAR(16) NOT NULL,
  ID int8 NOT NULL,
  tags hstore,
  PRIMARY KEY (TYPE,ID)
);
SELECT AddGeometryColumn('geo_cities', 'geom', 4326, 'GEOMETRY', 2);

INSERT INTO geo_cities(id,type,tags,geom)
SELECT n.id,'NODE',n.tags,n.geom
  FROM nodes n, nodes_in_Belarus nb
 WHERE exist_key(tags, 'place') 
   AND n.id = nb.id;

INSERT INTO geo_cities(id,type,tags,geom)
SELECT id,'WAY',tags,makepolygonway(id)
  FROM ways
 WHERE exist_key(tags, 'place');

INSERT INTO geo_cities(id,type,tags,geom) -- спачатку самі межы
SELECT id,'RELATION',tags,makepolygonrel(id)
  FROM relations r
 WHERE exist_key(tags, 'place') AND NOT EXISTS (
          SELECT 1
            FROM relation_members rm
           WHERE rm.relation_id = r.id
             AND rm.member_role = 'border'
             AND rm.member_type = 'R'
        );

INSERT INTO geo_cities(id,type,tags,geom)
SELECT r.id,'RELATION',r.tags,rb.geom
  FROM relations r, 
       relation_members rm LEFT OUTER JOIN geo_cities rb ON rm.member_id = rb.id AND rb.type = 'RELATION'
 WHERE exist_key(r.tags, 'place')
   AND rm.relation_id = r.id
   AND rm.member_role = 'border'
   AND rm.member_type = 'R';
