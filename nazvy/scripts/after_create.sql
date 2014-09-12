\set ON_ERROR_STOP on

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


CREATE OR REPLACE FUNCTION exist_key(hstore, text) RETURNS bool AS $$
  SELECT $1 ? $2; 
$$ LANGUAGE sql;

-- Злучае way межаў Беларусі і правярае на валіднасьць
CREATE OR REPLACE FUNCTION makeBelarus() RETURNS geometry AS $$
DECLARE
    BORDER GEOMETRY;
BEGIN
  SELECT ST_MakePolygon(ST_LineMerge(ST_Collect(w.linestring)))
    INTO BORDER
    FROM relation_members rm,ways w,relations r
   WHERE r.id=59065 AND rm.relation_id=r.id and rm.member_type='W' and rm.member_role='outer' and rm.member_id=w.id
   GROUP BY r.id,r.tags;

--SELECT makepolygonrel(id) as geom
--  INTO BORDER
--  FROM relations 
-- WHERE id=59065;

  IF ST_IsValid(BORDER) THEN
    RETURN BORDER;
  ELSE
    RAISE EXCEPTION 'Wrong border of Belarus !';
  END IF;
END;
$$ LANGUAGE plpgsql;

/*-- Вяртае палігон межаў Беларусі
CREATE OR REPLACE FUNCTION getBelarus() RETURNS geometry
AS 'SELECT geom FROM geo_Belarus;'
LANGUAGE SQL IMMUTABLE;*/

-- Правярае ці ёсьць кропкі з way у межах Беларусі
CREATE OR REPLACE FUNCTION isWayInsideBelarus(_way_id ways.id%TYPE) RETURNS BOOLEAN AS $$
BEGIN
  -- check outside
  RETURN EXISTS (
      SELECT 1
        FROM way_nodes wn, nodes_in_Belarus n
       WHERE wn.node_id=n.id
         AND wn.way_id = _way_id
     );
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Правярае ці кропка ў Беларусі па табліцы nodes_in_Belarus 
/*CREATE OR REPLACE FUNCTION isNodeInsideBelarus(_node_id nodes.id%TYPE) RETURNS BOOLEAN AS $$
BEGIN
  -- check outside
  RETURN EXISTS (
      SELECT 1
        FROM nodes_in_Belarus n
       WHERE n.id = _node_id
     );
END;
$$ LANGUAGE plpgsql IMMUTABLE;*/

-- стварае лінію з way
CREATE OR REPLACE FUNCTION makeline(_way_id ways.id%TYPE) RETURNS GEOMETRY AS $$
DECLARE
  geom GEOMETRY;
BEGIN
  SELECT w.linestring INTO geom FROM ways w WHERE w.id = _way_id;

  IF NOT isWayInsideBelarus(_way_id) THEN
    RETURN NULL;
  ELSEIF geom IS NULL THEN
    INSERT INTO WAYS_ERROR(id,error)
    VALUES(_way_id,'Пустая геамэтрыя way для line');
    RETURN NULL;
  ELSEIF ST_NPoints(ST_Simplify(geom,0.00001))<2 THEN
    INSERT INTO WAYS_ERROR(id,error)
    VALUES(_way_id,'Недастаткова кропак way для line');
    RETURN NULL;
  ELSEIF NOT ST_IsValid(geom) THEN
    INSERT INTO WAYS_ERROR(id,error)
    VALUES(_way_id,'Невалідная геамэтрыя way для line');
    RETURN NULL;
  ELSE
    RETURN geom;
  END IF;
EXCEPTION
  WHEN OTHERS THEN
    INSERT INTO WAYS_ERROR(id,error)
    VALUES(_way_id,'Невалідная геамэтрыя way #'||_way_id||': '||SQLSTATE||' '||SQLERRM);
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- стварае палігон з way
CREATE OR REPLACE FUNCTION makepolygonway(_way_id ways.id%TYPE) RETURNS GEOMETRY AS $$
DECLARE
  geom GEOMETRY;
BEGIN
  SELECT w.linestring INTO geom FROM ways w WHERE w.id = _way_id;

  IF NOT isWayInsideBelarus(_way_id) THEN
    RETURN NULL;
  ELSEIF geom IS NULL THEN
    INSERT INTO WAYS_ERROR(id,error)
    VALUES(_way_id,'Пустая геамэтрыя way для polygon');
    RETURN NULL;
  ELSEIF ST_NPoints(geom)<3 THEN
    INSERT INTO WAYS_ERROR(id,error)
    VALUES(_way_id,'Недастаткова кропак way для polygon');
    RETURN NULL;
  ELSEIF NOT ST_IsValid(geom) THEN
    INSERT INTO WAYS_ERROR(id,error)
    VALUES(_way_id,'Невалідная геамэтрыя way для polygon');
    RETURN NULL;
  ELSEIF NOT ST_IsClosed(geom) THEN
    INSERT INTO WAYS_ERROR(id,error)
    VALUES(_way_id,'IsClosed=false геамэтрыя way для polygon');
    RETURN NULL;
  ELSE
    geom = ST_MakePolygon(geom);
    IF NOT ST_IsValid(geom) THEN
      INSERT INTO WAYS_ERROR(id,error)
      VALUES(_way_id,'Невалідная геамэтрыя way для polygon');
      RETURN NULL;
    ELSE
      RETURN geom;
    END IF;
  END IF;
END;
$$ LANGUAGE plpgsql;

-- стварае палігон з relation
CREATE OR REPLACE FUNCTION makepolygonrel(_rel_id relations.id%TYPE) RETURNS GEOMETRY AS $$
DECLARE
  geom GEOMETRY;
  gouter GEOMETRY;
  ginner GEOMETRY;
BEGIN
  IF EXISTS (
      SELECT 1
        FROM relation_members rm
       WHERE rm.relation_id = _rel_id
         AND rm.member_role IN ('outer','inner')
         AND rm.member_type <> 'W'
       LIMIT 1
    ) THEN
      INSERT INTO RELATIONS_ERROR(id,error)
      VALUES(_rel_id,'Межы не way');
      RETURN NULL;
  END IF;

  IF NOT EXISTS ( -- па-за межамі Беларусі
      SELECT 1
        FROM relation_members rm
       WHERE rm.relation_id = _rel_id
         AND rm.member_role IN ('outer','inner', 'border')
         AND rm.member_type = 'W'
         AND isWayInsideBelarus(rm.member_id)
    ) THEN
      RETURN NULL;
  END IF;

  SELECT ST_MakePolygon(ST_LineMerge(ST_Collect(w.linestring)))
--ST_CollectionExtract(ST_BuildArea(ST_Collect(w.linestring)),3)
    INTO gouter
    FROM ways w,relation_members rm
   WHERE rm.relation_id = _rel_id
     AND rm.member_id = w.id
     AND rm.member_role IN ('outer','border')
     AND rm.member_type = 'W';

  SELECT ST_CollectionExtract(ST_Polygonize(w.linestring),3)
    INTO ginner
    FROM ways w,relation_members rm
   WHERE rm.relation_id = _rel_id
     AND rm.member_id = w.id
     AND rm.member_role = 'inner'
     AND rm.member_type = 'W';

  IF ginner IS NULL THEN
    geom = gouter;
  ELSE
    geom = ST_Difference(gouter, ginner);
  END IF;
  IF NOT ST_IsValid(geom) THEN
    INSERT INTO RELATIONS_ERROR(id,error)
    VALUES(_rel_id,'Невалідны polygon');
    RETURN NULL;
  ELSE
    RETURN geom;
  END IF;
EXCEPTION
  WHEN OTHERS THEN
    INSERT INTO RELATIONS_ERROR(id,error)
    VALUES(_rel_id,'Невалідная геамэтрыя rel #'||_rel_id||': '||SQLSTATE||' '||SQLERRM);
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- стварае палігон з relation па дамах
CREATE OR REPLACE FUNCTION makepolygonrelhouse(_rel_id relations.id%TYPE) RETURNS GEOMETRY AS $$
DECLARE
  _way_id int8;
BEGIN
  SELECT rm.member_id
    INTO _way_id
    FROM relation_members rm
   WHERE rm.relation_id = _rel_id
     AND rm.member_role = 'house'
     AND rm.member_type = 'W'
     AND isWayInsideBelarus(rm.member_id)
   LIMIT 1;
   
  IF _way_id IS NULL THEN
    RETURN NULL;
  ELSE
    RETURN makepolygonway(_way_id);
  END IF;
END;
$$ LANGUAGE plpgsql;

DROP TABLE IF EXISTS geo_belarus;
DROP TABLE IF EXISTS geo_regions;
DROP TABLE IF EXISTS nodes_in_Belarus;

-- Мяжа Беларусі
CREATE TABLE geo_belarus();
SELECT AddGeometryColumn('geo_belarus', 'geom', 4326, 'GEOMETRY', 2);

INSERT INTO geo_belarus(geom)
SELECT makeBelarus();

-- Вобласьці і раёны Беларусі
CREATE TABLE geo_regions(
  ID int8 NOT NULL
);
SELECT AddGeometryColumn('geo_regions', 'geom', 4326, 'GEOMETRY', 2);

--INSERT INTO geo_regions(ID,geom)
--SELECT id, makepolygonrel(id)
--  FROM relations 
-- WHERE id in (59065,59189,71116,59188,71107,71121,71105,71099,71118,71112,71097,71101,71124,71117,71100,71119,71094,71125,59506,70660,70668,70691,70684,59504,70676,70703,70716,70656,70665,70679,70690,70707,70671,70680,70662,70649,70669,70746,70701,70677,59161,59755,70801,59175,70795,1469691,59092,70810,59167,71149,70814,59149,71147,70799,70815,70809,71145,59174,70812,59137,70802,59178,59275,70721,70728,70790,70771,70737,59273,70768,70772,70732,70751,70747,70754,70749,71093,71114,70725,70748,59162,59753,59148,70586,70637,71138,70620,70618,70604,70580,70610,70577,70602,70638,70614,62147,70616,70589,70605,70591,70595,70626,59752,70569,70575,70565,70568,70563,71132,71130,70639,70542,71140,70566,59190,70719,71128,70549,71133,71134,69554,71135,70752,70561,59751)

-- кропкі ў Беларусі
CREATE TABLE nodes_in_Belarus(
  ID int8 NOT NULL
);

INSERT INTO nodes_in_Belarus(id)
SELECT n.id
  FROM nodes n, geo_Belarus b
 WHERE ST_Intersects(b.geom, n.geom);

CREATE UNIQUE INDEX pk_nodes_in_Belarus ON nodes_in_Belarus  (id);

