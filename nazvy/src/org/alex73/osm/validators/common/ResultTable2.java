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

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * Выніковая табліца для angular з пераключэньнем рэгіёнаў.
 */
public class ResultTable2 {
    public static Locale BE = new Locale("be");
    public static Collator BEL = Collator.getInstance(BE);

    private final List<String> attributes;
    private final List<Region> regions = new ArrayList<>();
    private int rowsCount;

    private transient final Map<String, Region> regionsByName = new HashMap<>();

    public ResultTable2(String... attributes) {
        this(Arrays.asList(attributes));
    }

    public ResultTable2(List<String> attributes) {
        this.attributes = Collections.unmodifiableList(attributes);
    }

    private Region getRegion(String region) {
        Region r = regionsByName.get(region);
        if (r == null) {
            r = new Region(region);
            regions.add(r);
            regionsByName.put(region, r);
        }
        return r;
    }

    public void sort() {
        Collections.sort(regions, new Comparator<Region>() {
            @Override
            public int compare(Region o1, Region o2) {
                return BEL.compare(o1.name, o2.name);
            }
        });
        for (Region r : regions) {
            Collections.sort(r.rows, new Comparator<ResultTableRow>() {
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
    
    public int getRowsCount() {
        return rowsCount;
    }

    public Object getJS() {
        return this;
    }

    public class Region {
        public final String name;
        public final List<ResultTableRow> rows = new ArrayList<>();

        public Region(String name) {
            this.name = name;
        }
    }

    public class ResultTableRow {
        private final transient String region;
        public final String id, name;
        public TagInfo[] tags;

        public ResultTableRow(String region, String id, String name) {
            this.region = region;
            this.id = id;
            this.name = name;
            tags = new TagInfo[attributes.size()];
        }

        public void setAttr(String attrName, String oldValue, String newValue) {
            int pos = attributes.indexOf(attrName);
            if (pos < 0) {
                throw new RuntimeException("Няма атрыбута '" + attrName + "' у табліцы");
            }
            tags[pos] = new TagInfo(oldValue, newValue);
        }

        public boolean needChange() {
            for (TagInfo ti : tags) {
                if (ti != null && !StringUtils.equals(ti.ov, ti.nv)) {
                    return true;
                }
            }
            return false;
        }

        public void addChanged() {
            if (needChange()) {
                rowsCount++;
                getRegion(region).rows.add(this);
            }
        }
    }

    public static class TagInfo {
        public final String ov;
        public final String nv;

        public TagInfo(String oldValue, String newValue) {
            this.ov = oldValue == null ? "" : oldValue;
            this.nv = newValue == null ? "" : newValue;
        }
    }
}
