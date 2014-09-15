package org.alex73.osm.monitors.export;

import gen.alex73.osm.monitor.Attr;
import gen.alex73.osm.monitor.Group;
import gen.alex73.osm.monitor.Monitor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.alex73.osmemory.BaseObject2;
import org.alex73.osmemory.FastPolygon;
import org.alex73.osmemory.MemoryStorage2;
import org.alex73.osmemory.NodeObject2;
import org.alex73.osmemory.RelationObject2;
import org.alex73.osmemory.WayObject2;

public class MonitorContext {
    private final MemoryStorage2 osm;
    private final Monitor monitor;
    private final FastPolygon Belarus;
    private final List<BaseObject2> collected = new ArrayList<>();

    public MonitorContext(MemoryStorage2 osm, Monitor m, FastPolygon Belarus) throws Exception {
        this.osm = osm;
        this.monitor = m;
        this.Belarus = Belarus;
    }

    public void process(BaseObject2 o) {
        boolean dump = false;
        for (Group g : monitor.getGroup()) {
            if (isCorespondsGroup(o, g)) {
                if (Belarus.contains(o)) {
                    dump = true;
                }
                break;
            }
        }
        if (dump) {
            collected.add(o);
        }
    }

    public void dump(File outputDirectory) throws Exception {
        File outFile = new File(outputDirectory, monitor.getOutput() + ".txt");
        // sort
        if (monitor.getSort() != null) {
            Collections.sort(collected, new Comparator<BaseObject2>() {
                @Override
                public int compare(BaseObject2 o1, BaseObject2 o2) {
                    String v1 = osm.getTag(o1, monitor.getSort());
                    String v2 = osm.getTag(o2, monitor.getSort());
                    if (v1 == null) {
                        v1 = "\uFFFF";
                    }
                    if (v2 == null) {
                        v2 = "\uFFFF";
                    }
                    int r = v1.compareTo(v2);
                    if (r == 0) {
                        int t1 = getObjectTypePriority(o1);
                        int t2 = getObjectTypePriority(o2);
                        r = Integer.compare(t1, t2);
                    }
                    if (r == 0) {
                        r = Long.compare(o1.getId(), o2.getId());
                    }
                    return r;
                }
            });
        }

        OutputFormatter formatter = new OutputFormatter(osm);

        try (PrintStream wr = new PrintStream(new FileOutputStream(outFile), false, "UTF-8")) {
            for (Group g : monitor.getGroup()) {
                String o = "", oa = "";
                if (g.isInNodes()) {
                    o += ", nodes";
                }
                if (g.isInWays()) {
                    o += ", ways";
                }
                if (g.isInRelations()) {
                    o += ", relations";
                }
                o = o.substring(1);
                for (Attr a : g.getAttr()) {
                    oa += ", " + a.getName() + "=" + (a.getValue() != null ? a.getValue() : "<exist>");
                }
                oa = oa.substring(1);
                wr.println("#" + o + ":" + oa);
            }
            wr.println();

            for (BaseObject2 o : collected) {
                if (o instanceof NodeObject2) {
                    NodeObject2 n = (NodeObject2) o;
                    wr.println(formatter.objectName(n));
                    wr.println("  other names: " + formatter.otherNames(n));
                    wr.println("  other tags : " + formatter.otherTags(n));
                    wr.println("    geometry : " + formatter.getGeometry(n));
                } else if (o instanceof WayObject2) {
                    WayObject2 w = (WayObject2) o;
                    wr.println(formatter.objectName(w));
                    wr.println("  other names: " + formatter.otherNames(w));
                    wr.println("  other tags : " + formatter.otherTags(w));
                    wr.println("    geometry :" + formatter.getGeometry(w));
                } else if (o instanceof RelationObject2) {
                    RelationObject2 r = (RelationObject2) o;
                    wr.println(formatter.objectName(r));
                    wr.println("  other names: " + formatter.otherNames(r));
                    wr.println("  other tags : " + formatter.otherTags(r));
                    for (String g : formatter.getGeometry(r)) {
                        wr.println("    geometry : " + g);
                    }
                } else {
                    throw new RuntimeException();
                }
            }
        }
    }

    boolean isCorespondsGroup(BaseObject2 o, Group g) {
        if (o instanceof NodeObject2 && !g.isInNodes()) {
            return false;
        }
        if (o instanceof WayObject2 && !g.isInWays()) {
            return false;
        }
        if (o instanceof RelationObject2 && !g.isInRelations()) {
            return false;
        }
        for (Attr a : g.getAttr()) {
            String v = osm.getTag(o, a.getName());
            if (v == null) {
                return false;
            }
            if (a.getValue() != null && !a.getValue().equals(v)) {
                return false;
            }
        }
        return true;
    }

    int getObjectTypePriority(BaseObject2 o) {
        if (o instanceof NodeObject2) {
            return 1;
        } else if (o instanceof WayObject2) {
            return 2;
        } else if (o instanceof RelationObject2) {
            return 3;
        } else {
            throw new RuntimeException();
        }
    }
}
