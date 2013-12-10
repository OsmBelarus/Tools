package org.alex73.osm.validators;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alex73.osm.data.BaseObject;
import org.alex73.osm.data.MemoryStorage;
import org.alex73.osm.data.NodeObject;
import org.alex73.osm.data.PbfDriver;
import org.alex73.osm.daviednik.CalcCorrectTags;
import org.alex73.osm.daviednik.Miesta;
import org.alex73.osm.utils.OSM;
import org.alex73.osm.utils.TSV;
import org.alex73.osm.utils.VelocityOutput;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

public class CheckCities {
    static public class Result {
        public String getCurrentDateTime() {
            return new Date().toGMTString();
        }

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
        public List<String> incorrectNames = new ArrayList<>();

        public List<String> getIncorrectNames() {
            return incorrectNames;
        }
    }

    static Result result = new Result();

    static MemoryStorage osm;
    static List<Miesta> daviednik;
    static Set<String> usedInDav = new HashSet<>();

    public static void main(String[] args) throws Exception {
        long mem = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        if (mem < 700) {
            System.err.println("Using memory: " + mem + "MiB, add memory using -Xmx800m");
            System.exit(1);
        }

        String pbf = null;
        String out = null;
        String dav = null;
        for (String a : args) {
            if (a.startsWith("--pbf=")) {
                pbf = a.substring(6);
            } else if (a.startsWith("--dav=")) {
                dav = a.substring(6);
            } else if (a.startsWith("--out=")) {
                out = a.substring(6).replace("$HOME", System.getProperty("user.home"));
            }
        }
        if (pbf == null || out == null || dav == null) {
            System.err
                    .println("CheckCities --pbf=http://download.geofabrik.de/europe/belarus-latest.osm.pbf --dav=https://github.com/OsmBelarus/Databases/raw/master/Nazvy_nasielenych_punktau/list.csv --out=/tmp/out.html");
            System.exit(1);
        }
        System.out.println("Loading pbf from " + pbf + "...");
        FileUtils.copyURLToFile(new URL(pbf), new File("tmp/pbf.pbf"));
        System.out.println("Parsing pbf...");
        osm = PbfDriver.process(new File("tmp/pbf.pbf"));

        System.out.println("Loading csv from " + dav + "...");
        FileUtils.copyURLToFile(new URL(dav), new File("tmp/list.csv"));
        System.out.println("Parsing csv...");
        daviednik = new TSV('\t').readCSV("tmp/list.csv", Miesta.class);

        System.out.println("Checking...");
        findNonExistInOsm();
        findUnusedInDav();
        findIncorrectNames();

        System.out.println("Output to " + out + "...");
        Collections.sort(result.nonExistInOsm);
        Collections.sort(result.unusedInDav);
        Collections.sort(result.incorrectNames);
        new File(out).getParentFile().mkdirs();
        VelocityOutput.output("org/alex73/osm/validators/validatar.velocity", result, out);
        System.out.println("done");
    }

    static void findNonExistInOsm() {
        for (Miesta m : daviednik) {
            if (m.osmID != null) {
                if (osm.getNodeById(m.osmID) == null) {
                    result.nonExistInOsm.add("Няма " + OSM.hist("n" + m.osmID) + " на мапе, але ёсць у " + m);
                } else {
                    usedInDav.add("n" + m.osmID);
                }
            }
            if (m.osmIDother != null && !m.osmIDother.trim().isEmpty()) {
                for (String id : m.osmIDother.split(";")) {
                    try {
                        BaseObject o = osm.getObject(id);
                        if (o == null) {
                            result.nonExistInOsm.add("Няма " + OSM.hist(id) + " на мапе, але ёсць у " + m);
                        } else {
                            usedInDav.add(id);
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
        for (BaseObject o : osm.allObjects) {
            String place = o.getTag("place");
            if (place == null || "island".equals(place) || "islet".equals(place)) {
                continue;
            }
            if (osm.isInsideBelarus(o)) {
                if (!usedInDav.contains(o.getCode())) {
                    result.unusedInDav.add("Няма ў даведніку : " + o.getTag("addr:region") + "|"
                            + o.getTag("addr:district") + "|" + o.getTag("name") + "/" + OSM.browse(o.getCode()));
                }
            }
        }
    }

    static void findIncorrectNames() {
        for (Miesta m : daviednik) {
            for (String code : getUsedCodes(m)) {
                try {
                    BaseObject o = osm.getObject(code);
                    Map<String, String> correctTags = CalcCorrectTags.calc(m, osm);
                    for (String tag : correctTags.keySet()) {
                        String mustBe = correctTags.get(tag);
                        String exist = o.getTag(tag);
                        if (!StringUtils.equals(mustBe, exist)) {
                            result.incorrectNames.add(m + "/" + OSM.hist(o.getCode()) + ": чакаецца " + tag + "='"
                                    + mustBe + "' але ёсць '" + exist + "' "
                                    + " <input type='radio' onClick='send(\"load_object?objects=" + code + "&addtags="
                                    + tag + "=" + mustBe + "\")'>");
                        }
                    }
                } catch (Exception ex) {
                    result.incorrectNames.add(ex.getMessage());
                }
            }
        }
    }

    /**
     * Вяртае усе аб'екты osm што выкарыстаныя ў радку даведніка.
     */
    static List<String> getUsedCodes(Miesta m) {
        List<String> c = new ArrayList<>();
        if (m.osmID != null) {
            NodeObject n = osm.getNodeById(m.osmID);
            if (n != null) {
                c.add(n.getCode());
            }
        }
        if (m.osmIDother != null && !m.osmIDother.trim().isEmpty()) {
            for (String id : m.osmIDother.split(";")) {
                try {
                    BaseObject o = osm.getObject(id);
                    if (o != null) {
                        c.add(o.getCode());
                    }
                } catch (Exception ex) {
                }
            }
        }
        return c;
    }
}
