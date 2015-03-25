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
import org.alex73.osm.utils.CSV;
import org.alex73.osm.utils.Env;
import org.alex73.osm.utils.Lat;
import org.alex73.osm.utils.POReader;
import org.alex73.osm.utils.TMX;
import org.alex73.osm.validators.common.Errors;
import org.alex73.osm.validators.harady.Miesta;
import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.geometry.FastArea;
import org.alex73.osmemory.geometry.IExtendedObject;
import org.alex73.osmemory.geometry.OsmHelper;

/**
 * Стварае файлы .po для перакладу назваў вуліц.
 */
public class StreetsParse3 {
    public static Locale BE = new Locale("be");
    public static Collator BEL = Collator.getInstance(BE);
    public static final Pattern RE_HOUSENUMBER = Pattern
            .compile("[1-9][0-9]*(/[1-9][0-9]*)?(/?[АБВГДабвгд])?( к[0-9]+)?");

    static String poOutputDir;
    static String tmxOutputDir;
    static String davFile;

    Belarus storage;
    List<Miesta> daviednik;
    Errors globalErrors = new Errors();
    List<City> cities = new ArrayList<>();
    List<IExtendedObject> houses = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        poOutputDir = Env.readProperty("po.source.dir");
        tmxOutputDir = Env.readProperty("tmx.output.dir");

        StreetsParse3 s = new StreetsParse3();
        s.run();
    }

    void run() throws Exception {
        davFile = Env.readProperty("dav") + "/Nazvy_nasielenych_punktau.csv";
        System.out.println("Parsing csv from " + davFile);
        daviednik = new CSV('\t').readCSV(davFile, Miesta.class);

        System.out.println("Checking...");
        loadCities();
        storage.byTag("addr:housenumber", h -> houses.add(OsmHelper.extendedFromObject(h, storage)));

        for (City c : cities) {
            start(c);
            processStreets(c);
            processHouses(c);
            end(c);
        }
    }

    short addrStreetTag, nameTag, namebeTag, houseNumberTag;

    public void loadCities() throws Exception {
        storage = new Belarus();
        addrStreetTag = storage.getTagsPack().getTagCode("addr:street");
        nameTag = storage.getTagsPack().getTagCode("name");
        namebeTag = storage.getTagsPack().getTagCode("name:be");
        houseNumberTag = storage.getTagsPack().getTagCode("addr:housenumber");

        for (Miesta m : daviednik) {
            switch (m.typ) {
            case "г.":
            case "г. п.":
                if (m.osmIDother != null && !m.osmIDother.trim().isEmpty()) {
                    FastArea border = null;
                    for (String id : m.osmIDother.split(";")) {
                        IOsmObject city = storage.getObject(id);
                        if (city != null) {
                            try {
                                border = new FastArea(city, storage);
                            } catch (Exception ex) {
                                globalErrors.addError("Памылка стварэньня межаў " + m.nazva + ": "
                                        + ex.getMessage());
                            }
                            break;
                        }
                    }
                    if (border != null) {
                        cities.add(new City(m, border));
                    } else {
                        globalErrors.addError("Няма межаў горада " + m.nazva);
                    }
                }
                break;
            }
        }
    }

    void start(City c) throws Exception {

    }

    void end(City c) throws Exception {
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
        BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(poFile), "UTF-8"));
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

    public void processStreets(City c) {
        storage.byTag("highway", o -> o.isWay() && o.hasTag(nameTag) && c.geom.covers(o),
                o -> processStreet(c, o));
    }

    void processStreet(City c, IOsmObject s) {
        String highway = s.getTag("highway", storage);
        switch (highway) {
        case "raceway":
        case "cycleway":
        case "path":
        case "bus_stop":
            return;
        }
        c.streets.add(s);

        String nameOSM = s.getTag("name", storage);
        if (nameOSM == null) {
            return;
        }
        String name = StreetNameParser.fix(nameOSM);
        String name_be = s.getTag("name:be", storage);
        if (name_be != null) {
            name_be = StreetNameParser.fix(name_be);
        }

        try {
            process(c, s, name, name_be);
        } catch (NullPointerException ex) {
            throw ex;
        } catch (Exception ex) {
            postProcessError(c, s, ex.getMessage());
        }
    }

    public void processHouses(City c) throws Exception {

    }

    void process(City c, IOsmObject obj, String name, String name_be) throws Exception {
        StreetName orig = StreetNameParser.parse(name);

        if (orig.name == null) {
            return;
        }

        LocalizationInfo li = c.uniq.get(orig.name);
        if (li == null) {
            li = new LocalizationInfo();
            c.uniq.put(orig.name, li);
        }
        li.name.add(name);
        li.ways.add(obj.getObjectCode());

        if (name_be != null) {
            try {
                StreetNameBe be = new StreetNameBe();
                be.parseAny(name_be);
                if (be.name != null) {
                    li.name_be.add(be.name);
                }
            } catch (ParseException ex) {
            }
        }

        postProcess(c, obj, orig);
    }

    void postProcessError(City c, IOsmObject obj, String error) {
    }

    void postProcess(City c, IOsmObject obj, StreetName orig) throws Exception {
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
            this.fn = Lat.unhac(Lat.lat(nazva, false)).replace(' ', '_').replace("<kraina>/Minsk", "Minskaja/Minsk");
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
}
