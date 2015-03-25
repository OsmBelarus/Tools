/**************************************************************************
 PO file writer

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

/**
 * Запісывае .po файл.
 */
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

        BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(poFile), "UTF-8"));
        List<String> list = new ArrayList<>(data.keySet());
        Collections.sort(list, COMPARATOR);
        for (String u : list) {
            Info i = data.get(u);
            wr.write("# Objects : " + i.objects + "\n");
            wr.write("# name:be: " + i.translations + "\n");
            wr.write("msgid \"" + u + "\"\n");
            if (i.translations.size() == 1) {
                //wr.write("msgstr \"" + i.translations.iterator().next() + "\"");
                wr.write("msgstr \"\"");
            } else {
                wr.write("msgstr \"\"");
            }
            wr.write("\n\n");
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
