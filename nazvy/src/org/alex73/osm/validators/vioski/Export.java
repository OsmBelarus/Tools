/**************************************************************************
 Some tools for OSM.

 Copyright (C) 2014 Aleś Bułojčyk <alex73mail@gmail.com>
               Home page: http://www.omegat.org/
               Support center: http://groups.yahoo.com/group/OmegaT/

 This is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This software is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/

package org.alex73.osm.validators.vioski;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.alex73.osm.data.MemoryStorage;
import org.alex73.osm.data.NodeObject;
import org.alex73.osm.data.PbfDriver;
import org.alex73.osm.daviednik.Miesta;
import org.alex73.osm.utils.Env;
import org.alex73.osm.utils.TSV;
import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion;

/**
 * Стварае html+js для прагляду вёсак на мапе (http://latlon.org/~alex73/vioski/vioski.html).
 */
public class Export {

    public static void main(String[] args) throws Exception {
        Env.load();
        MemoryStorage osm = PbfDriver.process(new File("tmp/belarus-latest.osm.pbf"));

        String dav = Env.readProperty("dav");
        List<Miesta> daviednik = new TSV('\t').readCSV(
                dav, Miesta.class);

        Map<String, List<Mdav>> rajony = new TreeMap<>();
        for (Miesta m : daviednik) {
            List<Mdav> list = rajony.get(m.rajon);
            if (list == null) {
                list = new ArrayList<>();
                rajony.put(m.rajon, list);
            }
            Mdav mm = new Mdav();
            mm.osmID = m.osmID;
            mm.ss = m.sielsaviet;
            mm.why = m.osmComment;
            mm.nameBe = m.nazvaNoStress;
            mm.nameRu = m.ras;
            mm.varyjantBe = m.varyjantyBel;
            mm.varyjantRu = m.rasUsedAsOld;
            list.add(mm);
        }

        Map<String, List<Mosm>> map = new TreeMap<>();
        for (NodeObject n : osm.nodes) {
            String place = n.getTag("place");
            if (place == null || "island".equals(place) || "islet".equals(place)) {
                continue;
            }
            String rajon = n.getTag("addr:district");

            if (rajon != null && osm.isInsideBelarus(n)) {
                // List<M> list = rajony.get(n.rajon);
                List<Mosm> list = map.get(rajon);
                if (list == null) {
                    list = new ArrayList<>();
                    map.put(rajon, list);
                }
                Mosm mm = new Mosm();
                mm.osmID = n.id;
                mm.lat = n.lat;
                mm.lon = n.lon;
                mm.nameBe = n.getTag("name:be");
                mm.name = n.getTag("name");
                list.add(mm);
            }
        }

        String outDir = Env.readProperty("out.dir");
        File foutDir = new File(outDir+"/vioski");
        foutDir.mkdirs();

        List<PadzielOsmNas> padziel = new TSV('\t').readCSV("vioski/padziel.csv", PadzielOsmNas.class);
        Map<String, String> padzielo = new TreeMap<>();
        for(PadzielOsmNas p:padziel) {
            if (p.nasNameRajon!=null) {
                padzielo.put(p.nasNameRajon, osm.getRelationById(p.relationID).getTag("name"));
            }
        }

        ObjectMapper om = new ObjectMapper();
        String o = "var data={};\n";
        o += "data.dav=" + om.writeValueAsString(rajony) + "\n";
        o += "data.map=" + om.writeValueAsString(map) + "\n";
        o += "data.padziel=" + om.writeValueAsString(padzielo) + "\n";
        FileUtils.writeStringToFile(new File(outDir+"/vioski/data.js"), o);
		FileUtils.copyFileToDirectory(new File("vioski/control.js"), foutDir);
		FileUtils.copyFileToDirectory(new File("vioski/vioski.html"), foutDir);
    }

    @JsonSerialize(include = Inclusion.NON_NULL)
    public static class Mdav {
        public Long osmID;
        public String ss;
        public String why;
        public String nameBe, nameRu, varyjantBe, varyjantRu;
    }

    @JsonSerialize(include = Inclusion.NON_NULL)
    public static class Mosm {
        public Long osmID;
        public double lat, lon;
        public String nameBe, name;
    }
}
