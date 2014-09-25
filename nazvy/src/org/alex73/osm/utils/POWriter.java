package org.alex73.osm.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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

public class POWriter {
    private Map<String, Info> data = new HashMap<>();

    public void add(String source, String translation, String objectCode) {
        if (source == null) {
            source = "<null>";
        }
        Info info = data.get(source);
        if (info == null) {
            info = new Info();
            data.put(source, info);
        }
        info.objects.add(objectCode);
        if (translation != null) {
            info.translations.add(translation);
        }
    }
    
    public Map<String, Info> getData() {
        return data;
    }

    public void write(String filename) throws IOException {
        File poFile = new File(filename);
        poFile.getParentFile().mkdirs();

        BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(poFile)));
        List<String> list = new ArrayList<>(data.keySet());
        Collections.sort(list, COMPARATOR);
        for (String u : list) {
            Info i = data.get(u);
            wr.write("# Objects : " + i.objects + "\n");
            wr.write("# name:be: " + i.translations + "\n");
            wr.write("msgid \"" + u + "\"\n");
            String t = "";
            wr.write("\n");
        }
        wr.close();
    }

    public static Comparator<String> COMPARATOR = new Comparator<String>() {
        Locale RU = new Locale("ru");
        Collator RUC = Collator.getInstance(RU);

        @Override
        public int compare(String o1, String o2) {
            return RUC.compare(o1, o2);
        }
    };

    static class Info {
        Set<String> objects = new TreeSet<>();
        Set<String> translations = new TreeSet<>();
    }
}
