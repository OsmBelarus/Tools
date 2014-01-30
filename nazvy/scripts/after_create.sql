\set ON_ERROR_STOP on

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
  ELSEIF ST_NPoints(geom)<2 THEN
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

  SELECT ST_CollectionExtract(ST_Polygonize(w.linestring),3)
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