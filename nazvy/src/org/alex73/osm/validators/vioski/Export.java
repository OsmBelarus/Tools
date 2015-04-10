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

import gen.alex73.osm.validators.rehijony.Rajon;
import gen.alex73.osm.validators.rehijony.Voblasc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.alex73.osm.utils.Belarus;
import org.alex73.osm.utils.CSV;
import org.alex73.osm.utils.Env;
import org.alex73.osm.validators.common.RehijonyLoad;
import org.alex73.osm.validators.harady.Miesta;
import org.alex73.osmemory.IOsmNode;
import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion;

/**
 * Стварае html+js для прагляду вёсак на мапе (http://latlon.org/~alex73/vioski/vioski.html).
 */
public class Export {
    static Belarus osm;
    static short placeTag;
    static Map<String, List<Mosm>> map = new TreeMap<>();

    public static void main(String[] args) throws Exception {
        RehijonyLoad.load(Env.readProperty("dav") + "/Rehijony.xml");

        osm = new Belarus();

        String dav = Env.readProperty("dav") + "/Nazvy_nasielenych_punktau.csv";
        List<Miesta> daviednik = new CSV('\t').readCSV(dav, Miesta.class);

        Map<String, List<Mdav>> rajony = new TreeMap<>();
        for (Miesta m : daviednik) {
            String r=m.rajon.startsWith("<")?m.rajon:m.rajon+" раён";
            List<Mdav> list = rajony.get(r);
            if (list == null) {
                list = new ArrayList<>();
                rajony.put(r, list);
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

        placeTag = osm.getTagsPack().getTagCode("place");

        osm.byTag("place", o -> o.isNode() && !o.getTag(placeTag).equals("island")
                && !o.getTag(placeTag).equals("islet"), o -> processNode((IOsmNode) o));

        String outDir = Env.readProperty("out.dir");
        File foutDir = new File(outDir + "/vioski");
        foutDir.mkdirs();

        Map<String, String> padzielo = new TreeMap<>();
        for(Voblasc v:RehijonyLoad.kraina.getVoblasc() ) {
            for(Rajon r:v.getRajon()) {
                padzielo.put(r.getNameBe(), osm.getObject(r.getOsmID()).getTag("name", osm));
            }
        }

        ObjectMapper om = new ObjectMapper();
        String o = "var data={};\n";
        o += "data.dav=" + om.writeValueAsString(rajony) + "\n";
        o += "data.map=" + om.writeValueAsString(map) + "\n";
        o += "data.padziel=" + om.writeValueAsString(padzielo) + "\n";
        FileUtils.writeStringToFile(new File(outDir + "/vioski/data.js"), o);
        FileUtils.copyFileToDirectory(new File("vioski/control.js"), foutDir);
        FileUtils.copyFileToDirectory(new File("vioski/vioski.html"), foutDir);
    }

    static void processNode(IOsmNode node) {
        String rajon = node.getTag("addr:district", osm);

        if (rajon != null && osm.contains(node)) {
            // List<M> list = rajony.get(n.rajon);
            List<Mosm> list = map.get(rajon);
            if (list == null) {
                list = new ArrayList<>();
                map.put(rajon, list);
            }
            Mosm mm = new Mosm();
            mm.osmID = node.getId();
            mm.lat = node.getLatitude();
            mm.lon = node.getLongitude();
            mm.nameBe = node.getTag("name:be", osm);
            mm.name = node.getTag("name", osm);
            list.add(mm);
        }
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
