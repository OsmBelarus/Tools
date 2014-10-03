package org.alex73.osm.monitors.export;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alex73.osmemory.IOsmNode;
import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.IOsmRelation;
import org.alex73.osmemory.IOsmWay;
import org.alex73.osmemory.MemoryStorage;
import org.alex73.osmemory.OsmBase;

public class OutputFormatter {
    final MemoryStorage osm;

    public OutputFormatter(MemoryStorage osm) {
        this.osm = osm;
    }

    String objectName(IOsmObject o) {
        StringBuilder out = new StringBuilder(200);
        out.append(o.getObjectCode());
        out.append("  ");
        out.append(o.getTag("name:be", osm));
        String tarask = o.getTag("name:be-tarask", osm);
        if (tarask != null) {
            out.append(" / ");
            out.append(tarask);
        }
        out.append(" / ");
        out.append(o.getTag("int_name", osm));
        out.append(" / ");
        out.append(o.getTag("name", osm));
        return out.toString();
    }

    String otherNames(IOsmObject o) {
        StringBuilder out = new StringBuilder(200);
        Map<String, String> tags = o.extractTags(osm);
        for (String t : tags.keySet()) {
            switch (t) {
            case "name":
            case "name:be":
            case "int_name":
                break;
            default:
                if (t.startsWith("name:")) {
                    out.append(" | ");
                    out.append(t);
                    out.append('=');
                    out.append(tags.get(t));
                }
                break;
            }
        }
        return out.length() > 0 ? out.substring(3) : "";
    }

    String otherTags(IOsmObject o) {
        StringBuilder out = new StringBuilder(200);
        Map<String, String> tags = o.extractTags(osm);
        for (String t : tags.keySet()) {
            if (t.equals("name") || t.startsWith("name:") || t.startsWith("int_name")) {
                continue;
            }
            out.append(" | ");
            out.append(t);
            out.append('=');
            out.append(tags.get(t));
        }
        return out.length() > 0 ? out.substring(3) : "";
    }

    String getGeometry(IOsmNode n) {
        StringBuilder out = new StringBuilder(200);
        addCoord(n, out);
        return out.toString();
    }

    String getGeometry(IOsmWay w) {
        StringBuilder out = new StringBuilder(200);
        for (long nid : w.getNodeIds()) {
            out.append(' ');
            out.append(IOsmObject.getNodeCode(nid));
            addCoord(osm.getNodeById(nid), out);
        }
        return out.toString();
    }

    List<String> getGeometry(IOsmRelation r) {
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < r.getMembersCount(); i++) {
            StringBuilder out = new StringBuilder(200);
            out.append('<');
            out.append(r.getMemberRole(osm, i));
            out.append('>');
            out.append(' ');

            long mid = r.getMemberID(i);

            switch (r.getMemberType(i)) {
            case IOsmObject.TYPE_NODE:
                out.append(IOsmObject.getNodeCode(mid));
                IOsmNode n = osm.getNodeById(mid);
                addCoord(n, out);
                if (n != null) {
                    out.append(" : ");
                    out.append(objectName(n));
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
                        addCoord(osm.getNodeById(nid), out);
                    }
                    out.append(" : ");
                    out.append(objectName(w));
                } else {
                    out.append(" [???]");
                }
                break;
            case IOsmObject.TYPE_RELATION:
                IOsmRelation r2 = osm.getRelationById(mid);
                out.append(IOsmObject.getRelationCode(mid));
                if (r2 != null) {
                    out.append(": [ ");
                    for (int j = 0; i < r2.getMembersCount(); j++) {
                        out.append(r2.getMemberRole(osm, j));
                        out.append(' ');
                        switch (r2.getMemberType(i)) {
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
                    out.append(objectName(r2));
                } else {
                    out.append(" [???]");
                }
                break;
            }

            result.add(out.toString());
        }
        return result;
    }

    static DecimalFormat COORD_FORMAT = new DecimalFormat("##0.00######");

    void addCoord(IOsmNode n, StringBuilder str) {
        if (n != null) {
            str.append('[');
            str.append(COORD_FORMAT.format(n.getLatitude()));
            str.append(',');
            str.append(COORD_FORMAT.format(n.getLongitude()));
            str.append(']');
        } else {
            str.append("[???]");
        }
    }
}
