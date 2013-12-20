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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.alex73.osm.data.BaseObject;
import org.alex73.osm.utils.Lat;
import org.alex73.osm.utils.POReader;
import org.alex73.osm.utils.VelocityOutput;

/**
 * Правярае несупадзеньне тэгаў OSM правільным назвам.
 * 
 * -DdisableAddrStreet - не правяраць назвы вуліц таксама ў relation.
 * 
 * -DdisableIntName - не правяраць int_name
 * 
 * -DdisableAddressStreetBe - не правяраць address:street:be
 */
public class CheckStreets extends StreetsParse {
    static String pbfFile;
    static String outDir;
    static String poInputDir;

    public static void main(String[] args) throws Exception {
        long mem = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        if (mem < 700) {
            System.err.println("Using memory: " + mem + "MiB, add memory using -Xmx800m");
            System.exit(1);
        }

        for (String a : args) {
            if (a.startsWith("--pbf=")) {
                pbfFile = a.substring(6).replace("$HOME", System.getProperty("user.home"));
            } else if (a.startsWith("--dav=")) {
                davFile = a.substring(6).replace("$HOME", System.getProperty("user.home"));
            } else if (a.startsWith("--po-dir=")) {
                poInputDir = a.substring(9).replace("$HOME", System.getProperty("user.home"));
            } else if (a.startsWith("--out-dir=")) {
                outDir = a.substring(10).replace("$HOME", System.getProperty("user.home"));
            }
        }
        if (pbfFile == null || davFile == null || poInputDir == null || outDir == null) {
            System.err
                    .println("CheckStreets --pbf=tmp/belarus-latest.osm.pbf --po-dir=../strstr/target/ --out-dir=$HOME/");
            System.exit(1);
        }

        CheckStreets s = new CheckStreets();
        s.run(pbfFile);
    }

    List<String> log2 = new ArrayList<>();
    Map<City, Result> result = new HashMap<>();

    @Override
    public void init() throws Exception {
        super.init();
        for (City c : cities) {
            result.put(c, new Result());
        }
        for (City c : cities) {
            String file = poInputDir + '/' + c.fn + ".po";
            c.po = new POReader(file);
            System.out.println("Read " + c.po.size() + " translations from " + file);
        }
    }

    @Override
    void end() throws Exception {
        for (StreetNames n : resultStreets) {
            if (n.error == null) {
                result.get(n.c).vulicy.add(n);
            } else {
                addWithCreate(result.get(n.c).pamylki, n.error, n);
            }
        }
        for (StreetNames n : resultHouses) {
            if (n.error == null) {
                addWithCreate(result.get(n.c).damy, n, n);
            } else {
                addWithCreate(result.get(n.c).pamylki, n.error, n);
            }
        }
        for (StreetNames n : resultRelations) {
            if (n.error == null) {
                result.get(n.c).vulicy.add(n);
            } else {
                addWithCreate(result.get(n.c).pamylki, n.error, n);
            }
        }

        for (Result r : result.values()) {
            Collections.sort(r.vulicy, new Comparator<StreetNames>() {
                @Override
                public int compare(StreetNames o1, StreetNames o2) {
                    String s1 = nvl(o1.required.name, o1.exist.name);
                    String s2 = nvl(o2.required.name, o2.exist.name);
                    return s1.compareToIgnoreCase(s2);
                }

                String nvl(String... ss) {
                    for (String s : ss) {
                        if (s != null) {
                            return s;
                        }
                    }
                    return "";
                }
            });
        }

        List<City> sortedList = new ArrayList<>(result.keySet());
        Collections.sort(sortedList, new Comparator<City>() {
            public int compare(City o1, City o2) {
                return o1.nazva.compareToIgnoreCase(o2.nazva);
            }
        });

        System.out.println("Output to " + outDir + "/vulicy.html...");
        VelocityOutput.output("org/alex73/osm/validators/vulicy/vulicySpis.velocity", outDir + "/vulicy.html",
                "harady", sortedList, "data", result);
        for (City c : cities) {
            System.out.println("Output to " + outDir + "/vulicy-" + c.fn + ".html...");
            VelocityOutput.output("org/alex73/osm/validators/vulicy/vulicyHorada.velocity", outDir + "/vulicy-" + c.fn
                    + ".html", "horad", c.nazva, "data", result.get(c));
        }
    }

    @Override
    void postProcess(City c, BaseObject obj, StreetNames streetNames, StreetName street, String name, String name_ru,
            String name_be) throws Exception {
        String trans = c.po.get(street.name);
        if ("".equals(trans)) {
            trans = null;
        }
        if (trans == null) {
            throw new Exception("Не перакладзена '" + street.name + "'");
        }

        if (street.term == null) {
            street.term = StreetTerm.вуліца;
        }

        int pm = trans.indexOf('/');
        if (pm < 0) {
            throw new Exception("Не пазначана часьціна мовы: " + street.name + " => " + trans);
        }
        String mode = trans.substring(0, pm);
        trans = trans.substring(pm + 1);

        StreetNameBe be = new StreetNameBe();
        be.term = street.term;
        be.index = street.index;
        be.name = trans;
        switch (mode) {
        case "н": // назоўнік
            street.prym = false;
            be.prym = false;
            break;
        case "пж": // прыметнік жаночага роду
            street.prym = true;
            be.prym = true;
            if (street.term.getRodRu() != StreetTermRod.ZAN | be.term.getRodBe() != StreetTermRod.ZAN) {
                throw new Exception("Не супадае род: " + streetNames.exist.name + " => " + be);
            }
            break;
        case "пм": // прыметнік мужчынскага роду
            street.prym = true;
            be.prym = true;
            if (street.term.getRodRu() != StreetTermRod.MUZ | be.term.getRodBe() != StreetTermRod.MUZ) {
                throw new Exception("Не супадае род: " + streetNames.exist.name + " => " + be);
            }
            break;
        case "пн":
            street.prym = true;
            be.prym = true;
            if (street.term.getRodRu() != StreetTermRod.NI | be.term.getRodBe() != StreetTermRod.NI) {
                throw new Exception("Не супадае род: " + streetNames.exist.name + " => " + be);
            }
            break;
        case "пнж":
            street.prym = true;
            be.prym = true;
            if (street.term.getRodRu() != StreetTermRod.NI | be.term.getRodBe() != StreetTermRod.ZAN) {
                throw new Exception("Не супадае род: " + streetNames.exist.name + " => " + be);
            }
            break;
        default:
            throw new Exception("Невядомы прэфікс у перакладзе: " + c.po.get(street.name));
        }

        streetNames.required.name = streetNames.tags.name == null ? null : street.getRightName();
        streetNames.required.name_ru = streetNames.tags.name_ru == null ? null : streetNames.required.name;
        streetNames.required.name_be = streetNames.tags.name_be == null ? null : be.getRightName();
        streetNames.required.int_name = streetNames.tags.int_name == null ? null : Lat.lat(be.getRightName(), false);
    }

    static <K, V> void addWithCreate(Map<K, List<V>> map, K key, V value) {
        List<V> list = map.get(key);
        if (list == null) {
            list = new ArrayList<>();
            map.put(key, list);
        }
        list.add(value);
    }

    public static class Result {
        public int getErrCount() {
            return vulicy.size() + pamylki.size() + damy.size();
        }

        public List<StreetNames> vulicy = new ArrayList<>();

        public Map<String, List<StreetNames>> pamylki = new TreeMap<>();

        public Map<StreetNames, List<StreetNames>> damy = new TreeMap<>(new Comparator<StreetNames>() {
            public int compare(StreetNames o1, StreetNames o2) {
                int c = 0;
                if (c == 0) {
                    c = cmp(o1.exist.name, o2.exist.name);
                }
                if (c == 0) {
                    c = cmp(o1.required.name, o2.required.name);
                }
                if (c == 0) {
                    c = cmp(o1.exist.name_be, o2.exist.name_be);
                }
                if (c == 0) {
                    c = cmp(o1.required.name_be, o2.required.name_be);
                }
                if (c == 0) {
                    c = cmp(o1.exist.name_ru, o2.exist.name_ru);
                }
                if (c == 0) {
                    c = cmp(o1.required.name_ru, o2.required.name_ru);
                }
                if (c == 0) {
                    c = cmp(o1.exist.int_name, o2.exist.int_name);
                }
                if (c == 0) {
                    c = cmp(o1.required.int_name, o2.required.int_name);
                }
                return c;
            }

            int cmp(String s1, String s2) {
                if (s1 == null)
                    s1 = "";
                if (s2 == null)
                    s2 = "";
                return s1.compareTo(s2);
            }
        });

        public String mergeCodes(List<StreetNames> list) {
            StringBuilder o = new StringBuilder();
            for (StreetNames n : list) {
                o.append(',');
                o.append(n.objCode);
            }
            return o.substring(1);
        }
    }
}
