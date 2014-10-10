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
import java.util.regex.Pattern;

import org.alex73.osm.utils.Env;
import org.alex73.osm.utils.Lat;
import org.alex73.osm.utils.OSM;
import org.alex73.osm.utils.POReader;
import org.alex73.osm.utils.VelocityOutput;
import org.alex73.osmemory.IOsmObject;
import org.apache.commons.lang.StringUtils;

/**
 * Правярае несупадзеньне тэгаў OSM правільным назвам.
 * 
 * -DdisableIntName - не правяраць int_name
 */
public class CheckStreets3 extends StreetsParse3 {
    static final Pattern RE_ALLOWED_CHARS = Pattern
            .compile("[1234567890ЁЙЦУКЕНГШЎЗХФЫВАПРОЛДЖЭЯЧСМІТЬБЮ’ёйцукенгшўзхфывапролджэячсмітьбю \\/\\-]+");
    static String outDir;
    static String poInputDir;

    public static void main(String[] args) throws Exception {
        Env.load();

        davFile = Env.readProperty("dav");
        poInputDir = Env.readProperty("po.target.dir");
        outDir = Env.readProperty("out.dir");

        CheckStreets3 s = new CheckStreets3();
        s.run();
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
                addWithCreate(result.get(n.c).pamylkiVulic, n.error, n);
            }
        }
        for (HouseError n : resultHouses) {
            result.get(n.c).pamylkiDamou.add(n);
        }
        for (Result r : result.values()) {
            r.pamylkiDamouG = groupBy(r.pamylkiDamou, new Group.Keyer<HouseError>() {
                public String getKey(HouseError data) {
                    return data.error;
                }
            }, new Group.Creator<HouseError>() {
                public Group<HouseError> create(HouseError data) {
                    return new HouseGroup(data.error);
                }
            });
            Collections.sort(r.pamylkiDamouG, new Comparator<Group<HouseError>>() {
                @Override
                public int compare(Group<HouseError> o1, Group<HouseError> o2) {
                    return o1.getKey().compareTo(o2.getKey());
                }
            });
            // for (HouseGroup hg : (List<HouseGroup>) (List) r.pamylkiDamouG) {
            // hg.xmin = Double.MAX_VALUE;
            // hg.xmax = Double.MIN_VALUE;
            // hg.ymin = Double.MAX_VALUE;
            // hg.ymax = Double.MIN_VALUE;
            // for (HouseError he : hg.objects) {
            // hg.xmin = Math.min(hg.xmin, he.object.xmin);
            // hg.xmax = Math.max(hg.xmax, he.object.xmax);
            // hg.ymin = Math.min(hg.ymin, he.object.ymin);
            // hg.ymax = Math.max(hg.ymax, he.object.ymax);
            // }
            // hg.xmin -= 0.002;
            // hg.xmax += 0.002;
            // hg.ymin -= 0.002;
            // hg.ymax += 0.002;
            // }
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
        VelocityOutput.output("org/alex73/osm/validators/vulicy/vulicySpis.velocity",
                outDir + "/vulicy.html", "harady", sortedList, "data", result, "errors", errors);
        for (City c : cities) {
            System.out.println("Output to " + outDir + "/vulicy-" + c.fn + ".html...");
            VelocityOutput.output("org/alex73/osm/validators/vulicy/vulicyHorada.velocity", outDir
                    + "/vulicy-" + c.fn + ".html", "horad", c.nazva, "data", result.get(c), "OSM", OSM.class);
            VelocityOutput.output("org/alex73/osm/validators/vulicy/damyHorada.velocity", outDir + "/damy-"
                    + c.fn + ".html", "horad", c.nazva, "data", result.get(c));
        }
    }

    @Override
    void postProcess(City c, IOsmObject obj, StreetNames streetNames, StreetName street, String name,
            String name_ru, String name_be) throws Exception {
        String trans = c.po.get(street.name);
        if ("".equals(trans)) {
            trans = null;
        }
        if (trans == null) {
            throw new Exception("Не перакладзена '" + street.name + "'");
        }
        // выдаляем лацінскія нумары
        String test = trans.replaceAll("/[XVI]+ ", "/").replaceAll(" [XVI]+$", "")
                .replaceAll("\\-[XVI]+$", "").replaceAll(" [XVI]+ ", " ");
        if (!RE_ALLOWED_CHARS.matcher(test).matches()) {
            throw new Exception("Невядомыя літары ў '" + trans + "'");
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

        if (!trans.equals(trans.trim().replaceAll("\\s{2,}", " "))) {
            throw new Exception("Няправільныя прагалы ў перакладзе: " + street.name + " => " + trans);
        }

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
        case "0":
            street.term = StreetTerm.няма;
            be.term = StreetTerm.няма;
            break;
        default:
            throw new Exception("Невядомы прэфікс у перакладзе: " + c.po.get(street.name));
        }

        streetNames.required.name = streetNames.tags.name == null ? null : street.getRightName();
        streetNames.required.name_ru = streetNames.tags.name_ru == null ? null : streetNames.required.name;
        streetNames.required.name_be = streetNames.tags.name_be == null ? null : be.getRightName();
        streetNames.required.int_name = streetNames.tags.int_name == null ? null : Lat.lat(be.getRightName(),
                false);

        switch (Env.readProperty("nazvy_vulic.name")) {
        case "skip":
            streetNames.required.name = null;
            streetNames.exist.name = null;
            break;
        case "change":
            if (StringUtils.isEmpty(streetNames.exist.name)) {
                streetNames.required.name = null;
                streetNames.exist.name = null;
            }
            break;
        }
        switch (Env.readProperty("nazvy_vulic.name_ru")) {
        case "skip":
            streetNames.required.name_ru = null;
            streetNames.exist.name_ru = null;
            break;
        case "change":
            if (StringUtils.isEmpty(streetNames.exist.name_ru)) {
                streetNames.required.name_ru = null;
                streetNames.exist.name_ru = null;
            }
            break;
        }
        switch (Env.readProperty("nazvy_vulic.name_be")) {
        case "skip":
            streetNames.required.name_be = null;
            streetNames.exist.name_be = null;
            break;
        case "change":
            if (StringUtils.isEmpty(streetNames.exist.name_be)) {
                streetNames.required.name_be = null;
                streetNames.exist.name_be = null;
            }
            break;
        }
        switch (Env.readProperty("nazvy_vulic.int_name")) {
        case "skip":
            streetNames.required.int_name = null;
            streetNames.exist.int_name = null;
            break;
        case "change":
            if (StringUtils.isEmpty(streetNames.exist.int_name)) {
                streetNames.required.int_name = null;
                streetNames.exist.int_name = null;
            }
            break;
        }
    }

    static <K, V> void addWithCreate(Map<K, List<V>> map, K key, V value) {
        List<V> list = map.get(key);
        if (list == null) {
            list = new ArrayList<>();
            map.put(key, list);
        }
        list.add(value);
    }

    static <T> List<Group<T>> groupBy(List<T> data, Group.Keyer<T> keyer, Group.Creator<T> creator) {
        Map<String, Group<T>> groups = new HashMap<>();
        for (T d : data) {
            String key = keyer.getKey(d);
            Group<T> g = groups.get(key);
            if (g == null) {
                g = creator.create(d);
                groups.put(key, g);
            }
            g.add(d);
        }
        return new ArrayList<>(groups.values());
    }

    public static class Result {
        public int getErrCount() {
            return vulicy.size() + pamylkiVulic.size() + damy.size();
        }

        public int getHouseErrCount() {
            return pamylkiDamou.size();
        }

        public List<StreetNames> vulicy = new ArrayList<>();

        public Map<String, List<StreetNames>> pamylkiVulic = new TreeMap<>();

        public List<HouseError> pamylkiDamou = new ArrayList<>();
        public List<Group<HouseError>> pamylkiDamouG = new ArrayList<>();

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

    public static class HouseGroup extends Group<HouseError> {
        public double xmin, xmax, ymin, ymax;

        public HouseGroup(String key) {
            super(key);
        }

        public String getCommaObjectIds() {
            StringBuilder o = new StringBuilder(200);
            for (HouseError h : objects) {
                o.append(",").append(h.object.getObjectCode());
            }
            return o.substring(1);
        }
    }
}
