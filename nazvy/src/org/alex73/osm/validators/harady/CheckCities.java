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

package org.alex73.osm.validators.harady;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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
import org.apache.commons.lang.StringUtils;

/**
 * Правярае супадзеньне назваў населеных пунктаў OSM назвам деведніка.
 */
public class CheckCities {
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
                pbf = a.substring(6).replace("$HOME", System.getProperty("user.home"));
            } else if (a.startsWith("--dav=")) {
                dav = a.substring(6).replace("$HOME", System.getProperty("user.home"));
            } else if (a.startsWith("--out=")) {
                out = a.substring(6).replace("$HOME", System.getProperty("user.home"));
            }
        }
        if (pbf == null || out == null || dav == null) {
            System.err.println("CheckCities --pbf=tmp/belarus-latest.osm.pbf --dav=tmp/list.csv --out=/tmp/out.html");
            System.exit(1);
        }
        System.out.println("Parsing pbf from " + pbf);
        osm = PbfDriver.process(new File(pbf));

        System.out.println("Parsing csv from " + dav);
        daviednik = new TSV('\t').readCSV(dav, Miesta.class);

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

    static void findNonExistInOsm() {
        for (Miesta m : daviednik) {
            if (m.osmID != null) {
                if (osm.getNodeById(m.osmID) == null) {
                    result.nonExistInOsm.add("Няма " + OSM.histText("n" + m.osmID) + " на мапе, але ёсць у " + m);
                } else {
                    usedInDav.add("n" + m.osmID);
                }
            }
            if (m.osmIDother != null && !m.osmIDother.trim().isEmpty()) {
                for (String id : m.osmIDother.split(";")) {
                    try {
                        BaseObject o = osm.getObject(id);
                        if (o == null) {
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
        findUnusedInDavList(osm.nodes);
        findUnusedInDavList(osm.ways);
        findUnusedInDavList(osm.relations);
    }

    static void findUnusedInDavList(List<? extends BaseObject> list) {
        for (BaseObject o : list) {
            String place = o.getTag("place");
            if (place == null || "island".equals(place) || "islet".equals(place)) {
                continue;
            }
            if (osm.isInsideBelarus(o)) {
                if (!usedInDav.contains(o.getCode())) {
                    result.unusedInDav.add(o.getTag("addr:region") + "|" + o.getTag("addr:district") + "|"
                            + o.getTag("name") + "/" + OSM.browse(o.getCode()));
                }
            }
        }
    }

    /**
     * Шукаем аб'екты што ня маюць нейкіх патрэбных тэгаў.
     */
    static void findRequiredTags() {
        findRequiredTagsList(osm.nodes);
        findRequiredTagsList(osm.ways);
        findRequiredTagsList(osm.relations);
    }

    static void findRequiredTagsList(List<? extends BaseObject> list) {
        for (BaseObject o : list) {
            String place = o.getTag("place");
            if (place == null || "island".equals(place) || "islet".equals(place)) {
                continue;
            }
            if (osm.isInsideBelarus(o)) {
                NoTags w = new NoTags();
                w.davName = o.getTag("name");
                w.osmLink = OSM.histIcon(o.getCode());
                w.existCountry = o.getTag("addr:country") != null;
                w.existRegion = o.getTag("addr:region") != null;
                w.existDistrict = o.getTag("addr:district") != null;
                switch (place) {
                case "city":
                case "town":
                case "village":
                    String population = o.getTag("population");
                    String population_date = o.getTag("population_date");
                    w.correctPopulation = population != null && population.matches("[0-9]+")
                            && Integer.parseInt(population) < 2500000 && Integer.parseInt(population) > 1000
                            && population_date != null && population_date.matches("[0-9]{4}")
                            && Integer.parseInt(population_date) > Calendar.getInstance().get(Calendar.YEAR) - 40
                            && Integer.parseInt(population_date) <= Calendar.getInstance().get(Calendar.YEAR);
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
        TagChecker tcNameBexOld = new TagChecker("name:be-x-old") {
            void onError(WrongTags w, String errText) {
                w.other = add(w.other, "name:be-x-old: " + errText);
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
                    BaseObject o = osm.getObject(code);
                    w.osmLink = OSM.histIcon(o.getCode());
                    Map<String, String> correctTags = CalcCorrectTags.calc(m, osm);
                    if ("suburb".equals(o.getTag("place")) && "hamlet".equals(correctTags.get("place"))) {
                        // hamlet => suburb - ok
                        correctTags.put("place", o.getTag("place"));
                    }
                    if ("neighbourhood".equals(o.getTag("place")) && "hamlet".equals(correctTags.get("place"))) {
                        // hamlet => neighbourhood - ok
                        correctTags.put("place", o.getTag("place"));
                    }
                    // правяраем тэгі
                    tcName.check(w, o, correctTags);
                    tcNameBe.check(w, o, correctTags);
                    tcNameRu.check(w, o, correctTags);
                    tcIntName.check(w, o, correctTags);
                    tcNameBeTarask.check(w, o, correctTags);
                    tcNameBexOld.check(w, o, correctTags);
                    if (correctTags.containsKey("place")) {
                        tcPlace.check(w, o, correctTags);
                    }
                    tcAltNameBe.check(w, o, correctTags);
                    tcAltNameRu.check(w, o, correctTags);
                    tcAltNameEn.check(w, o, correctTags);
                    tcAltName.check(w, o, correctTags);

                    if (!correctTags.isEmpty()) {
                        // яшчэ засталіся неправераныя ?
                        throw new Exception("Unchecked tags: " + correctTags.keySet());
                    }
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

        public void check(WrongTags w, BaseObject o, Map<String, String> correctTags) {
            String exist = o.getTag(tagName);
            String mustBe = correctTags.get(tagName);
            if (!StringUtils.equals(exist, mustBe)) {
                w.correct = false;
                onError(w, "<span class='err'>" + exist + " => " + mustBe
                        + " <input type='radio' onClick='send(\"load_object?objects=" + o.getCode() + "&addtags="
                        + tagName + "=" + mustBe + "\")'></span>");
            } else {
                onOk(w, mustBe);
            }
            correctTags.remove(tagName);
        }
    }
}
