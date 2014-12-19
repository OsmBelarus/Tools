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

import org.alex73.osm.monitors.export.Borders;
import org.alex73.osm.utils.Belarus;
import org.alex73.osm.utils.CSV;
import org.alex73.osm.utils.POReader;
import org.alex73.osm.utils.POWriter;
import org.alex73.osm.utils.RehijonTypeSeparator;
import org.alex73.osm.validators.common.ResultTable2;
import org.alex73.osm.validators.common.ResultTable2.ResultTableRow;
import org.alex73.osmemory.IOsmObject;
import org.apache.commons.lang.StringUtils;

public class TranslationProcessor extends RehijonTypeSeparator {
    Map<String, TypReh> trs = new HashMap<>();
    short nameTag, nameBeTag;
    Map<String, ResultTable2> tables = new HashMap<>();

    public TranslationProcessor(Belarus osm, Borders borders) throws Exception {
        super(osm, borders);
        nameTag = osm.getTagsPack().getTagCode("name");
        nameBeTag = osm.getTagsPack().getTagCode("name:be");
    }

    @Override
    synchronized protected void storeToOutput(IOsmObject obj, String typ, String rehijon) throws Exception {
        String name = obj.getTag(nameTag);
        if (StringUtils.isBlank(name)) {// TODO set conversion for spaces -> empty
            return;
        }

        TypReh tr = getTypeReh(typ, rehijon);
        tr.add(obj);
    }

    synchronized void save() throws Exception {
        trs.values().forEach(tr -> tr.write());
        tables.values().forEach(v -> v.sort());
        tables.forEach((k, v) -> v.writeJS("/tmp/tr/" + k + ".js"));
    }

    synchronized TypReh getTypeReh(String typ, String rehijon) throws Exception {
        String key = getKey(typ, rehijon);
        TypReh tr = trs.get(key);
        if (tr == null) {

            ResultTable2 table = tables.get(typ);
            if (table == null) {
                table = new ResultTable2("name", "name:be");
                tables.put(typ, table);
            }
            tr = new TypReh(typ, rehijon, table);
            trs.put(key, tr);

        }
        return tr;
    }

    public class TypReh {
        String typ;
        String rehijon;
        ResultTable2 table;

        POReader translated;
        Map<String, String> fixes;

        POWriter output = new POWriter();

        public TypReh(String typ, String rehijon, ResultTable2 table) throws Exception {
            this.typ = typ;
            this.rehijon = rehijon;
            this.table = table;

            // чытаем пераклады
            File f = new File("/tmp/tr/target/" + getFile() + ".po");
            if (f.exists()) {
                translated = new POReader(f.getPath());
            }

            // чытаем мэпінг выпраўленьняў
            fixes = new HashMap<>();
            try {
                List<Replace> replaces = new CSV('\t').readCSV("/tmp/tr/source/" + getFile() + ".csv",
                        Replace.class);
                for (Replace r : replaces) {
                    if (!r.from.equals(r.to)) {// толькі тыя што не супадаюць
                        fixes.put(r.from, r.to);
                    }
                }
            } catch (FileNotFoundException ex) {
            }
        }

        void add(IOsmObject obj) {
            String name = obj.getTag(nameTag);
            String namebe = obj.getTag(nameBeTag);

            String namechanged = fixes.get(name);
            if (namechanged == null) {
                fixes.put(name, name);
                namechanged = name;
            }

            output.add(namechanged, namebe, obj.getObjectCode());

            ResultTableRow row = table.new ResultTableRow(rehijon, obj.getObjectCode(), name);
            row.setAttr("name", name, namechanged);
            row.setAttr("name:be", namebe, translated.getOrDefault(namechanged, namebe));
            row.addChanged();
        }

        public void write() {
            System.out.println("Save translation  " + typ + " " + rehijon);
            try {
                // запісваем для перакладу
                File f = new File("/tmp/tr/source/" + getFile() + ".po");
                output.write(f.getPath());

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
                new CSV('\t').saveCSV("/tmp/tr/source/" + getFile() + ".csv", Replace.class, names);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        String getFile() {
            return typ.replaceAll("^/+", "") + "/" + rehijon.replaceAll("/+$", "");
        }
    }

    static String getKey(String typ, String rehijon) {
        return typ + "|" + rehijon;
    }
}
