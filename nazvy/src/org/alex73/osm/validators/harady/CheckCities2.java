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

package org.alex73.osm.validators.harady;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alex73.osm.daviednik.CalcCorrectTags;
import org.alex73.osm.daviednik.Miesta;
import org.alex73.osm.utils.Env;
import org.alex73.osm.utils.OSM;
import org.alex73.osm.utils.TSV;
import org.alex73.osm.utils.VelocityOutput;
import org.alex73.osm.validators.vulicy2.OsmNamed;
import org.alex73.osm.validators.vulicy2.OsmPlace;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

/**
 * Правярае супадзеньне назваў населеных пунктаў OSM назвам деведніка.
 */
public class CheckCities2 {
    static public class Result {
        // Неіснуючыя ў osm аб'екты ў выглядзе
        public List<String> nonExistInOsm = new ArrayList<>();

        public List<String> getNonExistInOsm() {
            return nonExistInOsm;
        }

        // Неўжытыя ў даведніку аб'екты
        public List<String> unusedInDav = new ArrayList<>();

        public List<String> getUnusedInDav() {
            return unusedInDav;
        }

        // Несупадзеньне тэгаў з назвамі у даведніку й аб'екце
        public ResultTable incorrectTags;

        public ResultTable getIncorrectTags() {
            return incorrectTags;
        }
    }

    static Result result = new Result();

    static SqlSession db;
    static List<Miesta> daviednik;
    static Set<String> usedInDav = new HashSet<>();
    static Map<String,OsmPlace> dbPlaces;
    static Map<String,String> adminLevelsBelToRus;

    public static void main(String[] args) throws Exception {
        Env.load();
        
        String out =Env.readProperty("out.dir")+"/nazvy.html";
        String dav = Env.readProperty("dav");

        System.out.println("Parsing csv from " + dav);
        daviednik = new TSV('\t').readCSV(dav, Miesta.class);

        initDB();
        
        System.out.println("Checking...");
        loadAdminLevels();
        findNonExistInOsm();
        findUnusedInDav();
        findIncorrectTags();

        System.out.println("Output to " + out + "...");
        Collections.sort(result.nonExistInOsm);
        Collections.sort(result.unusedInDav);
        result.incorrectTags.sort();
        new File(out).getParentFile().mkdirs();
        VelocityOutput.output("org/alex73/osm/validators/harady/validatar.velocity", out, "data", result, "OSM",
                OSM.class);
        System.out.println("done");
    }

    static void initDB() throws Exception {
        System.out.println("Load database...");
        String resource = "osm.xml";
        SqlSessionFactory sqlSessionFactory;
        InputStream inputStream = Resources.getResourceAsStream(resource);
        try {
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream, Env.env);
        } finally {
            inputStream.close();
        }
        db = sqlSessionFactory.openSession();
        
        dbPlaces = new HashMap<>();
        List<OsmPlace> list = db.selectList("osm.getPlaces");
        for (OsmPlace place : list) {
            dbPlaces.put(place.getCode(), place);
        }
    }

    static void loadAdminLevels() {
        adminLevelsBelToRus = new HashMap<>();
        List<OsmNamed> list = db.selectList("osm.admin_levels");
        for (OsmNamed place : list) {
            if (place != null && place.name_be != null && place.name != null) {
                if (place.name_be.endsWith(" раён") || place.name_be.endsWith(" вобласць")) {
                    adminLevelsBelToRus.put(place.name_be, place.name);
                }
            }
        }
    }

    static void findNonExistInOsm() {
        for (Miesta m : daviednik) {
            if (m.osmID != null) {
                if (!dbPlaces.containsKey("n"+m.osmID)) {
                    result.nonExistInOsm.add("Няма " + OSM.histText("n" + m.osmID) + " на мапе, але ёсць у " + m);
                } else {
                    if (!usedInDav.add("n" + m.osmID)) {
                        result.nonExistInOsm.add(OSM.histText("n" + m.osmID) + " выкарыстоўваецца двойчы ў даведніку: " + m);
                    }
                }
            }
            if (m.osmIDother != null && !m.osmIDother.trim().isEmpty()) {
                for (String id : m.osmIDother.split(";")) {
                    try {
                        if (!dbPlaces.containsKey(id)) {
                            result.nonExistInOsm.add("Няма " + OSM.histText(id) + " на мапе, але ёсць у " + m);
                        } else {
                            if (!usedInDav.add(id)) {
                                result.nonExistInOsm.add(OSM.histText(id) + " выкарыстоўваецца двойчы ў даведніку: " + m);
                            }
                        }
                    } catch (Exception ex) {
                        result.nonExistInOsm.add(ex.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Шукаем аб'екты што ёсьць ў даведніку але няма ў osm. Магчыма, былі
     * выдаленыя.
     */
    static void findUnusedInDav() {       
        for (OsmPlace p : dbPlaces.values()) {
            if ("island".equals(p.place) || "islet".equals(p.place)) {
                continue;
            }
                if (!usedInDav.contains(p.getCode())) {
                    result.unusedInDav.add(p.addr_region + "|" + p.addr_district + "|"
                            + p.name + "/" + OSM.browse(p.getCode()));
                }
        }
    }

    static void findIncorrectTags() {
        List<String> attrs = new ArrayList<>();
        addColumnIfAllowed("place", attrs);
        addColumnIfAllowed("name", attrs);
        addColumnIfAllowed("name:ru", attrs);
        addColumnIfAllowed("name:be", attrs);
        addColumnIfAllowed("int_name", attrs);
        addColumnIfAllowed("alt_name:be", attrs);
        addColumnIfAllowed("alt_name:ru", attrs);
        addColumnIfAllowed("name:be-tarask", attrs);
        addColumnIfAllowed("alt_name", attrs);
        addColumnIfAllowed("addr:country", attrs);
        addColumnIfAllowed("addr:region", attrs);
        addColumnIfAllowed("addr:district", attrs);
        result.incorrectTags = new ResultTable(attrs);

        for (Miesta m : daviednik) {
            for (final String code : getUsedCodes(m)) {
                // final WrongTags w = new WrongTags();
                ResultTable.ResultTableRow w = result.incorrectTags.new ResultTableRow(code, m.rajon + '|'
                        + m.sielsaviet + '|' + m.nazva);
                try {
                    OsmPlace p = dbPlaces.get(code);
                    OsmPlace correctTags = CalcCorrectTags.calc(m, db);
                    if ("suburb".equals(p.place) && "hamlet".equals(correctTags.place)) {
                        // hamlet => suburb - ok
                        correctTags.place = p.place;
                    }
                    if ("neighbourhood".equals(p.place) && "hamlet".equals(correctTags.place)) {
                        // hamlet => neighbourhood - ok
                        correctTags.place = p.place;
                    }
                    // правяраем тэгі
                    setAttrIfAllowed(w, "name", p.name, correctTags.name);
                    setAttrIfAllowed(w, "name:be", p.name_be, correctTags.name_be);
                    setAttrIfAllowed(w, "name:ru", p.name_ru, correctTags.name_ru);
                    setAttrIfAllowed(w, "int_name", p.int_name, correctTags.int_name);
                    setAttrIfAllowed(w, "name:be-tarask", p.name_be_tarask, correctTags.name_be_tarask);
                    if (correctTags.place != null) {
                        setAttrIfAllowed(w, "place", p.place, correctTags.place);
                    }
                    setAttrIfAllowed(w, "alt_name:be", p.alt_name_be, correctTags.alt_name_be);
                    setAttrIfAllowed(w, "alt_name:ru", p.alt_name_ru, correctTags.alt_name_ru);
                    setAttrIfAllowed(w, "alt_name", p.alt_name, null);

                    setAttrIfAllowed(w, "addr:country", p.addr_country, "BY");
                    setAttrIfAllowed(w, "addr:region", p.addr_region, adminLevelsBelToRus.get(m.voblasc + " вобласць"));
                    setAttrIfAllowed(w, "addr:district", p.addr_district, adminLevelsBelToRus.get(m.rajon + " раён"));
                } catch (Exception ex) {
                    result.nonExistInOsm.add(ex.getClass().getName() + ": " + ex.getMessage());
                }
                if (w.needChange()) {
                    result.incorrectTags.rows.add(w);
                }
            }
        }
    }

    static void addColumnIfAllowed(String attrName, List<String> list) {
        if (!"skip".equals(Env.readProperty("nazvy_viosak." + attrName.replace(':', '_')))) {
            list.add(attrName);
        }
    }

    static void setAttrIfAllowed(ResultTable.ResultTableRow w, String attrName, String oldValue, String newValue) {
        if (!"skip".equals(Env.readProperty("nazvy_viosak." + attrName.replace(':', '_')))) {
            w.setAttr(attrName, oldValue, newValue);
        }
    }

    static String add(String prev, String add) {
        if (prev == null) {
            return add;
        } else {
            return prev + "<br/>" + add;
        }
    }

    /**
     * Вяртае усе аб'екты osm што выкарыстаныя ў радку даведніка.
     */
    static List<String> getUsedCodes(Miesta m) {
        List<String> c = new ArrayList<>();
        if (m.osmID != null) {
            OsmPlace n = dbPlaces.get("n"+m.osmID);
            if (n != null) {
                c.add(n.getCode());
            }
        }
        if (m.osmIDother != null && !m.osmIDother.trim().isEmpty()) {
            for (String id : m.osmIDother.split(";")) {
                    OsmPlace o = dbPlaces.get(id);
                    if (o != null) {
                        c.add(o.getCode());
                    }
            }
        }
        return c;
    }
}
