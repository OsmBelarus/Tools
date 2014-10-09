/**************************************************************************
 TMX writer

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
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Запісвае .tmx файл.
 */
@SuppressWarnings("serial")
public class TMX extends HashMap<String, String> {
    public static Locale BE = new Locale("be");
    public static Collator BEL = Collator.getInstance(BE);

    public void save(File file) throws Exception {
        List<String> keys = new ArrayList<>(keySet());
        Collections.sort(keys, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return BEL.compare(o1, o2);
            }
        });
        Writer wr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
        wr.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        wr.write("<!DOCTYPE tmx SYSTEM \"tmx11.dtd\">\n");
        wr.write("<tmx version=\"1.1\">\n");
        wr.write("  <header creationtool=\"own\" adminlang=\"EN-US\" datatype=\"plaintext\" segtype=\"paragraph\" srclang=\"RU\"/>\n");
        wr.write("  <body>\n");
        for (String r : keys) {
            wr.write("    <tu>\n");
            wr.write("      <tuv lang=\"RU\"><seg>" + r + "</seg></tuv>\n");
            wr.write("      <tuv lang=\"BE\"><seg>" + get(r) + "</seg></tuv>\n");
            wr.write("    </tu>\n");
        }
        wr.write("  </body>");
        wr.write("</tmx>");
        wr.close();
    }
}
