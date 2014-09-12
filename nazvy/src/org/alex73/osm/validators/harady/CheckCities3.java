package org.alex73.osm.validators.harady;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alex73.osm.daviednik.CalcCorrectTags2;
import org.alex73.osm.daviednik.Miesta;
import org.alex73.osm.utils.Env;
import org.alex73.osm.utils.OSM;
import org.alex73.osm.utils.TSV;
import org.alex73.osm.utils.VelocityOutput;
import org.alex73.osm.validators.vulicy2.OsmPlace;
import org.alex73.osmemory.BaseObject2;
import org.alex73.osmemory.FastPolygon;
import org.alex73.osmemory.MemoryStorage2;
import org.alex73.osmemory.NodeObject2;
import org.alex73.osmemory.PbfReader2;
import org.alex73.osmemory.Polygon;
import org.apache.commons.io.FileUtils;

/**
 * Правярае супадзеньне назваў населеных пунктаў OSM назвам деведніка.
 */
public class CheckCities3 {
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

    static MemoryStorage2 storage;
    static FastPolygon border;

    static List<Miesta> daviednik;
    static Set<String> usedInDav = new HashSet<>();
    static Map<String, BaseObject2> dbPlaces;
    static Map<String, String> adminLevelsBelToRus;

    public static void main(String[] args) throws Exception {
        Env.load();

        String out = Env.readProperty("out.dir") + "/nazvy.html";
        String dav = Env.readProperty("dav");

        System.out.println("Parsing csv from " + dav);
        daviednik = new TSV('\t').readCSV(dav, Miesta.class);

        loadData();

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

    static void loadData() throws Exception {
        String borderWKT = FileUtils.readFileToString(new File(Env.readProperty("coutry.border.wkt")), "UTF-8");
        Polygon Belarus = Polygon.fromWKT(borderWKT);

        System.out.println("Load data...");
        storage = new PbfReader2(Belarus.getBoundEnvelope()).read(new File(Env.readProperty("data.file")));
        storage.showStat();

        border = new FastPolygon(Belarus, storage);

        System.out.println("Find places...");
        // шукаем цэнтры гарадоў
        dbPlaces = new HashMap<>();
        storage.allHasTag("place").process(o -> border.contains(o), o -> dbPlaces.put(o.getCode(), o));
    }

    static void loadAdminLevels() {
        adminLevelsBelToRus = new HashMap<>();

        storage.allHasTag("admin_level").process(o -> isAdminPartOfBelarus(o), o -> storeAdminPart(o));
    }

    static boolean isAdminPartOfBelarus(BaseObject2 obj) {
        String admin_level = storage.getTag(obj, "admin_level");
        switch (admin_level) {
        case "2":
        case "4":
        case "6":
            break;
        default:
            return false;
        }
        if (!"boundary".equals(storage.getTag(obj, "type"))) {
            return false;
        }
        return true;
    }

    static void storeAdminPart(BaseObject2 obj) {
        String name = storage.getTag(obj, "name");
        String name_be = storage.getTag(obj, "name:be");
        if (name_be != null && name != null) {
            if (name_be.endsWith(" раён") || name_be.endsWith(" вобласць")) {
                adminLevelsBelToRus.put(name_be, name);
            }
        }
    }

    static void findNonExistInOsm() {
        for (Miesta m : daviednik) {
            if (m.osmID != null) {
                if (!dbPlaces.containsKey("n" + m.osmID)) {
                    if (storage.getNodeById(m.osmID) != null) {
                        result.nonExistInOsm.add("Ёсьць " + OSM.histText("n" + m.osmID)
                                + " на мапе, але не населены пункт " + m);
                    } else {
                        result.nonExistInOsm.add("Няма " + OSM.histText("n" + m.osmID) + " на мапе, але ёсць у " + m);
                    }
                } else {
                    if (!usedInDav.add("n" + m.osmID)) {
                        result.nonExistInOsm.add(OSM.histText("n" + m.osmID) + " выкарыстоўваецца двойчы ў даведніку: "
                                + m);
                    }
                }
            }
            if (m.osmIDother != null && !m.osmIDother.trim().isEmpty()) {
                for (String id : m.osmIDother.split(";")) {
                    try {
                        if (!dbPlaces.containsKey(id)) {
                            if (storage.getObject(id) != null) {
                                result.nonExistInOsm.add("Ёсьць " + OSM.histText(id)
                                        + " на мапе, але не населены пункт " + m);
                            } else {
                                result.nonExistInOsm.add("Няма " + OSM.histText(id) + " на мапе, але ёсць у " + m);
                            }
                        } else {
                            if (!usedInDav.add(id)) {
                                result.nonExistInOsm.add(OSM.histText(id) + " выкарыстоўваецца двойчы ў даведніку: "
                                        + m);
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
        for (BaseObject2 p : dbPlaces.values()) {
            String place = storage.getTag(p, "place");
            if ("island".equals(place) || "islet".equals(place)) {
                continue;
            }
            if (!usedInDav.contains(p.getCode())) {
                result.unusedInDav.add(storage.getTag(p, "addr:region") + "|" + storage.getTag(p, "addr:district")
                        + "|" + storage.getTag(p, "name") + "/" + OSM.browse(p.getCode()));
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
                    BaseObject2 p = dbPlaces.get(code);
                    Map<String, String> tags = storage.extractTags(p);
                    NodeObject2 centerNode = m.osmID != null ? storage.getNodeById(m.osmID) : null;
                    OsmPlace correctTags = CalcCorrectTags2.calc(m, storage, centerNode);
                    if ("suburb".equals(tags.get("place")) && "hamlet".equals(correctTags.place)) {
                        // hamlet => suburb - ok
                        correctTags.place = tags.get("place");
                    }
                    if ("neighbourhood".equals(tags.get("place")) && "hamlet".equals(correctTags.place)) {
                        // hamlet => neighbourhood - ok
                        correctTags.place = tags.get("place");
                    }
                    // правяраем тэгі
                    setAttrIfAllowed(w, "name", tags.get("name"), correctTags.name);
                    setAttrIfAllowed(w, "name:be", tags.get("name:be"), correctTags.name_be);
                    setAttrIfAllowed(w, "name:ru", tags.get("name:ru"), correctTags.name_ru);
                    setAttrIfAllowed(w, "int_name", tags.get("int_name"), correctTags.int_name);
                    setAttrIfAllowed(w, "name:be-tarask", tags.get("name:be-tarask"), correctTags.name_be_tarask);
                    if (correctTags.place != null) {
                        setAttrIfAllowed(w, "place", tags.get("place"), correctTags.place);
                    }
                    setAttrIfAllowed(w, "alt_name:be", tags.get("alt_name:be"), correctTags.alt_name_be);
                    setAttrIfAllowed(w, "alt_name:ru", tags.get("alt_name:ru"), correctTags.alt_name_ru);
                    setAttrIfAllowed(w, "alt_name", tags.get("alt_name"), null);

                    setAttrIfAllowed(w, "addr:country", tags.get("addr:country"), "BY");
                    setAttrIfAllowed(w, "addr:region", tags.get("addr:region"),
                            adminLevelsBelToRus.get(m.voblasc + " вобласць"));
                    setAttrIfAllowed(w, "addr:district", tags.get("addr:district"),
                            adminLevelsBelToRus.get(m.rajon + " раён"));
                } catch (Exception ex) {
                    ex.printStackTrace();
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
            BaseObject2 n = dbPlaces.get("n" + m.osmID);
            if (n != null) {
                c.add(n.getCode());
            }
        }
        if (m.osmIDother != null && !m.osmIDother.trim().isEmpty()) {
            for (String id : m.osmIDother.split(";")) {
                BaseObject2 o = dbPlaces.get(id);
                if (o != null) {
                    c.add(o.getCode());
                }
            }
        }
        return c;
    }
}
