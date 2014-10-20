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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.alex73.osm.utils.Env;
import org.alex73.osm.utils.Lat;
import org.alex73.osm.utils.LettersCheck;
import org.alex73.osm.utils.POReader;
import org.alex73.osm.utils.VelocityOutput;
import org.alex73.osm.validators.common.Errors;
import org.alex73.osm.validators.common.ResultTable;
import org.alex73.osm.validators.common.ResultTable.ResultTableRow;
import org.alex73.osmemory.IOsmObject;

/**
 * Правярае несупадзеньне тэгаў OSM правільным назвам.
 * 
 * -DdisableIntName - не правяраць int_name
 */
public class CheckStreets3 extends StreetsParse3 {
    static String outDir;
    static String poInputDir;

    public static void main(String[] args) throws Exception {
        poInputDir = Env.readProperty("po.target.dir");
        outDir = Env.readProperty("out.dir");

        CheckStreets3 s = new CheckStreets3();
        s.run();
        s.end();
    }

    Result cityResult;
    Map<City, Integer> streetErrorsCount = new HashMap<>();
    Map<City, Integer> houseErrorsCount = new HashMap<>();

    @Override
    void start(City c) throws Exception {
        cityResult = new Result();
        String file = poInputDir + '/' + c.fn + ".po";
        c.po = new POReader(file);
        System.out.println("Read " + c.po.size() + " translations from " + file);
    }

    @Override
    void end(City c) throws Exception {
        houseErrorsCount.put(c, cityResult.pamylkiDamou.getObjectsCount());
        streetErrorsCount.put(c, cityResult.pamylkiVulic.getObjectsCount() + cityResult.vulicy.rows.size());

        System.out.println("Output to " + outDir + "/vulicy-" + c.fn + ".html...");
        VelocityOutput.output("org/alex73/osm/validators/vulicy/vulicyHorada.velocity", outDir + "/vulicy-"
                + c.fn + ".html", "horad", c.nazva, "data", cityResult);
        VelocityOutput.output("org/alex73/osm/validators/vulicy/damyHorada.velocity", outDir + "/damy-"
                + c.fn + ".html", "horad", c.nazva, "data", cityResult);
    }

    void end() throws Exception {
        System.out.println("Output to " + outDir + "/vulicy.html...");
        VelocityOutput.output("org/alex73/osm/validators/vulicy/vulicySpis.velocity",
                outDir + "/vulicy.html", "cities", cities, "errors", globalErrors, "streetErrorsCount",
                streetErrorsCount, "houseErrorsCount", houseErrorsCount);
    }

    @Override
    void postProcessError(City c, IOsmObject obj, String error) {
        if (error == null) {
            error = "<null>";
        }
        cityResult.pamylkiVulic.addError(error, obj);
    }

    @Override
    void postProcess(City c, IOsmObject obj, StreetName orig) throws Exception {
        String trans = c.po.get(orig.name);
        checkName(orig.name, trans);

        if (orig.term == null) {
            orig.term = StreetTerm.вуліца;
        }

        int pm = trans.indexOf('/');
        if (pm < 0) {
            throw new Exception("Не пазначана часьціна мовы: " + orig.name + " => " + trans);
        }
        String mode = trans.substring(0, pm);
        trans = trans.substring(pm + 1);

        if (!trans.equals(trans.trim().replaceAll("\\s{2,}", " "))) {
            throw new Exception("Няправільныя прагалы ў перакладзе: " + orig.name + " => " + trans);
        }

        StreetNameBe be = new StreetNameBe();
        be.term = orig.term;
        be.index = orig.index;
        be.name = trans;
        switch (mode) {
        case "н": // назоўнік
            orig.prym = false;
            be.prym = false;
            break;
        case "пж": // прыметнік жаночага роду
            orig.prym = true;
            be.prym = true;
            if (orig.term.getRodRu() != StreetTermRod.ZAN | be.term.getRodBe() != StreetTermRod.ZAN) {
                throw new Exception("Не супадае род: " + orig + " => " + be);
            }
            break;
        case "пм": // прыметнік мужчынскага роду
            orig.prym = true;
            be.prym = true;
            if (orig.term.getRodRu() != StreetTermRod.MUZ | be.term.getRodBe() != StreetTermRod.MUZ) {
                throw new Exception("Не супадае род: " + orig + " => " + be);
            }
            break;
        case "пн":
            orig.prym = true;
            be.prym = true;
            if (orig.term.getRodRu() != StreetTermRod.NI | be.term.getRodBe() != StreetTermRod.NI) {
                throw new Exception("Не супадае род: " + orig + " => " + be);
            }
            break;
        case "пнж":
            orig.prym = true;
            be.prym = true;
            if (orig.term.getRodRu() != StreetTermRod.NI | be.term.getRodBe() != StreetTermRod.ZAN) {
                throw new Exception("Не супадае род: " + orig + " => " + be);
            }
            break;
        case "0":
            orig.term = StreetTerm.няма;
            be.term = StreetTerm.няма;
            break;
        default:
            throw new Exception("Невядомы прэфікс у перакладзе: " + c.po.get(orig.name));
        }

        ResultTableRow row = (cityResult.vulicy).new ResultTableRow(obj.getObjectCode(), "");
        if ("skip".equals(Env.readProperty("nazvy_vulic.name"))) {
            row.setAttr("name", null, null);
        } else {
            row.setAttr("name", obj.getTag("name", storage), orig.getRightName());
        }
        if ("skip".equals(Env.readProperty("nazvy_vulic.name_ru"))) {
            row.setAttr("name:ru", null, null);
        } else {
            row.setAttr("name:ru", obj.getTag("name:ru", storage), orig.getRightName());
        }
        if ("skip".equals(Env.readProperty("nazvy_vulic.name_be"))) {
            row.setAttr("name:be", null, null);
        } else {
            row.setAttr("name:be", obj.getTag("name:be", storage), be.getRightName());
        }
        if ("skip".equals(Env.readProperty("nazvy_vulic.int_name"))) {
            row.setAttr("int_name", null, null);
        } else {
            row.setAttr("int_name", obj.getTag("int_name", storage), Lat.lat(be.getRightName(), false));
        }
        if ("skip".equals(Env.readProperty("nazvy_vulic.name_en"))) {
            row.setAttr("name:en", null, null);
        } else {
            row.setAttr("name:en", obj.getTag("name:en", storage), null);
        }
        if ("skip".equals(Env.readProperty("nazvy_vulic.name_by"))) {
            row.setAttr("name:by", null, null);
        } else {
            row.setAttr("name:by", obj.getTag("name:by", storage), null);
        }

        if (row.needChange()) {
            cityResult.vulicy.rows.add(row);
        }
    }

    static void checkName(String orig, String trans) throws Exception {
        if ("".equals(trans)) {
            trans = null;
        }
        if (trans == null) {
            throw new Exception("Не перакладзена '" + orig + "'");
        }
        // выдаляем лацінскія нумары
        trans = trans.replaceAll("/[XVI]+ ", "/").replaceAll(" [XVI]+$", "").replaceAll("\\-[XVI]+$", "")
                .replaceAll(" [XVI]+ ", " ");
        // і праектуемыя вуліцы
        trans = trans.replaceAll("№[0-9]+", "");

        String errName = LettersCheck.checkBe(trans);
        if (errName != null) {
            throw new Exception("Няправільныя літары ў беларускай назьве: " + errName);
        }

        // выдаляем лацінскія нумары
        orig = orig.replaceAll("^[XVI]+ ", "").replaceAll(" [XVI]+$", "").replaceAll("\\-[XVI]+$", "")
                .replaceAll(" [XVI]+ ", " ");
        // і праектуемыя вуліцы
        orig = orig.replaceAll("№[0-9]+", "");

        errName = LettersCheck.checkRu(orig);
        if (errName != null) {
            throw new Exception("Няправільныя літары ў расейскай назьве: " + errName);
        }
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

    @Override
    public void processHouses(City c) throws Exception {
        System.out.println("Check houses in " + c.nazva);
        storage.byTag("addr:housenumber", h -> c.geom.covers(h), h -> processHouse(c, h));
    }

    public void processHouse(City c, IOsmObject s) {

        String streetNameOnHouse = s.getTag(addrStreetTag);
        if (streetNameOnHouse == null) {
            // няма тэга addr:street
            cityResult.pamylkiDamou.addError("Няма тэга addr:street для дому", s);
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
                cityResult.pamylkiDamou.addError(
                        "Беларускія назвы вуліц побач не супадаюць для дому з addr:street="
                                + streetNameOnHouse, s);
            }
            prev_name_be = name_be;
        }
        if (!streetFound) {
            // няма вуліцы для гэтага дому
            cityResult.pamylkiDamou.addError("Няма вуліцы '" + streetNameOnHouse + "' для дому", s);
            return;
        }
        checkHouseTags(c, s);
    }

    void checkHouseTags(City c, IOsmObject s) {
        String num = s.getTag(houseNumberTag);
        if (num != null) {
            if (!RE_HOUSENUMBER.matcher(num).matches()) {
                cityResult.pamylkiDamou.addError("Няправільны нумар дому: " + num, s);
            }
        }
    }

    public static class Result {
        public ResultTable vulicy = new ResultTable("name", "name:ru", "name:be", "name:en", "int_name",
                "name:en", "name:by");

        public Errors pamylkiVulic = new Errors();

        public Errors pamylkiDamou = new Errors();
    }
}
