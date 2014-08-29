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

import org.alex73.osm.data.BaseObject;
import org.alex73.osm.data.MemoryStorage;
import org.alex73.osm.data.NodeObject;
import org.alex73.osm.data.RelationObject;
import org.alex73.osm.data.WayObject;

public class MonitorContext {
    private final Monitor monitor;
    private final File outFile;
    private final List<BaseObject> collected = new ArrayList<>();

    public MonitorContext(Monitor m, File outputDirectory) throws Exception {
        this.monitor = m;
        outFile = new File(outputDirectory, m.getOutput() + ".txt");
    }

    public void process(BaseObject o) {
        boolean dump = false;
        for (Group g : monitor.getGroup()) {
            if (isCorespondsGroup(o, g)) {
                dump = true;
                break;
            }
        }
        if (dump) {
            collected.add(o);
        }
    }

    public void dump(MemoryStorage osm) throws Exception {
        // sort
        if (monitor.getSort() != null) {
            Collections.sort(collected, new Comparator<BaseObject>() {
                @Override
                public int compare(BaseObject o1, BaseObject o2) {
                    String v1 = o1.getTag(monitor.getSort());
                    String v2 = o2.getTag(monitor.getSort());
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
                        r = Long.compare(o1.id, o2.id);
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

            for (BaseObject o : collected) {
                if (o instanceof NodeObject) {
                    NodeObject n = (NodeObject) o;
                    wr.println(formatter.objectName(n));
                    wr.println("  other names: " + formatter.otherNames(n));
                    wr.println("  other tags : " + formatter.otherTags(n));
                    wr.println("    geometry : " + formatter.getGeometry(n));
                } else if (o instanceof WayObject) {
                    WayObject w = (WayObject) o;
                    wr.println(formatter.objectName(w));
                    wr.println("  other names: " + formatter.otherNames(w));
                    wr.println("  other tags : " + formatter.otherTags(w));
                    wr.println("    geometry :" + formatter.getGeometry(w));
                } else if (o instanceof RelationObject) {
                    RelationObject r = (RelationObject) o;
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

    static boolean isCorespondsGroup(BaseObject o, Group g) {
        if (o instanceof NodeObject && !g.isInNodes()) {
            return false;
        }
        if (o instanceof WayObject && !g.isInWays()) {
            return false;
        }
        if (o instanceof RelationObject && !g.isInRelations()) {
            return false;
        }
        for (Attr a : g.getAttr()) {
            String v = o.getTag(a.getName());
            if (v == null) {
                return false;
            }
            if (a.getValue() != null && !a.getValue().equals(v)) {
                return false;
            }
        }
        return true;
    }

    static int getObjectTypePriority(BaseObject o) {
        if (o instanceof NodeObject) {
            return 1;
        } else if (o instanceof WayObject) {
            return 2;
        } else if (o instanceof RelationObject) {
            return 3;
        } else {
            throw new RuntimeException();
        }
    }
}
