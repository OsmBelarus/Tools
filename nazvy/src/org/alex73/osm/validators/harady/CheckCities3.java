/**************************************************************************
 Some tools for OSM.

 Copyright (C) 2013-2014 Aleś Bułojčyk <alex73mail@gmail.com>
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.alex73.osm.utils.Belarus;
import org.alex73.osm.utils.CSV;
import org.alex73.osm.utils.Env;
import org.alex73.osm.utils.Lat;
import org.alex73.osm.utils.OSM;
import org.alex73.osm.utils.PadzielOsmNas;
import org.alex73.osm.utils.VelocityOutput;
import org.alex73.osm.validators.common.Errors;
import org.alex73.osm.validators.common.JS;
import org.alex73.osm.validators.common.ResultTable2;
import org.alex73.osmemory.IOsmNode;
import org.alex73.osmemory.IOsmObject;

/**
 * Правярае супадзеньне назваў населеных пунктаў OSM назвам деведніка.
 * 
 * admin_level - толькі для boundary=administrative
 */
public class CheckCities3 {
    static public class Result {
        public Errors errors = new Errors();

        // Неўжытыя ў даведніку аб'екты
        public Errors unusedInDav = new Errors();

        // Несупадзеньне тэгаў з назвамі у даведніку й аб'екце
        public ResultTable2 incorrectTags;
        public ResultTable2 incorrectTagsRehijony;

        public void writeJS(String file) throws Exception {
            JS js = new JS(file);
            js.add("errors", errors.getJS());
            js.add("unusedInDav", unusedInDav.getJS());
            js.add("incorrectTags", incorrectTags.getJS());
            js.add("incorrectTagsRehijony", incorrectTagsRehijony.getJS());
            js.add("errors", errors);
        }
    }

    static Result result = new Result();

    static Belarus storage;

    static List<Miesta> daviednik;
    static Map<String, Set<String>> usedInDav = new HashMap<>();
    static Map<String, IOsmObject> dbPlaces;
    static Map<String, String> adminLevelsBelToRus;
    static String davDirectory;

    public static void main(String[] args) throws Exception {
        String out = Env.readProperty("out.dir") + "/nazvy.html";
        String outjs = Env.readProperty("out.dir") + "/nazvy.js";
        davDirectory = Env.readProperty("dav");

        System.out.println("Parsing csv from " + davDirectory);
        daviednik = new CSV('\t').readCSV(davDirectory+"/Nazvy_nasielenych_punktau.csv", Miesta.class);

        loadData();

        System.out.println("Checking...");
        adminLevelsBelToRus = CheckRehijony.load();
        findNonExistInOsm();
        findUnusedInDav();
        findIncorrectTags();
        findRehijony();

        CheckRehijony.check(storage,result.errors);
        System.out.println("Output to " + out + "...");
        result.incorrectTags.sort();
        new File(out).getParentFile().mkdirs();
        result.writeJS(outjs);
        VelocityOutput.output("org/alex73/osm/validators/harady/validatar.velocity", out, "data", result,
                "OSM", OSM.class);
        System.out.println("done");
    }

    static void loadData() throws Exception {
        storage = new Belarus();

        System.out.println("Find places...");
        // шукаем цэнтры гарадоў
        dbPlaces = new HashMap<>();
        short placeTag = storage.getTagsPack().getTagCode("place");
        storage.byTag("place", o -> ciMiesta(o.getTag(placeTag)) && storage.contains(o),
                o -> dbPlaces.put(o.getObjectCode(), o));
        storage.byTag("abandoned:place", o -> storage.contains(o), o -> dbPlaces.put(o.getObjectCode(), o));
    }

    static boolean ciMiesta(String placeTagValue) {
        switch (placeTagValue) {
        case "city":
        case "town":
        case "village":
        case "hamlet":
        case "isolated_dwelling":
            return true;
        default:
            return false;
        }
    }

    static void findNonExistInOsm() {
        for (Miesta m : daviednik) {
            if (m.osmID != null) {
                if (!dbPlaces.containsKey("n" + m.osmID)) {
                    if (storage.getNodeById(m.osmID) != null) {
                        result.errors.addError("Ёсьць "
                                + " на мапе, але не населены пункт " + m, "n" + m.osmID);
                    } else {
                        result.errors.addError("Няма " 
                                + " на мапе, але ёсць у " + m, "n" + m.osmID);
                    }
                } else {
                    addUsed("n" + m.osmID, m.toString());
                }
            }
            if (m.osmIDother != null && !m.osmIDother.trim().isEmpty()) {
                for (String id : m.osmIDother.split(";")) {
                    try {
                        if (!dbPlaces.containsKey(id)) {
                            if (storage.getObject(id) != null) {
                                result.errors.addError("Ёсьць " 
                                        + " на мапе, але не населены пункт " + m, id);
                            } else {
                                result.errors.addError("Няма " +  " на мапе, але ёсць у "
                                        + m, id);
                            }
                        } else {
                            addUsed(id, m.toString());
                        }
                    } catch (Exception ex) {
                        result.errors.addError(ex.getMessage());
                    }
                }
            }
        }
        for (Map.Entry<String, Set<String>> en : usedInDav.entrySet()) {
            if (en.getValue().size() > 1) {
                result.errors.addError(OSM.histText(en.getKey()) + " выкарыстоўваецца двойчы ў даведніку: "
                        + en.getValue());
            }
        }
    }

    static void addUsed(String code, String where) {
        Set<String> w = usedInDav.get(code);
        if (w == null) {
            w = new TreeSet<>();
            usedInDav.put(code, w);
        }
        w.add(where);
    }

    /**
     * Шукаем аб'екты што ёсьць ў даведніку але няма ў osm. Магчыма, былі выдаленыя.
     */
    static void findUnusedInDav() {
        for (IOsmObject p : dbPlaces.values()) {
            String place = p.getTag("place", storage);
            if ("island".equals(place) || "islet".equals(place)) {
                continue;
            }
            if (!usedInDav.containsKey(p.getObjectCode())) {
                result.unusedInDav.addError(p.getTag("addr:region", storage) + "|"
                        + p.getTag("addr:district", storage) + "|" + p.getTag("name", storage) 
                        , p.getObjectCode());
            }
        }
    }

    static void findRehijony() throws Exception {
        result.incorrectTagsRehijony = new ResultTable2("name", "name:be", "name:ru", "int_name",
                "addr:country", "addr:region", "admin_level", "iso3166-2");
        short nameTag = storage.getTagsPack().getTagCode("name");
        short nameBeTag = storage.getTagsPack().getTagCode("name:be");
        short nameRuTag = storage.getTagsPack().getTagCode("name:ru");
        short intNameTag = storage.getTagsPack().getTagCode("int_name");
        short addrCountryTag = storage.getTagsPack().getTagCode("addr:country");
        short addrRegionTag = storage.getTagsPack().getTagCode("addr:region");
        short isoTag = storage.getTagsPack().getTagCode("iso3166-2");
        short adminLevelTag = storage.getTagsPack().getTagCode("admin_level");

        List<PadzielOsmNas> padziel = new CSV('\t').readCSV(davDirectory + "/Rehijony.csv",
                PadzielOsmNas.class);
        Map<String, String> codes = new HashMap<>();
        Map<String, String> voblasciBelToRus = new HashMap<>();
        for (PadzielOsmNas p : padziel) {
            IOsmObject o = storage.getRelationById(p.relationID);
            ResultTable2.ResultTableRow w = result.incorrectTagsRehijony.new ResultTableRow("", o.getObjectCode(),
                    p.toString());
            String name, nameru, reg = null;
            if (p.voblasc != null && p.rajon == null) {
                codes.put(p.voblasc, p.iso_3166_2);
                voblasciBelToRus.put(p.voblasc, p.osmNameRu);
            }
            String admin_level;
            if (p.rajon != null) {
                name = (p.osmName != null ? p.osmName : p.rajon) + " раён";
                nameru = p.osmNameRu + " район";
                reg = voblasciBelToRus.get(p.voblasc) + " область";
                admin_level = "6";
            } else if (p.voblasc != null) {
                name = (p.osmName != null ? p.osmName : p.voblasc) + " вобласць";
                nameru = p.osmNameRu + " область";
                admin_level = "4";
            } else {
                name = "Беларусь";
                nameru = "Беларусь";
                admin_level = "2";
            }

            w.setAttr("name", o.getTag(nameTag), nameru);
            w.setAttr("name:be", o.getTag(nameBeTag), name);
            w.setAttr("name:ru", o.getTag(nameRuTag), nameru);
            w.setAttr("int_name", o.getTag(intNameTag), Lat.lat(name, false));
            if (p.rajon == null && p.voblasc == null) {
                w.setAttr("int_name", o.getTag(intNameTag), "Belarus");
            }
            w.setAttr("addr:country", o.getTag(addrCountryTag), "BY");
            w.setAttr("addr:region", o.getTag(addrRegionTag), reg);
            w.setAttr("iso3166-2", o.getTag(isoTag), p.iso_3166_2);
            w.setAttr("admin_level", o.getTag(adminLevelTag), admin_level);
            w.addChanged();
        }
    }

    static void findIncorrectTags() throws Exception {
        List<String> attrs = new ArrayList<>();
        addColumnIfAllowed("place", attrs);
        addColumnIfAllowed("abandoned:place", attrs);
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
        addColumnIfAllowed("admin_level", attrs);
        result.incorrectTags = new ResultTable2(attrs);

        for (Miesta m : daviednik) {
            for (final String code : getUsedCodes(m)) {
                // final WrongTags w = new WrongTags();
                ResultTable2.ResultTableRow w = result.incorrectTags.new ResultTableRow(m.voblasc + '|'
                        + m.rajon, code, m.rajon + '|' + m.sielsaviet + '|' + m.nazva);
                IOsmObject p = dbPlaces.get(code);
                Map<String, String> tags = p.extractTags(storage);
                IOsmNode centerNode = m.osmID != null ? storage.getNodeById(m.osmID) : null;
                if (centerNode == null) {
                    result.errors.addError("Няма цэнтру ",code);
                    continue;
                }
                PlaceTags correctTags = CalcCorrectTags2.calc(m, storage, centerNode);
                // правяраем тэгі
                setAttrIfAllowed(w, "name", tags.get("name"), correctTags.name);
                setAttrIfAllowed(w, "name:be", tags.get("name:be"), correctTags.name_be);
                setAttrIfAllowed(w, "name:ru", tags.get("name:ru"), correctTags.name_ru);
                setAttrIfAllowed(w, "int_name", tags.get("int_name"), correctTags.int_name);
                setAttrIfAllowed(w, "name:be-tarask", tags.get("name:be-tarask"), correctTags.name_be_tarask);
                setAttrIfAllowed(w, "place", tags.get("place"), correctTags.place);
                setAttrIfAllowed(w, "abandoned:place", tags.get("abandoned:place"), correctTags.abandonedPlace);
                setAttrIfAllowed(w, "alt_name:be", tags.get("alt_name:be"), correctTags.alt_name_be);
                setAttrIfAllowed(w, "alt_name:ru", tags.get("alt_name:ru"), correctTags.alt_name_ru);
                setAttrIfAllowed(w, "alt_name", tags.get("alt_name"), null);

                setAttrIfAllowed(w, "addr:country", tags.get("addr:country"), "BY");
                String voblasc = adminLevelsBelToRus.get(m.voblasc + " вобласць");
                String rajon = adminLevelsBelToRus.get(m.rajon + " раён");
                if ("<краіна>".equals(m.rajon)) {
                    setAttrIfAllowed(w, "addr:region", tags.get("addr:region"), null);
                    setAttrIfAllowed(w, "addr:district", tags.get("addr:district"), null);
                    setAttrIfAllowed(w, "admin_level", tags.get("admin_level"), "4");
                } else if ("<вобласць>".equals(m.rajon)) {
                    setAttrIfAllowed(w, "addr:region", tags.get("addr:region"), voblasc != null ? voblasc
                            : "???");
                    setAttrIfAllowed(w, "addr:district", tags.get("addr:district"), null);
                    setAttrIfAllowed(w, "admin_level", tags.get("admin_level"), "6");
                } else if ("<раён>".equals(m.rajon)) {
                    setAttrIfAllowed(w, "addr:region", tags.get("addr:region"), voblasc != null ? voblasc
                            : "???");
                    setAttrIfAllowed(w, "addr:district", tags.get("addr:district"), rajon != null ? rajon
                            : "???");
                    setAttrIfAllowed(w, "admin_level", tags.get("admin_level"), "8");
                } else {
                    setAttrIfAllowed(w, "addr:region", tags.get("addr:region"), voblasc != null ? voblasc
                            : "???");
                    setAttrIfAllowed(w, "addr:district", tags.get("addr:district"), rajon != null ? rajon
                            : "???");
                    setAttrIfAllowed(w, "admin_level", tags.get("admin_level"), "8");
                }
                if (!"administrative".equals(tags.get("boundary"))) {
                    setAttrIfAllowed(w, "admin_level", tags.get("admin_level"), null);
                }
                if ("suburb".equals(correctTags.place)) {
                    setAttrIfAllowed(w, "addr:country", tags.get("addr:country"), null);
                    setAttrIfAllowed(w, "addr:region", tags.get("addr:region"), null);
                    setAttrIfAllowed(w, "addr:district", tags.get("addr:district"), null);
                }

                w.addChanged();
            }
        }
    }

    static void addColumnIfAllowed(String attrName, List<String> list) {
        if (!"skip".equals(Env.readProperty("nazvy_viosak." + attrName.replace(':', '_')))) {
            list.add(attrName);
        }
    }

    static void setAttrIfAllowed(ResultTable2.ResultTableRow w, String attrName, String oldValue,
            String newValue) {
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
            IOsmObject n = dbPlaces.get("n" + m.osmID);
            if (n != null) {
                c.add(n.getObjectCode());
            }
        }
        if (m.osmIDother != null && !m.osmIDother.trim().isEmpty()) {
            for (String id : m.osmIDother.split(";")) {
                IOsmObject o = dbPlaces.get(id);
                if (o != null) {
                    c.add(o.getObjectCode());
                }
            }
        }
        return c;
    }
}
