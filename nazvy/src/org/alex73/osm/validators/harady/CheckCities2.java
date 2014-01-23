package org.alex73.osm.validators.harady;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alex73.osm.daviednik.CalcCorrectTags;
import org.alex73.osm.daviednik.Miesta;
import org.alex73.osm.utils.Env;
import org.alex73.osm.utils.OSM;
import org.alex73.osm.utils.TSV;
import org.alex73.osm.utils.VelocityOutput;
import org.alex73.osm.validators.vulicy2.OsmPlace;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

/**
 * Правярае супадзеньне назваў населеных пунктаў OSM назвам деведніка.
 */
public class CheckCities2 {
    static public class WrongTags implements Comparable<WrongTags> {
        public String osmLink;
        public String davName;
        public String type, name, nameBe, nameRu, intName, other;
        public boolean correct = true;

        @Override
        public int compareTo(WrongTags o) {
            return davName.compareToIgnoreCase(o.davName);
        }
    }

    static public class NoTags implements Comparable<NoTags> {
        public String osmLink;
        public String davName;
        public boolean existCountry, existDistrict, existRegion, correctPopulation;

        boolean isCorrect() {
            return existCountry && existDistrict && existRegion && correctPopulation;
        }

        @Override
        public int compareTo(NoTags o) {
            return davName.compareToIgnoreCase(o.davName);
        }
    }

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
        public List<WrongTags> incorrectTags = new ArrayList<>();

        public List<WrongTags> getIncorrectTags() {
            return incorrectTags;
        }

        // Нявызначаныя тэгі
        public List<NoTags> requiredTags = new ArrayList<>();

        public List<NoTags> getRequiredTags() {
            return requiredTags;
        }
    }

    static Result result = new Result();

    static SqlSession db;
    static List<Miesta> daviednik;
    static Set<String> usedInDav = new HashSet<>();
    static Map<String,OsmPlace> dbPlaces;

    public static void main(String[] args) throws Exception {
        Env.load();
        
        String out =Env.readProperty("out.dir")+"/nazvy.html";
        String dav = Env.readProperty("dav");

        System.out.println("Parsing csv from " + dav);
        daviednik = new TSV('\t').readCSV(dav, Miesta.class);

        initDB();
        
        System.out.println("Checking...");
        findNonExistInOsm();
        findUnusedInDav();
        findIncorrectTags();
        findRequiredTags();

        System.out.println("Output to " + out + "...");
        Collections.sort(result.nonExistInOsm);
        Collections.sort(result.unusedInDav);
        Collections.sort(result.incorrectTags);
        new File(out).getParentFile().mkdirs();
        VelocityOutput.output("org/alex73/osm/validators/harady/validatar.velocity", out, "data", result);
        System.out.println("done");
    }

    static void initDB() throws Exception {
        System.out.println("Load database...");
        String resource = "osm.xml";
        SqlSessionFactory sqlSessionFactory;
        InputStream inputStream = Resources.getResourceAsStream(resource);
        try {
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream, Env.env);
        } finally {
            inputStream.close();
        }
        db = sqlSessionFactory.openSession();
        
        dbPlaces = new HashMap<>();
        List<OsmPlace> list = db.selectList("osm.getPlaces");
        for (OsmPlace place : list) {
            dbPlaces.put(place.getCode(), place);
        }
    }
    
    static void findNonExistInOsm() {
        for (Miesta m : daviednik) {
            if (m.osmID != null) {
                if (!dbPlaces.containsKey("n"+m.osmID)) {
                    result.nonExistInOsm.add("Няма " + OSM.histText("n" + m.osmID) + " на мапе, але ёсць у " + m);
                } else {
                    usedInDav.add("n" + m.osmID);
                }
            }
            if (m.osmIDother != null && !m.osmIDother.trim().isEmpty()) {
                for (String id : m.osmIDother.split(";")) {
                    try {
                        if (!dbPlaces.containsKey(id)) {
                            result.nonExistInOsm.add("Няма " + OSM.histText(id) + " на мапе, але ёсць у " + m);
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
        for (OsmPlace p : dbPlaces.values()) {
            if ("island".equals(p.place) || "islet".equals(p.place)) {
                continue;
            }
                if (!usedInDav.contains(p.getCode())) {
                    result.unusedInDav.add(p.addr_region + "|" + p.addr_district + "|"
                            + p.name + "/" + OSM.browse(p.getCode()));
                }
        }
    }

    /**
     * Шукаем аб'екты што ня маюць нейкіх патрэбных тэгаў.
     */
    static void findRequiredTags() {
        for (OsmPlace p : dbPlaces.values()) {
            if ("island".equals(p.place) || "islet".equals(p.place)) {
                continue;
            }
                NoTags w = new NoTags();
                w.davName = p.name;
                w.osmLink = OSM.histIcon(p.getCode());
                w.existCountry = p.addr_country != null;
                w.existRegion = p.addr_region != null;
                w.existDistrict = p.addr_district != null;
                switch (p.place) {
                case "city":
                case "town":
                case "village":
                    w.correctPopulation = p.population != null && p.population.matches("[0-9]+")
                            && Integer.parseInt(p.population) < 2500000 && Integer.parseInt(p.population) > 1000
                            && p.population_date != null && p.population_date.matches("[0-9]{4}")
                            && Integer.parseInt(p.population_date) > Calendar.getInstance().get(Calendar.YEAR) - 40
                            && Integer.parseInt(p.population_date) <= Calendar.getInstance().get(Calendar.YEAR);
                    break;
                default:
                    w.correctPopulation = true;
                    break;
                }
                if (!w.isCorrect()) {
                    result.requiredTags.add(w);
                }
        }
    }
    

    static void findIncorrectTags() {
        // ствараем праверкі для тэгаў
        TagChecker tcName = new TagChecker("name") {
            void onError(WrongTags w, String errText) {
                w.name = errText;
            }

            void onOk(WrongTags w, String correct) {
                w.name = correct;
            }
        };
        TagChecker tcNameBe = new TagChecker("name:be") {
            void onError(WrongTags w, String errText) {
                w.nameBe = errText;
            }

            void onOk(WrongTags w, String correct) {
                w.nameBe = correct;
            }
        };
        TagChecker tcNameRu = new TagChecker("name:ru") {
            void onError(WrongTags w, String errText) {
                w.nameRu = errText;
            }

            void onOk(WrongTags w, String correct) {
                w.nameRu = correct;
            }
        };
        TagChecker tcIntName = new TagChecker("int_name") {
            void onError(WrongTags w, String errText) {
                w.intName = errText;
            }

            void onOk(WrongTags w, String correct) {
                w.intName = correct;
            }
        };
        TagChecker tcNameBeTarask = new TagChecker("name:be-tarask") {
            void onError(WrongTags w, String errText) {
                w.other = add(w.other, "name:be-tarask: " + errText);
            }

            void onOk(WrongTags w, String correct) {
            }
        };
        TagChecker tcPlace = new TagChecker("place") {
            void onError(WrongTags w, String errText) {
                w.type = errText;
            }

            void onOk(WrongTags w, String correct) {
                w.type = correct;
            }
        };
        TagChecker tcAltNameBe = new TagChecker("alt_name:be") {
            void onError(WrongTags w, String errText) {
                w.other = add(w.other, "alt_name:be: " + errText);
            }

            void onOk(WrongTags w, String correct) {
            }
        };
        TagChecker tcAltNameRu = new TagChecker("alt_name:ru") {
            void onError(WrongTags w, String errText) {
                w.other = add(w.other, "alt_name:ru: " + errText);
            }

            void onOk(WrongTags w, String correct) {
            }
        };
        TagChecker tcAltNameEn = new TagChecker("alt_name:en") {
            void onError(WrongTags w, String errText) {
                w.other = add(w.other, "alt_name:en: " + errText);
            }

            void onOk(WrongTags w, String correct) {
            }
        };
        TagChecker tcAltName = new TagChecker("alt_name") {
            void onError(WrongTags w, String errText) {
                w.other = add(w.other, "alt_name: " + errText);
            }

            void onOk(WrongTags w, String correct) {
            }
        };

        for (Miesta m : daviednik) {
            for (final String code : getUsedCodes(m)) {
                final WrongTags w = new WrongTags();
                w.davName = m.sielsaviet + '|' + m.nazva;
                try {
                    OsmPlace p = dbPlaces.get(code);
                    w.osmLink = OSM.histIcon(p.getCode());
                    OsmPlace correctTags = CalcCorrectTags.calc(m, db);
                    if ("suburb".equals(p.place) && "hamlet".equals(correctTags.place)) {
                        // hamlet => suburb - ok
                        correctTags.place=p.place;
                    }
                    if ("neighbourhood".equals(p.place) && "hamlet".equals(correctTags.place)) {
                        // hamlet => neighbourhood - ok
                        correctTags.place= p.place;
                    }
                    // правяраем тэгі
                    tcName.check(w, p, correctTags);
                    tcNameBe.check(w, p, correctTags);
                    tcNameRu.check(w, p, correctTags);
                    tcIntName.check(w, p, correctTags);
                    tcNameBeTarask.check(w, p, correctTags);
                    if (correctTags.place!=null) {
                        tcPlace.check(w, p, correctTags);
                    }
                    tcAltNameBe.check(w, p, correctTags);
                    tcAltNameRu.check(w, p, correctTags);
                    tcAltNameEn.check(w, p, correctTags);
                    tcAltName.check(w, p, correctTags);

                } catch (Exception ex) {
                    w.other = add(w.other, ex.getMessage());
                }
                if (!w.correct) {
                    result.incorrectTags.add(w);
                }
            }
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
            OsmPlace n = dbPlaces.get("n"+m.osmID);
            if (n != null) {
                c.add(n.getCode());
            }
        }
        if (m.osmIDother != null && !m.osmIDother.trim().isEmpty()) {
            for (String id : m.osmIDother.split(";")) {
                    OsmPlace o = dbPlaces.get(id);
                    if (o != null) {
                        c.add(o.getCode());
                    }
            }
        }
        return c;
    }

    /**
     * Helper for check some specific tag.
     */
    public static abstract class TagChecker {
        final String tagName;

        public TagChecker(String tagName) {
            this.tagName = tagName;
        }

        abstract void onError(WrongTags w, String errText);

        abstract void onOk(WrongTags w, String correct);

        public void check(WrongTags w, OsmPlace p, OsmPlace correctTags) {
            String exist = getTag(tagName,p);
            String mustBe = getTag(tagName,correctTags);
            if (!StringUtils.equals(exist, mustBe)) {
                w.correct = false;
                onError(w, "<span class='err'>" + exist + " => " + mustBe
                        + " <input type='radio' onClick='send(\"load_object?objects=" + p.getCode() + "&addtags="
                        + tagName + "=" + mustBe + "\")'></span>");
            } else {
                onOk(w, mustBe);
            }
        }
        String getTag(String tagName, OsmPlace obj) {
            try {
            Field f=OsmPlace.class.getField(tagName.replace(':','_').replace('-', '_'));
            return (String)f.get(obj);
            } catch(Exception ex) {
                throw new RuntimeException();
            }
        }
    }
}
