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
import java.util.Set;
import java.util.TreeSet;

import org.alex73.osm.data.BaseObject;
import org.alex73.osm.utils.Lat;
import org.alex73.osm.utils.POReader;
import org.alex73.osm.utils.VelocityOutput;

/**
 * Правярае несупадзеньне тэгаў OSM правіьлным назвам.
 * 
 * -DdisableAddrStreet - не правяраць назвы вуліц таксама ў relation.
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
                pbfFile = a.substring(6);
            } else if (a.startsWith("--po-dir=")) {
                poInputDir = a.substring(9).replace("$HOME", System.getProperty("user.home"));
            } else if (a.startsWith("--out-dir=")) {
                outDir = a.substring(10).replace("$HOME", System.getProperty("user.home"));
            }
        }
        if (pbfFile == null || poInputDir == null || outDir == null) {
            System.err
                    .println("CheckStreets --pbf=tmp/belarus-latest.osm.pbf --po-dir=../strstr/target/ --out-dir=$HOME/");
            System.exit(1);
        }

        CheckStreets s = new CheckStreets();
        s.run(pbfFile);
    }

    Set<String> warnings = new TreeSet<>();
    List<String> log2 = new ArrayList<>();
    Map<String, List<StreetNames>> result = new HashMap<>();

    @Override
    public void init() throws Exception {
        super.init();
        for (City c : cities) {
            result.put(c.name, new ArrayList<StreetNames>());
        }
        for (City c : cities) {
            c.po = new POReader(poInputDir + '/' + c.name + ".po");
        }
    }

    @Override
    void end() throws Exception {
        for (List<StreetNames> list : result.values()) {
            Collections.sort(list, new Comparator<StreetNames>() {
                @Override
                public int compare(StreetNames o1, StreetNames o2) {
                    return nvl(o1.required.name).compareToIgnoreCase(nvl(o2.required.name));
                }

                String nvl(String s) {
                    return s != null ? s : "";
                }
            });
        }
        System.out.println("Output to " + outDir + "/vulicy.html...");
        VelocityOutput.output("org/alex73/osm/validators/vulicy/vulicySpis.velocity", result, outDir + "/vulicy.html");
        for (City c : cities) {
            System.out.println("Output to " + outDir + "/vulicy-" + c.name + ".html...");
            VelocityOutput.output("org/alex73/osm/validators/vulicy/vulicyHorada.velocity", result.get(c.name), outDir
                    + "/vulicy-" + c.name + ".html");
        }
    }

    @Override
    void postProcess(City c, BaseObject obj, StreetNames streetNames, StreetName street, String name, String name_ru,
            String name_be) {
        String trans = c.po.get(street.name);
        if ("".equals(trans)) {
            trans = null;
        }
        if (trans == null) {
            warnings.add("Не перакладзена " + street.name);
            return;
        }

        if (street.term == null) {
            street.term = StreetTerm.вуліца;
        }

        int pm = trans.indexOf('/');
        if (pm < 0) {
            warnings.add("Не пазначана часьціна мовы: " + street.name + " => " + trans);
            return;
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
                warnings.add("Не супадае род: " + streetNames.exist.name + " => " + be);
                return;
            }
            break;
        case "пм": // прыметнік мужчынскага роду
            street.prym = true;
            be.prym = true;
            if (street.term.getRodRu() != StreetTermRod.MUZ | be.term.getRodBe() != StreetTermRod.MUZ) {
                warnings.add("Не супадае род: " + streetNames.exist.name + " => " + be);
                return;
            }
            break;
        case "пн":
            street.prym = true;
            be.prym = true;
            if (street.term.getRodRu() != StreetTermRod.NI | be.term.getRodBe() != StreetTermRod.NI) {
                warnings.add("Не супадае род: " + streetNames.exist.name + " => " + be);
                return;
            }
            break;
        case "пнж":
            street.prym = true;
            be.prym = true;
            if (street.term.getRodRu() != StreetTermRod.NI | be.term.getRodBe() != StreetTermRod.ZAN) {
                warnings.add("Не супадае род: " + streetNames.exist.name + " => " + be);
                return;
            }
            break;
        default:
            warnings.add("Невядомы прэфікс: " + c.po.get(street.name));
        }

        streetNames.required.name = streetNames.tags.name == null ? null : street.getRightName();
        streetNames.required.name_ru = streetNames.tags.name_ru == null ? null : streetNames.required.name;
        streetNames.required.name_be = streetNames.tags.name_be == null ? null : be.getRightName();
        streetNames.required.int_name = streetNames.tags.int_name == null ? null : Lat.lat(be.getRightName(), false);

        if (streetNames.needToChange()) {
            result.get(c.name).add(streetNames);
        }
    }
}
