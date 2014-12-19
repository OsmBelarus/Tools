/**************************************************************************
 Some tools for OSM.

 Copyright (C) 2014 Aleś Bułojčyk <alex73mail@gmail.com>
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

package org.alex73.osm.validators.common;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;

/**
 * Выніковая табліца для angular з пераключэньнем рэгіёнаў.
 */
public class ResultTable2 {
    public static Locale BE = new Locale("be");
    public static Collator BEL = Collator.getInstance(BE);

    public final List<String> attributes;
    public final Map<String, List<ResultTableRow>> rows;

    int rowsCount;

    public ResultTable2(String... attributes) {
        this(Arrays.asList(attributes));
    }

    public ResultTable2(List<String> attributes) {
        this.attributes = Collections.unmodifiableList(attributes);
        rows = new HashMap<>();
    }

    public void add(ResultTableRow row) {
        rowsCount++;
        List<ResultTableRow> list = rows.get(row.rehijon);
        if (list == null) {
            list = new ArrayList<>();
            rows.put(row.rehijon, list);
        }
        list.add(row);
    }

    public int getObjectsCount() {
        return rowsCount;
    }

    public void sort() {
        for (List<ResultTableRow> list : rows.values()) {
            Collections.sort(list, new Comparator<ResultTableRow>() {
                @Override
                public int compare(ResultTableRow o1, ResultTableRow o2) {
                    int r = BEL.compare(o1.name, o2.name);
                    if (r == 0) {
                        r = o1.id.compareTo(o2.id);
                    }
                    return r;
                }
            });
        }
    }

    public void writeJS(String file) {
        OutTable o = new OutTable();
        for (Map.Entry<String, List<ResultTableRow>> en : rows.entrySet()) {
            OutRehijon or = new OutRehijon();
            or.rehijonName = en.getKey();
            for (ResultTableRow row : en.getValue()) {
                OutRow orr = new OutRow();
                orr.name = row.name;
                or.rows.add(orr);
            }
            o.rehijony.add(or);
        }

        Gson gson = new Gson();
        String json = gson.toJson(o);
        try {
            FileUtils.writeStringToFile(new File(file), json, "UTF-8");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static class OutTable {
        public List<OutRehijon> rehijony = new ArrayList<>();
    }

    public static class OutRehijon {
        public String rehijonName;
        public List<OutRow> rows = new ArrayList<>();
    }

    public static class OutRow {
        public String name;
        public String[] v;
    }

    public class ResultTableRow {
        public final String rehijon, id, name;
        public TagInfo[] tags;

        public ResultTableRow(String rehijon, String id, String name) {
            this.rehijon = rehijon;
            this.id = id;
            this.name = name;
            tags = new TagInfo[attributes.size()];
        }

        public void setAttr(String attrName, String oldValue, String newValue) {
            int pos = attributes.indexOf(attrName);
            if (pos < 0) {
                throw new RuntimeException("Няма атрыбута '" + attrName + "' у табліцы");
            }
            tags[pos] = new TagInfo(attrName, oldValue, newValue);
        }

        public boolean needChange() {
            for (TagInfo ti : tags) {
                if (ti != null && !StringUtils.equals(ti.oldValue, ti.newValue)) {
                    return true;
                }
            }
            return false;
        }

        public void addChanged() {
            if (needChange()) {
                add(this);
            }
        }
    }

    public static class TagInfo {
        public final String attrName, oldValue, newValue;

        public TagInfo(String attrName, String oldValue, String newValue) {
            this.attrName = attrName;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
    }
}
