\set ON_ERROR_STOP on

DROP TABLE IF EXISTS NODES_ERROR;
DROP TABLE IF EXISTS WAYS_ERROR;
DROP TABLE IF EXISTS RELATIONS_ERROR;

DROP TABLE IF EXISTS nodes_in_Belarus;
DROP TABLE IF EXISTS geo_belarus;
DROP TABLE IF EXISTS geo_roads;
DROP TABLE IF EXISTS geo_houses;
DROP TABLE IF EXISTS geo_cities;
DROP TABLE IF EXISTS geo_address_streets;

CREATE TABLE NODES_ERROR (
  ID int8 NOT NULL,
  ERROR VARCHAR(1024)
);
CREATE INDEX pk_nodes_error ON NODES_ERROR(ID);
CREATE TABLE WAYS_ERROR (
  ID int8 NOT NULL,
  ERROR VARCHAR(1024)
);
CREATE INDEX pk_ways_error ON WAYS_ERROR(ID);
CREATE TABLE RELATIONS_ERROR (
  ID int8 NOT NULL,
  ERROR VARCHAR(1024)
);
CREATE INDEX pk_relations_error ON RELATIONS_ERROR(ID);

-- Мяжа Беларусі
CREATE TABLE geo_belarus();
SELECT AddGeometryColumn('geo_belarus', 'geom', 4326, 'GEOMETRY', 2);

INSERT INTO geo_belarus(geom)
SELECT makeBelarus();

-- кропкі ў Беларусі
CREATE TABLE nodes_in_Belarus(
  ID int8 NOT NULL
);

INSERT INTO nodes_in_Belarus(id)
SELECT n.id
  FROM nodes n, geo_Belarus b
 WHERE ST_Intersects(b.geom, n.geom);

CREATE UNIQUE INDEX pk_nodes_in_Belarus ON nodes_in_Belarus  (id);

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
