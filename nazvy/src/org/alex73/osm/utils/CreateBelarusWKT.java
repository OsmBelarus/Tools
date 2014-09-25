package org.alex73.osm.utils;

import java.io.File;

import org.alex73.osmemory.MemoryStorage;
import org.alex73.osmemory.O5MReader;
import org.alex73.osmemory.geometry.Area;
import org.apache.commons.io.FileUtils;

/**
 * Стварае WKT для Беларусі (relation 79842)
 */
public class CreateBelarusWKT {

    public static void main(String[] args) throws Exception {
        MemoryStorage osm = new O5MReader().read(new File(Env.readProperty("data.file")));
        osm.showStat();

        Area a = Area.fromOSM(osm, osm.getRelationById(79842));

        FileUtils.writeStringToFile(new File("Belarus-79842.wkt"), a.getGeometry().toText());
    }
}
