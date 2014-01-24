SELECT GET_BORDER();

-- ствараем табліцы геамэтрыі наноў
--DROP TABLE IF EXISTS nodes_geom;
--DROP TABLE IF EXISTS ways_geom;
--DROP TABLE IF EXISTS relations_geom;
DROP TABLE IF EXISTS NODES_ERROR;
DROP TABLE IF EXISTS WAYS_ERROR;
DROP TABLE IF EXISTS RELATIONS_ERROR;

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

CREATE TABLE NODES_GEOM (
  ID int8 NOT NULL PRIMARY KEY,
  TYP VARCHAR(128),
  TAGS hstore NOT NULL
);
SELECT AddGeometryColumn('nodes_geom', 'geom', 4326, 'GEOMETRY', 2);
CREATE TABLE WAYS_GEOM (
  ID int8 NOT NULL PRIMARY KEY,
  TYP VARCHAR(128),
  TAGS hstore NOT NULL
);
SELECT AddGeometryColumn('ways_geom', 'geom', 4326, 'GEOMETRY', 2);
CREATE TABLE RELATIONS_GEOM (
  ID int8 NOT NULL PRIMARY KEY,
  TYP VARCHAR(128),
  TAGS hstore NOT NULL
);
SELECT AddGeometryColumn('relations_geom', 'geom', 4326, 'GEOMETRY', 2);

-- Памылкі
--INSERT INTO WAYS_ERROR
--SELECT id,'Ёсьць тэгі building і highway адначасова'
--  FROM ways
-- WHERE exist_key(tags, 'building') and exist_key(tags, 'highway');

--INSERT INTO WAYS_ERROR
--SELECT id,'Ёсьць тэгі building і place адначасова'
--  FROM ways
-- WHERE exist_key(tags, 'building') and exist_key(tags, 'place');

--INSERT INTO RELATIONS_ERROR
--SELECT id,'Ёсьць тэгі place і type=address адначасова'
--  FROM relations
-- WHERE exist_key(tags, 'place') and tags->'type'='address';

--INSERT INTO RELATIONS_ERROR
--SELECT DISTINCT relation_id,'Няма ролі ў нейкай частцы relation'
--  FROM relation_members
-- WHERE member_role='';

-- Усе кропкі з тэгамі
--INSERT INTO nodes_geom(id,tags,typ,geom)
--SELECT n.id,n.tags,'Кропка з тэгамі',n.geom
--  FROM nodes n
-- WHERE tags<>hstore(ROW());

-- Мяжа Беларусі
INSERT INTO relations_geom(id,tags,typ,geom)
SELECT r.id,r.tags,'Мяжа Беларусі',ST_MakePolygon(ST_LineMerge(ST_Collect(w.linestring)))
  FROM relation_members rm,ways w,relations r
 WHERE r.id=59065 AND rm.relation_id=r.id and rm.member_type='W' and rm.member_role='outer' and rm.member_id=w.id
 GROUP BY r.id,r.tags;

-- Вуліцы
INSERT INTO ways_geom(id,tags,typ,geom)
SELECT id,tags,'Вуліца ці дарога',linestring
  FROM ways
 WHERE exist_key(tags, 'highway') AND ST_NPoints(linestring)>1 AND id NOT IN (SELECT id FROM ways_error);
-- Дамы
INSERT INTO ways_geom(id,tags,typ,geom)
SELECT id,tags,'Будынак',ST_MakePolygon(linestring)
  FROM ways
 WHERE exist_key(tags, 'building') AND ST_IsClosed(linestring) AND ST_NPoints(linestring)>3 AND id NOT IN (SELECT id FROM ways_error);
-- Адрасы
INSERT INTO relations_geom(id,tags,typ,geom)
SELECT r.id,r.tags,'Адрасы',ST_CollectionExtract(ST_Polygonize(w.linestring),3)
  FROM relation_members rm,ways w,relations r
 WHERE rm.relation_id=r.id and 
       rm.member_type='W' and rm.member_id=w.id AND
       ST_NPoints(w.linestring)>2 AND
       r.tags->'type' = 'address' AND
       r.id NOT IN (SELECT id FROM relations_error)
 GROUP BY r.id,r.tags;

-- Межы гарадоў: ways
INSERT INTO ways_geom(id,tags,typ,geom)
SELECT id,tags,'Мяжа горада',ST_MakePolygon(linestring)
  FROM ways
 WHERE exist_key(tags, 'place') AND 
       ST_IsClosed(linestring) AND 
       ST_NPoints(linestring)>2 AND 
       id NOT IN (SELECT id FROM ways_error);

-- Межы гарадоў: relations
INSERT INTO relations_geom(id,tags,typ,geom)
SELECT r.id,r.tags,'Мяжа горада',ST_CollectionExtract(ST_Polygonize(w.linestring),3)
  FROM relation_members rm,ways w,relations r
 WHERE rm.relation_id=r.id and 
       rm.member_type='W' and 
       rm.member_role='outer' and rm.member_id=w.id AND
       ST_NPoints(w.linestring)>1 AND
       exist_key(r.tags, 'place') AND 
       NOT EXISTS (SELECT 1 FROM relation_members rm WHERE rm.relation_id=r.id AND member_role='border') AND
       r.id NOT IN (SELECT id FROM relations_error)
 GROUP BY r.id,r.tags;

INSERT INTO relations_geom(id,tags,typ,geom)
SELECT r.id,r.tags,'Мяжа горада',ST_CollectionExtract(ST_Polygonize(w.linestring),3)
  FROM relation_members rm,ways w,relations r
 WHERE rm.relation_id=r.id and 
       rm.member_type='W' and 
       rm.member_role='border' and 
       rm.member_id=w.id AND
       exist_key(r.tags, 'place') AND 
       EXISTS (SELECT 1 FROM relation_members rm WHERE rm.relation_id=r.id AND member_role='border' and member_type='W') AND
       r.id NOT IN (SELECT id FROM relations_error)
 GROUP BY r.id,r.tags;

INSERT INTO relations_geom(id,tags,typ,geom)
SELECT r.id,r.tags,'Мяжа горада',w.geom
  FROM relations r,relation_members rm,relations_geom w
 WHERE rm.relation_id=r.id and 
       rm.member_type='R' and 
       rm.member_role='border' and 
       rm.member_id=w.id and 
       exist_key(r.tags, 'place') AND 
       EXISTS (SELECT 1 FROM relation_members rm WHERE rm.relation_id=r.id AND member_role='border' and member_type='R') AND
       r.id NOT IN (SELECT id FROM relations_error);

CREATE INDEX idx_geom_ways ON ways_geom USING gist(geom);
CREATE INDEX idx_geom_relations ON relations_geom USING gist(geom);

CREATE INDEX idx_hstore_nodes ON nodes_geom USING gist(tags);
CREATE INDEX idx_hstore_ways ON ways_geom USING gist(tags);
CREATE INDEX idx_hstore_relationss ON relations_geom USING gist(tags);
