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

package org.alex73.osm.monitors.export;

import java.text.DecimalFormat;
import java.util.Map;

import org.alex73.osmemory.IOsmNode;
import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.IOsmRelation;
import org.alex73.osmemory.IOsmWay;
import org.alex73.osmemory.MemoryStorage;
import org.alex73.osmemory.OsmBase;

/**
 * Стварае тэкставы файл.
 */
public class OutputFormatter {
    final MemoryStorage osm;
    final StringBuilder out = new StringBuilder(1024 * 1024);
    short tagName, tagIntName, tagnamebe, tagnamebetarask, tagnameru;

    public OutputFormatter(MemoryStorage osm) {
        this.osm = osm;
        tagName = osm.getTagsPack().getTagCode("name");
        tagIntName = osm.getTagsPack().getTagCode("int_name");
        tagnamebe = osm.getTagsPack().getTagCode("name:be");
        tagnamebetarask = osm.getTagsPack().getTagCode("name:be-tarask");
        tagnameru = osm.getTagsPack().getTagCode("name:ru");
    }

    public String getOutput() {
        return out.toString();
    }

    void newLine() {
        out.append('\n');
    }

    void objectName(IOsmObject o) {
        String name = o.getTag(tagName);
        String intname = o.getTag(tagIntName);
        String namebe = o.getTag(tagnamebe);
        String tarask = o.getTag(tagnamebetarask);
        String nameru = o.getTag(tagnameru);
        if (name == null && intname == null && namebe == null && tarask == null && nameru == null) {
            out.append(o.getObjectCode());
            return;
        }

        out.append(o.getObjectCode());
        out.append("  ");
        out.append(namebe);
        if (tarask != null) {
            out.append(" / ");
            out.append(tarask);
        }
        out.append(" / ");
        out.append(intname);
        out.append(" / ");
        out.append(name);

        if (nameru != null) {
            out.append(" / ");
            out.append(nameru);
        }
    }

    void otherNames(IOsmObject o) {
        Map<String, String> tags = o.extractTags(osm);
        boolean first = true;
        for (String t : tags.keySet()) {
            switch (t) {
            case "name":
            case "name:be":
            case "name:be-tarask":
            case "name:ru":
            case "int_name":
                break;
            default:
                if (t.startsWith("name:")) {
                    if (first) {
                        out.append("  other names: ");
                        first = false;
                    } else {
                        out.append(" | ");
                    }
                    out.append(t);
                    out.append('=');
                    out.append(tags.get(t));
                }
                break;
            }
        }
        if (!first) {
            out.append('\n');
        }
    }

    void otherTags(IOsmObject o) {
        out.append("  other tags : ");
        Map<String, String> tags = o.extractTags(osm);
        boolean first = true;
        for (String t : tags.keySet()) {
            if (t.equals("name") || t.startsWith("name:") || t.startsWith("int_name")) {
                continue;
            }
            if (first) {
                first = false;
            } else {
                out.append(" | ");
            }
            out.append(t);
            out.append('=');
            out.append(tags.get(t));
        }
        out.append('\n');
    }

    void getGeometry(IOsmNode n) {
        out.append("    geometry : ");
        addCoord(n);
        out.append('\n');
    }

    void getGeometry(IOsmWay w) {
        out.append("    geometry :");
        for (long nid : w.getNodeIds()) {
            out.append(' ');
            out.append(IOsmObject.getNodeCode(nid));
            addCoord(osm.getNodeById(nid));
        }
        out.append('\n');
    }

    void getGeometry(IOsmRelation r) {
        for (int i = 0; i < r.getMembersCount(); i++) {
            out.append("    geometry : ");
            out.append('<');
            out.append(r.getMemberRole(osm, i));
            out.append('>');
            out.append(' ');

            long mid = r.getMemberID(i);

            switch (r.getMemberType(i)) {
            case IOsmObject.TYPE_NODE:
                out.append(IOsmObject.getNodeCode(mid));
                IOsmNode n = osm.getNodeById(mid);
                addCoord(n);
                if (n != null) {
                    out.append(" : ");
                    objectName(n);
                }
                break;
            case IOsmObject.TYPE_WAY:
                // outer way
                IOsmWay w = osm.getWayById(mid);
                out.append(IOsmObject.getWayCode(mid));
                out.append(':');
                if (w != null) {
                    for (long nid : w.getNodeIds()) {
                        out.append(' ');
                        out.append(IOsmObject.getNodeCode(nid));
                        addCoord(osm.getNodeById(nid));
                    }
                    out.append(" : ");
                    objectName(w);
                } else {
                    out.append(" [???]");
                }
                break;
            case IOsmObject.TYPE_RELATION:
                IOsmRelation r2 = osm.getRelationById(mid);
                out.append(IOsmObject.getRelationCode(mid));
                if (r2 != null) {
                    out.append(": [ ");
                    for (int j = 0; j < r2.getMembersCount(); j++) {
                        out.append(r2.getMemberRole(osm, j));
                        out.append(' ');
                        switch (r2.getMemberType(j)) {
                        case OsmBase.TYPE_NODE:
                            out.append(IOsmObject.getNodeCode(r2.getMemberID(j)));
                            break;
                        case OsmBase.TYPE_WAY:
                            out.append(IOsmObject.getWayCode(r2.getMemberID(j)));
                            break;
                        case OsmBase.TYPE_RELATION:
                            out.append(IOsmObject.getRelationCode(r2.getMemberID(j)));
                            break;
                        }
                        out.append(", ");
                    }
                    out.append("] : ");
                    objectName(r2);
                } else {
                    out.append(" [???]");
                }
                break;
            }

            out.append('\n');
        }
    }

    static DecimalFormat COORD_FORMAT = new DecimalFormat("##0.00######");

    void addCoord(IOsmNode n) {
        if (n != null) {
            out.append('[');
            String lat = Integer.toString(n.getLat());
            out.append(lat, 0, 2).append('.').append(lat, 2, lat.length());
            out.append(',');
            String lon = Integer.toString(n.getLon());
            out.append(lon, 0, 2).append('.').append(lon, 2, lon.length());
            out.append(']');
        } else {
            out.append("[???]");
        }
    }
}
