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

import org.alex73.osmemory.IOsmNode;
import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.IOsmRelation;
import org.alex73.osmemory.IOsmWay;
import org.alex73.osmemory.MemoryStorage;
import org.alex73.osmemory.geometry.FastArea;

/**
 * Экспартуе зьвесткі для вызначанага тыпу аб'ектаў. 
 */
public class MonitorContext {
    private final MemoryStorage osm;
    private final Monitor monitor;
    private final FastArea area;
    private final List<IOsmObject> collected = new ArrayList<>();

    public MonitorContext(MemoryStorage osm, Monitor m, FastArea area) throws Exception {
        this.osm = osm;
        this.monitor = m;
        this.area = area;
    }

    public void process(IOsmObject o) {
        boolean dump = false;
        for (Group g : monitor.getGroup()) {
            if (isCorespondsGroup(o, g)) {
                if (area.covers(o)) {
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
        short sortTag = osm.getTagsPack().getTagCode(monitor.getSort());
        // sort
        if (monitor.getSort() != null) {
            Collections.sort(collected, new Comparator<IOsmObject>() {
                @Override
                public int compare(IOsmObject o1, IOsmObject o2) {
                    String v1 = o1.getTag(sortTag);
                    String v2 = o2.getTag(sortTag);
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

            for (IOsmObject o : collected) {
                switch (o.getType()) {
                case IOsmObject.TYPE_NODE:
                    IOsmNode n = (IOsmNode) o;
                    wr.println(formatter.objectName(n));
                    wr.println("  other names: " + formatter.otherNames(n));
                    wr.println("  other tags : " + formatter.otherTags(n));
                    wr.println("    geometry : " + formatter.getGeometry(n));
                    break;
                case IOsmObject.TYPE_WAY:
                    IOsmWay w = (IOsmWay) o;
                    wr.println(formatter.objectName(w));
                    wr.println("  other names: " + formatter.otherNames(w));
                    wr.println("  other tags : " + formatter.otherTags(w));
                    wr.println("    geometry :" + formatter.getGeometry(w));
                    break;
                case IOsmObject.TYPE_RELATION:
                    IOsmRelation r = (IOsmRelation) o;
                    wr.println(formatter.objectName(r));
                    wr.println("  other names: " + formatter.otherNames(r));
                    wr.println("  other tags : " + formatter.otherTags(r));
                    for (String g : formatter.getGeometry(r)) {
                        wr.println("    geometry : " + g);
                    }
                    break;
                default:
                    throw new RuntimeException();
                }
            }
        }
    }

    boolean isCorespondsGroup(IOsmObject o, Group g) {
        switch (o.getType()) {
        case IOsmObject.TYPE_NODE:
            if (!g.isInNodes()) {
                return false;
            }
            break;
        case IOsmObject.TYPE_WAY:
            if (!g.isInWays()) {
                return false;
            }
            break;
        case IOsmObject.TYPE_RELATION:
            if (!g.isInRelations()) {
                return false;
            }
            break;
        default:
            throw new RuntimeException();
        }
        for (Attr a : g.getAttr()) {
            String v = o.getTag(a.getName(), osm);
            if (v == null) {
                return false;
            }
            if (a.getValue() != null && !a.getValue().equals(v)) {
                return false;
            }
        }
        return true;
    }

    int getObjectTypePriority(IOsmObject o) {
        return o.getType();
    }
}
