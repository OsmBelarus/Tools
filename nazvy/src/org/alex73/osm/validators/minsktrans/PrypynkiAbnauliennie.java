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

package org.alex73.osm.validators.minsktrans;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alex73.osm.utils.Belarus;
import org.alex73.osm.utils.CSV;
import org.alex73.osm.utils.Env;
import org.alex73.osm.utils.OSM;
import org.alex73.osm.utils.VelocityOutput;
import org.alex73.osm.validators.common.Errors;
import org.alex73.osm.validators.common.ResultTable;
import org.alex73.osm.validators.common.ResultTable.ResultTableRow;
import org.alex73.osmemory.IOsmNode;
import org.alex73.osmemory.geometry.ExtendedRelation;
import org.alex73.osmemory.geometry.FastArea;
import org.apache.commons.lang.StringUtils;

/**
 * Абнаўленьне прыпынкаў з базы Мінсктранс.
 */
public class PrypynkiAbnauliennie {
    static Belarus osm;
    static FastArea Miensk;
    static Map<Integer, MinsktransStop> minsktrans = new HashMap<>();
    static Map<Integer, MinsktransStop> daviednik = new HashMap<>();
    static List<MinsktransStop> daviednikSave = new ArrayList<>();
    static Map<Long, IOsmNode> prypynkiBielarusi = new HashMap<>();
    static List<IOsmNode> prypynkiMiensku = new ArrayList<>();
    static Set<Long> osmNodesUsed = new HashSet<>();
    static IOsmNode found;

    static ResultTable tableNames;
    static Errors errors = new Errors();

    public static void main(String[] a) throws Exception {
        osm = new Belarus();
        Miensk = new FastArea(new ExtendedRelation(osm.getRelationById(59195), osm).getArea(), osm);

        read();
        readMap();
        tableNames = new ResultTable("name");
        short nameTag = osm.getTagsPack().getTagCode("name");

        // шукаем зьмененыя і выдаленыя
        for (MinsktransStop stop : daviednik.values()) {
            MinsktransStop stopOrig = minsktrans.get(stop.id);
            if (stopOrig == null) {
                errors.addError("ERROR: Ёсьць у даведніку, але ўжо няма ў Мінсктрансе прыпынку '" + stop
                        + "'. Прыбрана ў 0list-new.csv");
                daviednikSave.remove(stop);
                continue;
            }
            if (stop.lat == null) {
                stop.lat = stopOrig.lat;
            }
            if (stop.lon == null) {
                stop.lon = stopOrig.lon;
            }
            stop.city = stopOrig.city;
            stop.area = stopOrig.area;
            stop.street = stopOrig.street;
            stop.info = stopOrig.info;
            stop.stops = stopOrig.stops;
            stop.stopNum = stopOrig.stopNum;
            stop.pikas = stopOrig.pikas;

            stopOrig.name = stopOrig.name.trim().replace("  ", " ").replace("  ", " ");
            if (!StringUtils.equals(stop.name, stopOrig.name)) {
                errors.addError("WARNING: Зьмянілася назва прыпынку " + stop + " " + coord(stop) + " -> '"
                        + stopOrig.name + "'. Трэба выправіць у даведніку.");
            }
            if (stop.lat.doubleValue() != stopOrig.lat.doubleValue()
                    || stop.lon.doubleValue() != stopOrig.lon.doubleValue()) {
                errors.addError("ERROR: Зьмянілася месца прыпынку '" + stop + "': " + coord(stop) + " -> "
                        + OSM.coord(stopOrig.lat, stopOrig.lon));
            }
            if (stop.osmNodeId != null) {
                IOsmNode node = prypynkiBielarusi.get(stop.osmNodeId);
                if (node == null) {
                    errors.addError("ERROR: Прыпынак n" + stop.osmNodeId + " што пазначаны ў даведніку для "
                            + stop + " " + coord(stop)
                            + ", не існуе на мапе. Трэба выправіць мапу ці даведнік.", "n" + stop.osmNodeId);
                    continue;
                }
                // супадаюць
                String d = formatDistanceKm(node, stop);
                ResultTableRow row = tableNames.new ResultTableRow(node.getObjectCode(), stop.name + " (" + d
                        + ") " + coord(stop));
                row.setAttr("name", node.getTag(nameTag), stop.osmNameRu);
                // row.setAttr("name:be", node.getTag(namebeTag), mt.osmNameBe);
                if (row.needChange()) {
                    tableNames.rows.add(row);
                }
            }
        }
        // шукаем новыя
        for (MinsktransStop stopOrig : minsktrans.values()) {
            if (!daviednik.containsKey(stopOrig.id)) {
                errors.addError("WARNING: Ужо ёсьць у Мінсктрансе, але няма ў даведніку прыпынаку '"
                        + stopOrig + "'. Дададзена ў 0list-new.csv");
                daviednikSave.add(copy(stopOrig));
            }
        }

        // шукаем дублікаты OsmModeID
        for (MinsktransStop stop : daviednik.values()) {
            if (stop.osmNodeId != null) {
                if (osmNodesUsed.contains(stop.osmNodeId)) {
                    errors.addError("ERROR: OSM node n" + stop.osmNodeId
                            + " выкарыстоўваецца ў 2 прыпынках. Трэба выправіць у даведніку.", "n"
                            + stop.osmNodeId);
                }
                osmNodesUsed.add(stop.osmNodeId);
            }
        }

        // паказваем тыя што толькі ў даведніку але не на мапе
        for (MinsktransStop stop : daviednik.values()) {
            if (stop.osmNodeId != null) {
                continue;
            }
            if (stop.lat == 0 || stop.lon == 0) {
                errors.addError("WARNING: Няма прыпынку '" + stop + "' " + coord(stop)
                        + " на мапе. Трэба знайсьці і выправіць osm:NodeID у даведніку");
            } else {
                // шукаем на мапе побач
                found = null;
                findNearest(0.01, stop);
                findNearest(0.03, stop);
                findNearest(0.05, stop);
                findNearest(0.08, stop);
                findNearest(0.1, stop);
                if (found != null) {
                    errors.addError("WARNING: Няма прыпынку '" + stop + "' " + coord(stop)
                            + " на мапе. Трэба знайсьці і выправіць osm:NodeID у даведніку. Магчыма гэта "
                            + OSM.browse("n" + found.getId()) + " (адлегласьць: " + distanceKm(found, stop)
                            + ")");
                } else {
                    errors.addError("WARNING: Няма прыпынку '" + stop + "' " + coord(stop)
                            + " на мапе. Трэба знайсьці і выправіць osm:NodeID у даведніку.");
                }
            }
        }

        // паказваем тыя што на мапе але не ў даведніку
        for (IOsmNode p : prypynkiMiensku) {
            if (!osmNodesUsed.contains(p.getId())) {
                errors.addError(
                        "WARNING: Прыпынак "
                                + p
                                + "/"
                                + p.getTag("name", osm)
                                + " ёсьць у Менску на мапе, але няма ў даведніку Мінсктранса. Трэба знайсьці і выправіць osm:NodeID у даведніку.",
                        p);
            }
        }

        write();

        String out = Env.readProperty("out.dir") + "/prypynkiMiensk.html";
        VelocityOutput.output("org/alex73/osm/validators/minsktrans/prypynki.velocity", out, "table",
                tableNames, "errors", errors);
    }

    static String coord(MinsktransStop stop) {
        return OSM.coord(stop.lat, stop.lon);
    }

    static void read() throws Exception {
        String davDirectory = Env.readProperty("dav");

        List<MinsktransStop> data = new CSV(';').readCSV(davDirectory + "/../Minsktrans/stops.txt",
                MinsktransStop.class);
        String prevName = null;
        for (MinsktransStop stop : data) {
            // прапушчаныя назвы - гэта такія самыя як папярэднія
            if (stop.name == null) {
                stop.name = prevName;
            } else {
                prevName = stop.name;
            }
            stop.lat /= 100000.0;
            stop.lon /= 100000.0;
            if (minsktrans.containsKey(stop.id)) {
                errors.addError("ERROR: Больш за 1 прыпынак з ID=" + stop.id
                        + " у файле Мінсктрансу. Трэба спраўдзіцб файл Мінсктранса.");
            }
            minsktrans.put(stop.id, stop);
        }

        List<MinsktransStop> data2 = new CSV('\t').readCSV(davDirectory + "/../Minsktrans/0list.csv",
                MinsktransStop.class);
        for (MinsktransStop stop : data2) {
            if (daviednik.containsKey(stop.id)) {
                errors.addError("Больш за 1 прыпынак з ID=" + stop.id
                        + " у даведніку. Выправіць у даведніку.");
            }
            daviednik.put(stop.id, stop);
            daviednikSave.add(stop);
        }
    }

    static void write() throws Exception {
        String davDirectory = Env.readProperty("dav");
        new CSV('\t').saveCSV(davDirectory + "/../Minsktrans/0list-new.csv", MinsktransStop.class,
                daviednikSave);
    }

    static void readMap() throws Exception {
        short highwayTag = osm.getTagsPack().getTagCode("highway");
        short amenityTag = osm.getTagsPack().getTagCode("amenity");

        // Менск
        osm.byTag("highway", o -> o.isNode() && "bus_stop".equals(o.getTag(highwayTag)) && Miensk.covers(o),
                o -> prypynkiMiensku.add((IOsmNode) o));
        osm.byTag("amenity",
                o -> o.isNode() && "bus_station".equals(o.getTag(amenityTag)) && Miensk.covers(o),
                o -> prypynkiMiensku.add((IOsmNode) o));
        // Беларусь
        osm.byTag("highway", o -> o.isNode() && "bus_stop".equals(o.getTag(highwayTag)) && osm.contains(o),
                o -> prypynkiBielarusi.put(o.getId(), (IOsmNode) o));
        osm.byTag("amenity",
                o -> o.isNode() && "bus_station".equals(o.getTag(amenityTag)) && osm.contains(o),
                o -> prypynkiBielarusi.put(o.getId(), (IOsmNode) o));
    }

    static MinsktransStop copy(MinsktransStop o) {
        MinsktransStop r = new MinsktransStop();
        r.id = o.id;
        r.city = o.city;
        r.area = o.area;
        r.street = o.street;
        r.name = o.name;
        r.info = o.info;
        r.lon = o.lon;
        r.lat = o.lat;
        r.stops = o.stops;
        r.stopNum = o.stopNum;
        r.pikas = o.pikas;
        return r;
    }

    /**
     * Шукаем бліжэйшы прыпынак.
     */
    static void findNearest(double maxDistance, MinsktransStop stop) {
        if (found != null) {
            return;
        }

        double minDistance = Double.MAX_VALUE;
        for (IOsmNode n : prypynkiBielarusi.values()) {
            if (osmNodesUsed.contains(n.getId())) {
                continue;
            }
            double dist = distanceKm(n, stop);
            if (dist < minDistance && dist < maxDistance) {
                minDistance = dist;
                found = n;
            }
        }
    }

    /**
     * Адлегласьць прыпынку ў даведніку ад прыпынку на мапе.
     */
    static double distanceKm(IOsmNode node, MinsktransStop mt) {
        return osm.distanceKm(node.getLatitude(), node.getLongitude(), mt.lat, mt.lon);
    }

    /**
     * Адлегласьць у прыгожым выглядзе.
     */
    static String formatDistanceKm(IOsmNode node, MinsktransStop mt) {
        if (mt.lat == 0) {
            return "???";
        }
        double d = osm.distanceKm(node.getLatitude(), node.getLongitude(), mt.lat, mt.lon);
        return new DecimalFormat("#.###").format(d);
    }
}
