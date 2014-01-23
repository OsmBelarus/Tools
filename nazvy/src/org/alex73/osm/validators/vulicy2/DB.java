package org.alex73.osm.validators.vulicy2;

import java.util.List;

import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

public interface DB {
   /* @Select("SELECT * FROM osm_polygon WHERE place = 'city' AND osm_id=#{placeId}")
    @Results(value = {
            @Result(property = "id", column = "osm_id"), 
            @Result(property = "name", column = "name"),
            @Result(property = "name_be", column = "name:be")
            })
    OsmPlace getPlaceById(long placeId);
    
    @Select("SELECT * FROM osm_polygon WHERE place = 'city'")
    @Results(value = {
            @Result(property = "id", column = "osm_id"), 
            @Result(property = "name", column = "name"),
            @Result(property = "name_be", column = "name:be")
            })
    List<OsmPlace> getPlaces();
    
    @Select("SELECT r.* "
            + "FROM osm_line r, osm_polygon c "
            + "WHERE c.osm_id=#{cityId} AND ST_Intersects(r.way, c.way)"
            + "  AND r.highway IS NOT NULL"
            + "  AND r.osm_id > 0 "
            + "  AND r.highway NOT IN ('raceway','cycleway','path')"
            + "  AND r.int_ref IS NULL")
    @Results(value = {
            @Result(property = "id", column = "osm_id"), 
            @Result(property = "name", column = "name"),
            @Result(property = "name_be", column = "name:be"),
            @Result(property = "name_ru", column = "name:ru"),
            @Result(property = "int_name", column = "int_name"),
            })
    List<OsmNamed> getStreetsInCity(long cityId);
    
    @Select("SELECT r.* "
            + "FROM osm_polygon r, osm_polygon c "
            + "WHERE c.osm_id=#{cityId} AND ST_Intersects(r.way, c.way) AND r.\"addr:street\" IS NOT NULL")
    @Results(value = {
            @Result(property = "id", column = "osm_id"), 
            @Result(property = "name", column = "addr:street"),
            @Result(property = "name_be", column = "addr:street:be")
            })
    List<OsmNamed> getHousesInCity(long cityId);
    
    @Select("SELECT r.* "
            + "FROM osm_roads r, osm_polygon c "
            + "WHERE c.osm_id=#{cityId} AND ST_Intersects(r.way, c.way) AND r.tags->'type' = 'address'")
    @Results(value = {
            @Result(property = "id", column = "osm_id"), 
            @Result(property = "name", column = "name"),
            @Result(property = "name_be", column = "name:be"),
            @Result(property = "name_ru", column = "name:ru"),
            @Result(property = "int_name", column = "int_name"),
            })
    List<OsmNamed> getAddressesInCity(long cityId);*/
}
