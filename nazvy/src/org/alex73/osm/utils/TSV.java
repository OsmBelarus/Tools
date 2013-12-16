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

package org.alex73.osm.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;

public class TSV {
    char SEPARATOR;

    public TSV(char separator) {
        this.SEPARATOR = separator;
    }

    public <T> void saveCSV(String file, Class<T> clazz, List<T> data) throws Exception {
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
        try {
            Field[] fields = clazz.getFields();
            String[] fn = new String[fields.length];
            for (int i = 0; i < fields.length; i++) {
                XmlAttribute attr = fields[i].getAnnotation(XmlAttribute.class);
                fn[i] = attr != null ? attr.name() : fields[i].getName();
            }
            out.write(print(fn));
            for (T obj : data) {
                for (int i = 0; i < fields.length; i++) {
                    Object v = fields[i].get(obj);
                    fn[i] = v != null ? v.toString() : "";
                }
                out.write(print(fn));
            }
        } finally {
            out.close();
        }
    }

    String print(String[] f) {
        StringBuilder out = new StringBuilder(120);
        for (int i = 0; i < f.length; i++) {
            if (i > 0)
                out.append(SEPARATOR);
            if (f[i].indexOf(SEPARATOR) >= 0) {
                throw new RuntimeException("Data with separator: " + f[i]);
            }
            out.append(f[i]);
        }
        out.append('\n');
        return out.toString();
    }

    public <T> List<T> readCSV(String file, Class<T> clazz) throws Exception {
        List<T> result = new ArrayList<>();
        BufferedReader rd = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        boolean warn = false;
        try {
            String s;
            Field[] fields = clazz.getFields();
            rd.readLine();
            while ((s = rd.readLine()) != null) {
                String[] fs = split(s, SEPARATOR);
                if (fields.length != fs.length) {
                    if (!warn) {
                        System.err.println("Count of fields is not the same");
                        warn = true;
                    }
                }
                T obj = clazz.newInstance();
                for (int i = 0; i < fs.length; i++) {
                    if (fs[i].isEmpty()) {
                        fs[i] = null;
                    }
                    if (fs[i] != null && fields[i].getType().isAssignableFrom(long.class)) {
                        fields[i].setLong(obj, Long.parseLong(fs[i]));
                    } else if (fs[i] != null && fields[i].getType().isAssignableFrom(Long.class)) {
                        fields[i].set(obj, Long.parseLong(fs[i]));
                    } else {
                        fields[i].set(obj, fs[i]);
                    }
                }
                result.add(obj);
            }
        } finally {
            rd.close();
        }
        return result;
    }

    String[] split(String line, char c) {
        String[] r = new String[256];
        int idx = 0;
        int prev = -1;
        while (true) {
            int p = line.indexOf(c, prev + 1);
            if (p < 0)
                break;
            r[idx] = line.substring(prev + 1, p);
            idx++;
            prev = p;
        }
        r[idx] = line.substring(prev + 1);
        idx++;
        return Arrays.copyOf(r, idx);
    }
}
