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

package org.alex73.osm.validators.harady;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;

/**
 * Выніковая табліца для выпраўленьня тэгаў OSM.
 */
public class ResultTable {
    public static Locale BE = new Locale("be");
    public static Collator BEL = Collator.getInstance(BE);

    public final List<String> attributes;
    public final List<ResultTableRow> rows;

    public ResultTable(String... attributes) {
        this.attributes = Collections.unmodifiableList(Arrays.asList(attributes));
        rows = new ArrayList<>();
    }

    public ResultTable(List<String> attributes) {
        this.attributes = Collections.unmodifiableList(attributes);
        rows = new ArrayList<>();
    }

    public void sort() {
        Collections.sort(rows, new Comparator<ResultTableRow>() {
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

    public class ResultTableRow {
        public final String id, name;
        public TagInfo[] tags;

        public ResultTableRow(String id, String name) {
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
