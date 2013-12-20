/**************************************************************************
 Some tools for OSM.

 Copyright (C) 2013 Aleś Bułojčyk <alex73mail@gmail.com>
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
package org.alex73.osm.validators.vulicy;

import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.Collator;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.alex73.osm.data.BaseObject;
import org.alex73.osm.data.MemoryStorage;
import org.alex73.osm.data.PbfDriver;
import org.alex73.osm.data.RelationObject;
import org.alex73.osm.data.WayObject;
import org.alex73.osm.daviednik.Miesta;
import org.alex73.osm.utils.Geo;
import org.alex73.osm.utils.Lat;
import org.alex73.osm.utils.OSM;
import org.alex73.osm.utils.POReader;
import org.alex73.osm.utils.TMX;
import org.alex73.osm.utils.TSV;
import org.apache.commons.lang.StringUtils;

/**
 * Стварае файлы .po для перакладу назваў вуліц.
 */
public class StreetsParse {
    public static Locale BE = new Locale("be");
    public static Collator BEL = Collator.getInstance(BE);

    static String poOutputDir;
    static String tmxOutputDir;
    static String davFile;

    MemoryStorage osm;
    List<Miesta> daviednik;
    List<City> cities = new ArrayList<>();
    List<StreetNames> resultStreets = new ArrayList<>();
    List<StreetNames> resultHouses = new ArrayList<>();
    List<StreetNames> resultRelations = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        String pbfFile = null;
        for (String a : args) {
            if (a.startsWith("--pbf=")) {
                pbfFile = a.substring(6).replace("$HOME", System.getProperty("user.home"));
            } else if (a.startsWith("--dav=")) {
                davFile = a.substring(6).replace("$HOME", System.getProperty("user.home"));
            } else if (a.startsWith("--po-out-dir=")) {
                poOutputDir = a.substring(13).replace("$HOME", System.getProperty("user.home"));
            } else if (a.startsWith("--tmx-out-dir=")) {
                tmxOutputDir = a.substring(14).replace("$HOME", System.getProperty("user.home"));
            }
        }
        if (pbfFile == null || davFile == null || poOutputDir == null || tmxOutputDir == null) {
            System.err
                    .println("StreetsParse --pbf=tmp/belarus-latest.osm.pbf --dav=tmp/list.csv --po-out-dir=../strstr/source/ --tmx-out-dir=../strstr/tm/");
            System.exit(1);
        }

        StreetsParse s = new StreetsParse();
        s.run(pbfFile);
    }

    void run(String pbfFile) throws Exception {
        System.out.println("Parsing pbf from " + pbfFile);
        osm = PbfDriver.process(new File(pbfFile));

        System.out.println("Parsing csv from " + davFile);
        daviednik = new TSV('\t').readCSV(davFile, Miesta.class);

        System.out.println("Checking...");
        init();

        for (WayObject w : osm.ways) {
            StreetNames n = processWayStreets(w);
            if (n != null && n.needToChange()) {
                resultStreets.add(n);
            }
        }
        for (WayObject w : osm.ways) {
            StreetNames n = processWayHouses(w);
            if (n != null && n.needToChange()) {
                resultHouses.add(n);
            }
        }
        for (RelationObject r : osm.relations) {
            StreetNames n = processRelation(r);
            if (n != null && n.needToChange()) {
                resultRelations.add(n);
            }
        }

        end();
    }

    public void init() throws Exception {
        for (Miesta m : daviednik) {
            Area border = null;
            switch (m.typ) {
            case "г.":
                if (m.osmIDother != null && !m.osmIDother.trim().isEmpty()) {
                    for (String id : m.osmIDother.split(";")) {
                        try {
                            BaseObject o = osm.getObject(id);
                            if (o.getType() == BaseObject.TYPE.WAY) {
                                border = Geo.way2area(osm, o.id);
                            } else if (o.getType() == BaseObject.TYPE.RELATION) {
                                border = Geo.rel2area(osm, o.id);
                            }
                        } catch (Exception ex) {
                        }
                        if (border != null) {
                            break;
                        }
                    }
                }
                break;
            }
            if (border != null) {
                cities.add(new City(m, border));
            }
        }
    }

    void end() throws Exception {
        for (City c : cities) {
            List<String> us = new ArrayList<>(c.uniq.keySet());
            Collections.sort(us, new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    return BEL.compare(o1.toLowerCase(BE), o2.toLowerCase(BE));
                }
            });

            TMX tmx = new TMX();
            File poFile = new File(poOutputDir + '/' + c.fn + ".po");
            File tmxFile = new File(tmxOutputDir + '/' + c.fn + ".tmx");
            poFile.getParentFile().mkdirs();
            tmxFile.getParentFile().mkdirs();
            BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(poFile)));
            for (String u : us) {
                LocalizationInfo li = c.uniq.get(u);
                wr.write("# Objects : " + li.ways + "\n");
                wr.write("# Names: " + li.name + "\n");
                wr.write("msgid \"" + u + "\"\n");
                String t = "";
                if (!li.name_be.isEmpty()) {
                    wr.write("msgstr \"\"\n");

                    for (String s : li.name_be) {
                        t += "#" + s;
                    }
                }
                wr.write("\n");
                if (!t.isEmpty()) {
                    tmx.put(u, t);
                }
            }
            wr.close();
            tmx.save(tmxFile);
            System.out.println(c.nazva + ": " + c.uniq.size());
        }
    }

    public StreetNames processWayStreets(WayObject way) {
        if (way.getTag("highway") != null) {
            Path2D wayPath = Geo.way2path(osm, way.id);
            for (City c : cities) {
                if (Geo.isInside(c.border, wayPath)) {
                    return processTags(c, way, "name", "name:ru", "name:be",
                            System.getProperty("disableIntName") == null ? "int_name" : null);
                }
            }
        }
        return null;
    }

    public StreetNames processWayHouses(WayObject way) {
        if (System.getProperty("disableAddrStreet") == null && way.getTag("addr:street") != null) {
            for (City c : cities) {
                Path2D wayPath = Geo.way2path(osm, way.id);
                if (Geo.isInside(c.border, wayPath)) {
                    return processTags(c, way, "addr:street", null,
                            System.getProperty("disableAddressStreetBe") == null ? "addr:street:be" : null, null);
                }
            }
        }
        return null;
    }

    public StreetNames processRelation(RelationObject relation) {
        if ("address".equals(relation.getTag("type"))) {
            Area relArea;
            try {
                relArea = Geo.rel2area(osm, relation.id);
            } catch (Exception ex) {
                return null;
                // TODO wrong role in rel
            }
            for (City c : cities) {
                boolean inside;
                inside = Geo.isInside(c.border, relArea);
                if (inside) {
                    return processTags(c, relation, "name", "name:ru", "name:be",
                            System.getProperty("disableIntName") == null ? "int_name" : null);
                }
            }
        }
        return null;
    }

    public static class Names {
        public String name, name_be, name_ru, int_name;
    }

    public static class StreetNames {
        public City c;
        public String objCode;
        public Names tags = new Names();
        public Names exist = new Names();
        public Names required = new Names();
        public String error;

        public String getLink() {
            return OSM.histIcon(objCode);
        }

        public boolean needToChange() {
            if (error != null) {
                return true;
            }
            return !StringUtils.equals(exist.name, required.name)
                    || !StringUtils.equals(exist.name_be, required.name_be)
                    || !StringUtils.equals(exist.name_ru, required.name_ru)
                    || !StringUtils.equals(exist.int_name, required.int_name);
        }
    }

    StreetNames processTags(City c, BaseObject obj, String nameTag, String nameRuTag, String nameBeTag,
            String nameIntlTag) {

        StreetNames names = new StreetNames();
        names.c = c;
        names.tags.name = nameTag;
        names.tags.name_be = nameBeTag;
        names.tags.name_ru = nameRuTag;
        names.tags.int_name = nameIntlTag;

        names.objCode = obj.getCode();
        names.exist.name = nameTag == null ? null : obj.getTag(nameTag);
        names.exist.name_be = nameBeTag == null ? null : obj.getTag(nameBeTag);
        names.exist.name_ru = nameRuTag == null ? null : obj.getTag(nameRuTag);
        names.exist.int_name = nameIntlTag == null ? null : obj.getTag(nameIntlTag);

        String nameOSM = obj.getTag(names.tags.name);
        if (nameOSM == null) {
            return null;
        }
        String name = StreetNameParser.fix(nameOSM);
        String name_ru = null;
        String name_be = null;
        if (names.tags.name_ru != null) {
            name_ru = obj.getTag(names.tags.name_ru);
        }
        if (names.tags.name_be != null) {
            name_be = obj.getTag(names.tags.name_be);
        }
        if (name_be != null) {
            name_be = StreetNameParser.fix(name_be);
        }

        try {
            process(c, obj, names, name, name_ru, name_be, names.exist.name);
        } catch (Exception ex) {
            names.error = ex.getMessage();
        }
        return names;
    }

    void process(City c, BaseObject obj, StreetNames streetNames, String name, String name_ru, String name_be,
            String toComment) throws Exception {
        StreetName orig = StreetNameParser.parse(name);
        StreetNameBe be = null;

        if (name_be != null) {
            be = new StreetNameBe();
            try {
                be.parseAny(name_be);
            } catch (ParseException ex) {
            }
        }

        if (orig.name == null) {
            return;
        }

        LocalizationInfo li = c.uniq.get(orig.name);
        if (li == null) {
            li = new LocalizationInfo();
            c.uniq.put(orig.name, li);
        }
        li.name.add(toComment);
        if (be != null && be.name != null) {
            li.name_be.add(be.name);
        }
        li.ways.add(streetNames.objCode);

        postProcess(c, obj, streetNames, orig, name, name_ru, name_be);
    }

    void postProcess(City c, BaseObject obj, StreetNames streetNames, StreetName street, String name, String name_ru,
            String name_be) throws Exception {
    }

    static class LocalizationInfo {
        List<String> ways = new ArrayList<>();
        Set<String> name = new TreeSet<>();
        Set<String> name_be = new TreeSet<>();
    }

    public static class City {
        public final String fn;
        public final String nazva;
        public final Area border;
        POReader po;
        Map<String, LocalizationInfo> uniq = new HashMap<>();
        Map<String, Set<String>> renames = new TreeMap<>();

        public City(Miesta m, Area border) {
            this.nazva = m.voblasc + '/' + m.nazvaNoStress;
            this.fn = Lat.unhac(Lat.lat(nazva, false));
            this.border = border;
        }

        @Override
        public int hashCode() {
            return nazva.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof City) {
                return nazva.equals(((City) obj).nazva);
            } else {
                return false;
            }
        }
    }
}
