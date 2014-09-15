package org.alex73.osm.monitors.export;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alex73.osm.data.NodeObject;
import org.alex73.osm.data.RelationObject;
import org.alex73.osm.data.WayObject;
import org.alex73.osmemory.BaseObject2;
import org.alex73.osmemory.MemoryStorage2;
import org.alex73.osmemory.NodeObject2;
import org.alex73.osmemory.RelationObject2;
import org.alex73.osmemory.WayObject2;

public class OutputFormatter {
    final MemoryStorage2 osm;

    public OutputFormatter(MemoryStorage2 osm) {
        this.osm = osm;
    }

    String objectName(BaseObject2 o) {
        StringBuilder out = new StringBuilder(200);
        out.append(o.getCode());
        out.append("  ");
        out.append(osm.getTag(o, "name:be"));
        String tarask = osm.getTag(o, "name:be-tarask");
        if (tarask != null) {
            out.append('/');
            out.append(tarask);
        }
        out.append('/');
        out.append(osm.getTag(o, "int_name"));
        out.append('/');
        out.append(osm.getTag(o, "name"));
        return out.toString();
    }

    String otherNames(BaseObject2 o) {
        StringBuilder out = new StringBuilder(200);
        Map<String, String> tags = osm.extractTags(o);
        for (String t : tags.keySet()) {
            switch (t) {
            case "name":
            case "name:be":
            case "int_name":
                break;
            default:
                if (t.startsWith("name:")) {
                    out.append(';');
                    out.append(t);
                    out.append('=');
                    out.append(tags.get(t));
                }
                break;
            }
        }
        return out.length() > 0 ? out.substring(1) : "";
    }

    String otherTags(BaseObject2 o) {
        StringBuilder out = new StringBuilder(200);
        Map<String, String> tags = osm.extractTags(o);
        for (String t : tags.keySet()) {
            if (t.equals("name") || t.startsWith("name:") || t.startsWith("int_name")) {
                continue;
            }
            out.append(';');
            out.append(t);
            out.append('=');
            out.append(tags.get(t));
        }
        return out.substring(1);
    }

    String getGeometry(NodeObject2 n) {
        StringBuilder out = new StringBuilder(200);
        addCoord(n, out);
        return out.toString();
    }

    String getGeometry(WayObject2 w) {
        StringBuilder out = new StringBuilder(200);
        for (long nid : w.nodeIds) {
            out.append(' ');
            out.append(NodeObject.getCode(nid));
            addCoord(osm.getNodeById(nid), out);
        }
        return out.toString();
    }

    List<String> getGeometry(RelationObject2 r) {
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < r.getMembersCount(); i++) {
            StringBuilder out = new StringBuilder(200);
            out.append('<');
            out.append(r.getMemberRole(osm, i));
            out.append('>');
            out.append(' ');

            long mid = r.getMemberID(i);

            switch (r.getMemberType(i)) {
            case BaseObject2.TYPE_NODE:
                out.append(NodeObject.getCode(mid));
                NodeObject2 n = osm.getNodeById(mid);
                addCoord(n, out);
                if (n != null) {
                    out.append(" : ");
                    out.append(objectName(n));
                }
                break;
            case BaseObject2.TYPE_WAY:
                // outer way
                WayObject2 w = osm.getWayById(mid);
                out.append(WayObject.getCode(mid));
                out.append(':');
                if (w != null) {
                    for (long nid : w.nodeIds) {
                        out.append(' ');
                        out.append(NodeObject.getCode(nid));
                        addCoord(osm.getNodeById(nid), out);
                    }
                    out.append(" : ");
                    out.append(objectName(w));
                } else {
                    out.append(" [???]");
                }
                break;
            case BaseObject2.TYPE_RELATION:
                RelationObject2 r2 = osm.getRelationById(mid);
                out.append(RelationObject.getCode(mid));
                if (r2 != null) {
                    out.append(": [ ");
                    for (int j = 0; i < r2.getMembersCount(); j++) {
                        out.append(r2.getMemberRole(osm, j));
                        out.append(' ');
                        switch (r2.getMemberType(i)) {
                        case BaseObject2.TYPE_NODE:
                            out.append(NodeObject2.getCode(r2.getMemberID(j)));
                            break;
                        case BaseObject2.TYPE_WAY:
                            out.append(WayObject2.getCode(r2.getMemberID(j)));
                            break;
                        case BaseObject2.TYPE_RELATION:
                            out.append(RelationObject2.getCode(r2.getMemberID(j)));
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

    void addCoord(NodeObject2 n, StringBuilder str) {
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
