package org.alex73.osm.monitors.export;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.alex73.osm.data.BaseObject;
import org.alex73.osm.data.MemoryStorage;
import org.alex73.osm.data.NodeObject;
import org.alex73.osm.data.RelationObject;
import org.alex73.osm.data.WayObject;

public class OutputFormatter {
    final MemoryStorage osm;

    public OutputFormatter(MemoryStorage osm) {
        this.osm = osm;
    }

    String objectName(BaseObject o) {
        StringBuilder out = new StringBuilder(200);
        out.append(o.getCode());
        out.append("  ");
        out.append(o.getTag("name:be"));
        String tarask = o.getTag("name:be-tarask");
        if (tarask != null) {
            out.append('/');
            out.append(tarask);
        }
        out.append('/');
        out.append(o.getTag("int_name"));
        out.append('/');
        out.append(o.getTag("name"));
        return out.toString();
    }

    String otherNames(BaseObject o) {
        StringBuilder out = new StringBuilder(200);
        for (String t : o.getTagNames()) {
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
                    out.append(o.getTag(t));
                }
                break;
            }
        }
        return out.length() > 0 ? out.substring(1) : "";
    }

    String otherTags(BaseObject o) {
        StringBuilder out = new StringBuilder(200);
        for (String t : o.getTagNames()) {
            if (t.equals("name") || t.startsWith("name:") || t.startsWith("int_name")) {
                continue;
            }
            out.append(';');
            out.append(t);
            out.append('=');
            out.append(o.getTag(t));
        }
        return out.substring(1);
    }

    String getGeometry(NodeObject n) {
        StringBuilder out = new StringBuilder(200);
        addCoord(n, out);
        return out.toString();
    }

    String getGeometry(WayObject w) {
        StringBuilder out = new StringBuilder(200);
        for (long nid : w.nodeIds) {
            out.append(' ');
            out.append(NodeObject.getCode(nid));
            addCoord(osm.getNodeById(nid), out);
        }
        return out.toString();
    }

    List<String> getGeometry(RelationObject r) {
        List<String> result = new ArrayList<String>();
        for (RelationObject.Member m : r.members) {
            StringBuilder out = new StringBuilder(200);
            out.append('<');
            out.append(m.role);
            out.append('>');
            out.append(' ');

            switch (m.type) {
            case NODE:
                out.append(NodeObject.getCode(m.id));
                NodeObject n = osm.getNodeById(m.id);
                addCoord(n, out);
                if (n != null) {
                    out.append(" : ");
                    out.append(objectName(n));
                }
                break;
            case WAY:
                // outer way
                WayObject w = osm.getWayById(m.id);
                out.append(WayObject.getCode(m.id));
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
            case RELATION:
                RelationObject r2 = osm.getRelationById(m.id);
                out.append(RelationObject.getCode(m.id));
                if (r2 != null) {
                    out.append(": [ ");
                    for (RelationObject.Member m2 : r2.members) {
                        out.append(m2.role);
                        out.append(' ');
                        switch (m2.type) {
                        case NODE:
                            out.append(NodeObject.getCode(m2.id));
                            break;
                        case WAY:
                            out.append(WayObject.getCode(m2.id));
                            break;
                        case RELATION:
                            out.append(RelationObject.getCode(m2.id));
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

    void addCoord(NodeObject n, StringBuilder str) {
        if (n != null) {
            str.append('[');
            str.append(COORD_FORMAT.format(n.lat));
            str.append(',');
            str.append(COORD_FORMAT.format(n.lon));
            str.append(']');
        } else {
            str.append("[???]");
        }
    }
}
