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
import java.util.regex.Pattern;

import org.alex73.osm.utils.Belarus;
import org.alex73.osm.utils.Env;
import org.alex73.osm.utils.Lat;
import org.alex73.osm.utils.OSM;
import org.alex73.osm.utils.POReader;
import org.alex73.osm.utils.TMX;
import org.alex73.osm.utils.TSV;
import org.alex73.osm.validators.harady.Miesta;
import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.IOsmWay;
import org.alex73.osmemory.geometry.Area;
import org.alex73.osmemory.geometry.FastArea;
import org.alex73.osmemory.geometry.Way;
import org.apache.commons.lang.StringUtils;

/**
 * Стварае файлы .po для перакладу назваў вуліц.
 */
public class StreetsParse3 {
    public static Locale BE = new Locale("be");
    public static Collator BEL = Collator.getInstance(BE);
    public static final Pattern RE_HOUSENUMBER = Pattern.compile("[1-9][0-9]*(/[1-9][0-9]*)?[АБВГ]?");

    static String poOutputDir;
    static String tmxOutputDir;
    static String davFile;

    Belarus storage;
    List<Miesta> daviednik;
    List<String> errors = new ArrayList<>();
    List<City> cities = new ArrayList<>();
    List<StreetNames> resultStreets = new ArrayList<>();
    List<HouseError> resultHouses = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        Env.load();
        poOutputDir = Env.readProperty("po.source.dir");
        tmxOutputDir = Env.readProperty("tmx.output.dir");
        davFile = Env.readProperty("dav");

        StreetsParse3 s = new StreetsParse3();
        s.run();
    }

    void run() throws Exception {
        System.out.println("Parsing csv from " + davFile);
        daviednik = new TSV('\t').readCSV(davFile, Miesta.class);

        System.out.println("Checking...");
        init();

        processStreets();
        // processHouses();

        end();
    }

    public void init() throws Exception {
        storage = new Belarus();

        for (Miesta m : daviednik) {
            switch (m.typ) {
            case "г.":
                if (m.osmIDother != null && !m.osmIDother.trim().isEmpty()) {
                    FastArea border = null;
                    for (String id : m.osmIDother.split(";")) {
                        long cid = Long.parseLong(id.substring(1));
                        String rqName;
                        if (id.charAt(0) == 'r') {
                            rqName = "osm.getPlaceByRelationId";
                        } else {
                            rqName = "osm.getPlaceByWayId";
                        }
                        IOsmObject city = storage.getObject(id);
                        if (city != null) {
                            try {
                                border = new FastArea(new Area(storage, city).getGeometry(), storage);
                            } catch (Exception ex) {
                                errors.add("Памылка стварэньня межаў " + m.nazva + ": " + ex.getMessage());
                            }
                            break;
                        }
                    }
                    if (border != null) {
                        cities.add(new City(m, border));
                    } else {
                        errors.add("Няма межаў горада " + m.nazva);
                    }
                }
                break;
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
                Collections.sort(li.ways, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        long i1 = Long.parseLong(o1.substring(1));
                        long i2 = Long.parseLong(o2.substring(1));
                        return Long.compare(i1, i2);
                    }
                });
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

    public void processStreets() {
        List<IOsmObject> highways = new ArrayList<>();
        List<Way> lines = new ArrayList<>();
        storage.byTag("highway",
                o -> o.isWay() && !o.hasTag("int_ref", storage) && !o.hasTag("ref", storage),
                o -> highways.add(o));
        for (IOsmObject o : highways) {
            lines.add(new Way((IOsmWay) o, storage));
        }
        for (City c : cities) {
            System.out.println("Check street in " + c.nazva);
            for (Way li : lines) {
                if (!c.geom.interceptBox(li.getBoundingBox())) {
                    continue;
                }
                if (c.geom.covers(li.getWay())) {
                    addStreet(c, li.getWay());
                }
            }
        }
    }

    void addStreet(City c, IOsmObject s) {
        String highway = s.getTag("highway", storage);
        switch (highway) {
        case "raceway":
        case "cycleway":
        case "path":
        case "bus_stop":
            return;
        }
        c.streets.add(s);
        StreetNames n = processTags(c, s, "name", "name:ru", "name:be",
                System.getProperty("disableIntName") == null ? "int_name" : null);
        if (n != null && n.needToChange()) {
            resultStreets.add(n);
        }
    }

    short addrStreetTag, nameTag, namebeTag;

    public void processHouses() throws Exception {
        addrStreetTag = storage.getTagsPack().getTagCode("addr:street");
        nameTag = storage.getTagsPack().getTagCode("name");
        namebeTag = storage.getTagsPack().getTagCode("name:be");
        for (City c : cities) {
            System.out.println("Check houses in " + c.nazva);
            storage.byTag("addr:housenumber", h -> c.geom.covers(h), h -> processHouse(c, h));
        }
    }

    public void processHouse(City c, IOsmObject s) {
        String streetNameOnHouse = s.getTag(addrStreetTag);
        if (streetNameOnHouse == null) {
            // няма тэга addr:street
            HouseError e = new HouseError();
            e.c = c;
            e.object = s;
            e.error = "Няма тэга addr:street для дому";
            resultHouses.add(e);
            return;
        }
        boolean streetFound = false;
        String prev_name_be = null;
        for (IOsmObject r : c.streets) {
            if (!streetNameOnHouse.equals(r.getTag(nameTag))) {
                continue;
            }
            streetFound = true;
            String name_be = r.getTag(namebeTag);
            if (prev_name_be != null && !prev_name_be.equals(name_be)) {
                // і беларуская назва не супадае
                HouseError e = new HouseError();
                e.c = c;
                e.object = s;
                e.error = "Беларускія назвы вуліц побач не супадаюць для дому з addr:street="
                        + streetNameOnHouse;
                resultHouses.add(e);
            }
            prev_name_be = name_be;
        }
        if (!streetFound) {
            // няма вуліцы для гэтага дому
            HouseError e = new HouseError();
            e.c = c;
            e.object = s;
            e.error = "Няма вуліцы '" + streetNameOnHouse + "' для дому";
            resultHouses.add(e);
            return;
        }
    }

    void checkHouseTags(City c, IOsmObject s) throws Exception {
        for (Map.Entry<String, String> en : s.extractTags(storage).entrySet()) {
            String k = en.getKey();
            String v = en.getValue();
            switch (k) {
            case "addr:street":
                break;
            case "addr:postcode":
                break;
            case "building:levels":
                break;
            case "addr:housenumber":
                if (!RE_HOUSENUMBER.matcher(v).matches()) {
                    HouseError e = new HouseError();
                    e.c = c;
                    e.object = s;
                    e.error = "Няправільны нумар дому: " + v;
                    resultHouses.add(e);
                }
                break;
            case "building":
                if (!"yes".equals(v)) {
                    HouseError e = new HouseError();
                    e.c = c;
                    e.object = s;
                    e.error = "Няправільны тэг building для дому: " + v;
                    resultHouses.add(e);
                }
                break;
            default:
                HouseError e = new HouseError();
                e.c = c;
                e.object = s;
                e.error = "Невядомы тэг для дому : " + k + "=" + v;
                resultHouses.add(e);
                break;
            }
        }
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

    StreetNames processTags(City c, IOsmObject obj, String nameTag, String nameRuTag, String nameBeTag,
            String nameIntlTag) {

        StreetNames names = new StreetNames();
        names.c = c;
        names.tags.name = nameTag;
        names.tags.name_be = nameBeTag;
        names.tags.name_ru = nameRuTag;
        names.tags.int_name = nameIntlTag;

        names.objCode = obj.getObjectCode();
        names.exist.name = nameTag == null ? null : obj.getTag("name", storage);
        names.exist.name_be = nameBeTag == null ? null : obj.getTag("name:be", storage);
        names.exist.name_ru = nameRuTag == null ? null : obj.getTag("name:ru", storage);
        names.exist.int_name = nameIntlTag == null ? null : obj.getTag("int_name", storage);

        String nameOSM = names.exist.name;
        if (nameOSM == null) {
            return null;
        }
        String name = StreetNameParser.fix(nameOSM);
        String name_ru = null;
        String name_be = null;
        if (names.tags.name_ru != null) {
            name_ru = names.exist.name_ru;
        }
        if (names.tags.name_be != null) {
            name_be = names.exist.name_be;
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

    void process(City c, IOsmObject obj, StreetNames streetNames, String name, String name_ru,
            String name_be, String toComment) throws Exception {
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

    void postProcess(City c, IOsmObject obj, StreetNames streetNames, StreetName street, String name,
            String name_ru, String name_be) throws Exception {
    }

    static class LocalizationInfo {
        List<String> ways = new ArrayList<>();
        Set<String> name = new TreeSet<>();
        Set<String> name_be = new TreeSet<>();
    }

    public static class City {
        public final String fn;
        public final String nazva;
        public final FastArea geom;
        List<IOsmObject> streets = new ArrayList<>();
        POReader po;
        Map<String, LocalizationInfo> uniq = new HashMap<>();
        Map<String, Set<String>> renames = new TreeMap<>();

        public City(Miesta m, FastArea geomText) {
            this.nazva = m.voblasc + '/' + m.nazvaNoStress;
            this.fn = Lat.unhac(Lat.lat(nazva, false)).replace(' ', '_');
            this.geom = geomText;
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

    public static class HouseError {
        public City c;
        public IOsmObject object;
        public String error;
        public String geom;

        public String getLink() {
            return OSM.histIcon(object.getObjectCode());
        }
    }
}
