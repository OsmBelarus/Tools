package org.alex73.osm.translate;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.alex73.osm.monitors.export.Borders;
import org.alex73.osm.utils.Belarus;
import org.alex73.osm.utils.CSV;
import org.alex73.osm.utils.Env;
import org.alex73.osm.utils.Lat;
import org.alex73.osm.utils.POWriter;
import org.alex73.osm.utils.RehijonTypeSeparator;
import org.alex73.osm.utils.TMX;
import org.alex73.osm.utils.VelocityOutput;
import org.alex73.osm.validators.common.JS;
import org.alex73.osm.validators.common.ResultTable2;
import org.alex73.osm.validators.common.ResultTable2.ResultTableRow;
import org.alex73.osmemory.IOsmObject;
import org.apache.commons.lang.StringUtils;
import org.omegat.util.Language;
import org.omegat.util.TMXReader2;
import org.omegat.util.TMXReader2.ParsedTu;
import org.omegat.util.TMXReader2.ParsedTuv;

public class TranslationProcessor extends RehijonTypeSeparator {

    Map<String, Typ> ts = new HashMap<>();
    short nameTag, nameBeTag, nameRuTag, nameIntTag;

    public TranslationProcessor(Belarus osm, Borders borders) throws Exception {
        super(osm, borders);
        nameTag = osm.getTagsPack().getTagCode("name");
        nameRuTag = osm.getTagsPack().getTagCode("name:ru");
        nameBeTag = osm.getTagsPack().getTagCode("name:be");
        nameIntTag = osm.getTagsPack().getTagCode("int_name");
    }

    @Override
    synchronized protected void storeToOutput(IOsmObject obj, String typ, String rehijon) throws Exception {
        String name = obj.getTag(nameTag);
        if (StringUtils.isBlank(name)) {// TODO set conversion for spaces -> empty
            return;
        }

        Typ t = getTyp(typ);
        t.add(obj, typ, rehijon);
    }

    synchronized void save() throws Exception {
        ts.values().forEach(t -> t.trs.values().forEach(tr -> tr.write()));
        ts.values().forEach(t -> t.write());
        ts.values().forEach(t -> t.table.sort());
        ts.values().forEach(t -> wr(Env.readProperty("out.dir") + "/pieraklad/" + t.typ + ".js", t.table));
        ts.values().forEach(
                t -> VelocityOutput.output("org/alex73/osm/translate/out.velocity",
                        Env.readProperty("out.dir") + "/pieraklad/" + t.typ + ".html", "typ", t.typ));
    }

    void wr(String file, ResultTable2 table) {
        try {
            JS js = new JS(file);
            js.add("table", table.getJS());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    Typ getTyp(String typ) throws Exception {
        Typ t = ts.get(typ);
        if (t == null) {
            t = new Typ(typ);
            ts.put(typ, t);
        }
        return t;
    }

    public class Typ {
        String typ;
        ResultTable2 table;
        Map<String, String> fixes;
        Map<String, TypReh> trs = new HashMap<>();
        Map<String, String> translation;
        Map<String, Set<String>> existTranslation = new HashMap<>();

        public Typ(String typ) throws Exception {
            this.typ = typ;
            this.table = new ResultTable2("name", "name:ru", "name:be", "int_name");
            // чытаем мэпінг выпраўленьняў
            fixes = new HashMap<>();
            try {
                List<Replace> replaces = new CSV('\t').readCSV(Env.readProperty("translations.dir")
                        + "/vypraulenni/" + getFile() + ".csv", Replace.class);
                for (Replace r : replaces) {
                    if (!r.from.equals(r.to)) {// толькі тыя што не супадаюць
                        fixes.put(r.from, r.to);
                    }
                }
            } catch (FileNotFoundException ex) {
            }

            translation = new HashMap<>();
            try {
                new TMXReader2().readTMX(new File(Env.readProperty("translations.dir") + "/pieraklady/"
                        + typ + ".tmx"), new Language("ru"), new Language("be"), false, false, true, false,
                        new TMXReader2.LoadCallback() {
                            @Override
                            public boolean onEntry(ParsedTu tu, ParsedTuv tuvSource, ParsedTuv tuvTarget,
                                    boolean isParagraphSegtype) {
                                translation.put(tuvSource.text, tuvTarget.text);
                                return true;
                            }
                        });
            } catch (FileNotFoundException ex) {
            }
        }

        void add(IOsmObject obj, String typ, String rehijon) throws Exception {
            String name = obj.getTag(nameTag);
            String namebe = obj.getTag(nameBeTag);
            String namechanged = fixes.get(name);
            if (namechanged == null) {
                namechanged = name;
            }
            if (name != null && namebe != null) {
                Set<String> exist = existTranslation.get(name);
                if (exist == null) {
                    exist = new TreeSet<>();
                    existTranslation.put(name, exist);
                }
                exist.add(namebe);
            }

            TypReh tr = getTypeReh(typ, rehijon);
            tr.add(obj, namechanged);
        }

        public void write() {
            System.out.println("Save translation  " + typ);
            try {
                // запісваем ў csv
                List<Replace> names = new ArrayList<>();
                for (Map.Entry<String, String> en : fixes.entrySet()) {
                    Replace r = new Replace();
                    r.from = en.getKey();
                    r.to = en.getValue();
                    names.add(r);
                }
                Collections.sort(names, new Comparator<Replace>() {
                    Locale RU = new Locale("ru");
                    Collator RUC = Collator.getInstance(RU);

                    @Override
                    public int compare(Replace o1, Replace o2) {
                        return RUC.compare(o1.from, o2.from);
                    }
                });
                new CSV('\t').saveCSV(Env.readProperty("translations.dir") + "/vypraulenni/" + getFile()
                        + ".csv", Replace.class, names);
                
                TMX tmx=new TMX();
                for (Map.Entry<String, Set<String>> en : existTranslation.entrySet()) {
                    if (en.getValue().size() == 1) {
                        tmx.put(en.getKey(), en.getValue().iterator().next());
                    }else {
                        tmx.put(en.getKey(), en.getValue().toString());
                    }
                }
                tmx.save(new File(Env.readProperty("translations.dir") + "/isnujucyja_pieraklady/"
                        + getFile() + ".tmx"));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        String getFile() {
            return typ.replaceAll("^/+", "");
        }

        TypReh getTypeReh(String typ, String rehijon) throws Exception {
            TypReh tr = trs.get(rehijon);
            if (tr == null) {
                tr = new TypReh(this, rehijon, table);
                trs.put(rehijon, tr);
            }
            return tr;
        }
    }

    public class TypReh {
        Typ typ;
        String rehijon;
        ResultTable2 table;

        POWriter output = new POWriter();

        public TypReh(Typ typ, String rehijon, ResultTable2 table) throws Exception {
            this.typ = typ;
            this.rehijon = rehijon;
            this.table = table;
        }

        void add(IOsmObject obj, String namechanged) {
            String name = obj.getTag(nameTag);
            String nameru = obj.getTag(nameRuTag);
            String namebe = obj.getTag(nameBeTag);
            String nameint = obj.getTag(nameIntTag);

            output.add(namechanged, namebe, obj.getObjectCode());

            if (typ.translation != null) {
                ResultTableRow row = table.new ResultTableRow(rehijon, obj.getObjectCode(), name);
                row.setAttr("name", name, namechanged);
                row.setAttr("name:ru", nameru, namechanged);
                row.setAttr("name:be", namebe, typ.translation.getOrDefault(namechanged, namebe));
                row.setAttr("int_name", nameint,
                        Lat.lat(typ.translation.getOrDefault(namechanged, namebe), false));
                row.addChanged();
            }
        }

        public void write() {
            System.out.println("Save translation  " + typ.typ + " " + rehijon);
            try {
                // запісваем для перакладу
                File f = new File(Env.readProperty("translations.dir") + "/zychodniki/" + getFile() + ".po");
                output.write(f.getPath());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        String getFile() {
            return typ.typ.replaceAll("^/+", "") + "/" + rehijon.replaceAll("/+$", "");
        }
    }
}
