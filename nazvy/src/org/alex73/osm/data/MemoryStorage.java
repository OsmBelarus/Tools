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

package org.alex73.osm.data;

import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alex73.osm.utils.Geo;

public class MemoryStorage {
    static final Pattern RE_OBJECT_CODE = Pattern.compile("([nwr])([0-9]+)");
    public static final long rel_Belarus = 79842;

    final public List<NodeObject> nodes = new ArrayList<>(9000000);
    final public List<WayObject> ways = new ArrayList<>(1400000);
    final public List<RelationObject> relations = new ArrayList<>(20000);
    public Area Belarus;

    void finishLoading() {
        if (nodes.size() < 7000000 || ways.size() < 900000 || relations.size() < 2000) {
            throw new RuntimeException("Not enough data for Belarus in .pbf file");
        }
        Collections.sort(nodes);
        Collections.sort(ways);
        Collections.sort(relations);

        Belarus = Geo.rel2area(this, rel_Belarus);
    }

    private static <T extends BaseObject> T getById(List<T> en, long id) {
        int low = 0;
        int high = en.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midvalue = en.get(mid).id;

            if (midvalue < id)
                low = mid + 1;
            else if (midvalue > id)
                high = mid - 1;
            else
                return en.get(mid);
        }
        return null;
    }

    public NodeObject getNodeById(long id) {
        return getById(nodes, id);
    }

    public WayObject getWayById(long id) {
        return getById(ways, id);
    }

    public RelationObject getRelationById(long id) {
        return getById(relations, id);
    }

    /**
     * Get object by code like n123, w456, r789.
     */
    public BaseObject getObject(String code) throws Exception {
        Matcher m = RE_OBJECT_CODE.matcher(code.trim());
        if (!m.matches()) {
            throw new Exception("Няправільны фарматы code: " + code);
        }
        long idl = Long.parseLong(m.group(2));
        switch (m.group(1)) {
        case "n":
            return getNodeById(idl);
        case "w":
            return getWayById(idl);
        case "r":
            return getRelationById(idl);
        default:
            throw new Exception("Няправільны фарматы code: " + code);
        }
    }

    public boolean isInsideBelarus(BaseObject o) {
        switch (o.getType()) {
        case NODE:
            return isInsideBelarus((NodeObject) o);
        case WAY:
            return isInsideBelarus((WayObject) o);
        case RELATION:
            return isInsideBelarus((RelationObject) o);
        default:
            throw new RuntimeException("Unknown type of object " + o.getCode());
        }
    }

    public boolean isInsideBelarus(NodeObject o) {
        return Geo.isInside(Belarus, Geo.node2point(this, o.id));
    }

    public boolean isInsideBelarus(WayObject o) {
        return Geo.isInside(Belarus, Geo.way2path(this, o.id));
    }

    public boolean isInsideBelarus(RelationObject o) {
        return Geo.isInside(Belarus, Geo.rel2area(this, o.id));
    }
}
