package org.alex73.osm.utils;

import java.io.File;

import org.alex73.osm.data.MemoryStorage;
import org.alex73.osm.data.PbfDriver;
import org.apache.commons.io.FileUtils;

import com.vividsolutions.jts.awt.ShapeReader;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;

/**
 * Стварае WKT для Беларусі (relation 79842)
 */
public class CreateBelarusWKT {

    public static void main(String[] args) throws Exception {
        GeometryFactory GEOM = new GeometryFactory(new PrecisionModel(1000000000));
        MemoryStorage osm = PbfDriver.process(new File("tmp/belarus-latest.osm.pbf"));
        Geometry g = new ShapeReader(GEOM).read(osm.Belarus.getPathIterator(null));
        FileUtils.writeStringToFile(new File("Belarus-79842.wkt"), g.toText());
    }
}
