package org.alex73.osm.validators;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.alex73.osm.data.MemoryStorage;
import org.alex73.osm.data.NodeObject;
import org.alex73.osm.data.PbfDriver;
import org.alex73.osm.daviednik.Miesta;
import org.alex73.osm.utils.TSV;
import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion;

public class Export {

    public static void main(String[] args) throws Exception {
        MemoryStorage osm = PbfDriver.process(new File("data-orig/belarus-latest.osm.pbf"));

        List<Miesta> daviednik = new TSV('\t').readCSV(
                "/home/alex/gits/OsmBelarus-Databases/Nazvy_nasielenych_punktau/list.csv", Miesta.class);

        Map<String, List<M>> rajony = new TreeMap<>();
        for (Miesta m : daviednik) {
            List<M> list = rajony.get(m.rajon);
            if (list == null) {
                list = new ArrayList<>();
                rajony.put(m.rajon, list);
            }
            M mm = new M();
            mm.osmID = m.osmID;
            mm.nameBe = m.nazvaNoStress;
            mm.nameRu = m.ras;
            mm.varyjantBe = m.varyjantyBel;
            mm.varyjantRu = m.rasUsedAsOld;
            list.add(mm);
        }

        Map<String, List<M>> map = new TreeMap<>();
        for (NodeObject n : osm.nodes) {
            String place = n.getTag("place");
            if (place == null || "island".equals(place) || "islet".equals(place)) {
                continue;
            }
            String rajon=n.getTag("addr:district");
            if (osm.isInsideBelarus(n)) {
                //List<M> list = rajony.get(n.rajon);
            }
        }

        ObjectMapper om = new ObjectMapper();
        String o = "var rajony=" + om.writeValueAsString(rajony) + "\n";
        o += "var map=" + om.writeValueAsString(rajony) + "\n";
        FileUtils.writeStringToFile(new File("html/rajony.js"), o);
    }

    @JsonSerialize(include = Inclusion.NON_NULL)
    public static class M {
        public Long osmID;
        public String nameBe, nameRu, varyjantBe, varyjantRu;
    }
}
