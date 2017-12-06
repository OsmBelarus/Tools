package org.alex73.osm.validators.vioski;

import java.util.ArrayList;
import java.util.List;

import org.alex73.osm.utils.Belarus;
import org.alex73.osm.utils.CSV;
import org.alex73.osm.validators.common.RehijonyLoad;
import org.alex73.osm.validators.harady.Miesta;
import org.alex73.osm.validators.harady.MiestaOld;
import org.alex73.osmemory.IOsmNode;

/**
 * Абнаўляе каардынаты цэнтраў населеных пунктаў з мапы.
 */
public class UpdateCoord {
    public static void main(String[] args) throws Exception {
        RehijonyLoad.load("/data/gits/OsmBelarus-Databases/Daviedniki/Rehijony.xml");

        Belarus osm = new Belarus("/tmp/b.o5m");

        String dav = "/data/gits/OsmBelarus-Databases/Daviedniki/Nazvy_nasielenych_punktau.csv";
        List<MiestaOld> daviednik = new CSV('\t').readCSV(dav, MiestaOld.class);
        List<Miesta> daviednik2 = new ArrayList<>();
        for (MiestaOld m : daviednik) {
            Miesta m2 = m.cloneObject();
            if (m.osmID != null) {
                IOsmNode n = osm.getNodeById(m.osmID);
                if (n == null) {
                    System.out.println(m.osmID);
                } else {
                    m2.lat = n.getLat();
                    m2.lon = n.getLon();
                    if (m2.lat < 510000000 || m2.lat > 570000000) {
                        throw new Exception(m2.lat + "");
                    }
                    if (m2.lon < 230000000 || m2.lon > 330000000) {
                        throw new Exception(m2.lon + "");
                    }
                }
            }
            daviednik2.add(m2);
        }

        new CSV('\t').saveCSV("/data/gits/OsmBelarus-Databases/Daviedniki/Nazvy_nasielenych_punktau2.csv", Miesta.class,
                daviednik2);
    }
}
