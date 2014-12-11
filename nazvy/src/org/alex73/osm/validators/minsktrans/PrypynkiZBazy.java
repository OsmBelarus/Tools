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
import java.util.List;
import java.util.Map;

import org.alex73.osm.utils.Belarus;
import org.alex73.osm.utils.CSV;
import org.alex73.osm.utils.Env;
import org.alex73.osm.utils.VelocityOutput;
import org.alex73.osm.validators.common.Errors;
import org.alex73.osm.validators.common.ResultTable;
import org.alex73.osm.validators.common.ResultTable.ResultTableRow;
import org.alex73.osmemory.IOsmNode;
import org.alex73.osmemory.geometry.ExtendedRelation;
import org.alex73.osmemory.geometry.FastArea;

/**
 * Валідатар прыпынкаў Менску.
 */
public class PrypynkiZBazy {

    static Belarus osm;
    static FastArea Miensk;

    static List<MinsktransStop> minsktrans;
    static Map<Long, IOsmNode> map = new HashMap<>();
    static ResultTable table;
    static Errors errors = new Errors();

    public static void main(String[] args) throws Exception {
        String davDirectory = Env.readProperty("dav");
        minsktrans = new CSV('\t').readCSV(davDirectory + "/../Minsktrans/0list.csv", MinsktransStop.class);

        osm = new Belarus();
        Miensk = new FastArea(new ExtendedRelation(osm.getRelationById(59195), osm).getArea(), osm);

        readMap();

        table = new ResultTable("name", "name:be");
        compare();
        none();

        String out = Env.readProperty("out.dir") + "/prypynkiMiensk.html";
        VelocityOutput.output("org/alex73/osm/validators/minsktrans/prypynki.velocity", out, "table", table,
                "errors", errors);
    }

    static void compare() {
        short nameTag = osm.getTagsPack().getTagCode("name");
        short namebeTag = osm.getTagsPack().getTagCode("name:be");
        for (MinsktransStop mt : new ArrayList<>(minsktrans)) {
            if (mt.osmNodeId == null) {
                if (mt.lat == 0) {
                    errors.addError("Няма прыпынку '" + mt.name + "' на мапе");
                } else {
                    errors.addError("Няма прыпынку '" + mt.name
                            + "' на <a href='https://www.openstreetmap.org/#map=18/" + mt.lat + "/" + mt.lon
                            + "'>мапе</a>");
                }
                continue;
            }
            IOsmNode node = map.remove(mt.osmNodeId);
            if (node == null) {
                errors.addError("Няма прыпынку '" + mt.name
                        + "' на <a href='https://www.openstreetmap.org/#map=18/" + mt.lat + "/" + mt.lon
                        + "'>мапе</a>");
                continue;
            }

            String d = formatDistanceKm(node, mt);
            ResultTableRow row = table.new ResultTableRow(node.getObjectCode(), mt.name + " (" + d + ")");
            row.setAttr("name", node.getTag(nameTag), mt.osmNameRu);
            row.setAttr("name:be", node.getTag(namebeTag), mt.osmNameBe);
            table.rows.add(row);
        }
    }

    static void none() {
        short nameTag = osm.getTagsPack().getTagCode("name");
        for (IOsmNode n : map.values()) {
            errors.addError("Няма прыпынку '" + n.getTag(nameTag) + "' у табліцы Мінсктранса", n);
        }
    }

    static void readMap() throws Exception {
        short highwayTag = osm.getTagsPack().getTagCode("highway");
        short amenityTag = osm.getTagsPack().getTagCode("amenity");
        osm.byTag("highway", o -> o.isNode() && "bus_stop".equals(o.getTag(highwayTag)) && Miensk.covers(o),
                o -> map.put(o.getId(), (IOsmNode) o));
        osm.byTag("amenity",
                o -> o.isNode() && "bus_station".equals(o.getTag(amenityTag)) && Miensk.covers(o),
                o -> map.put(o.getId(), (IOsmNode) o));
    }

    static double distanceKm(IOsmNode node, MinsktransStop mt) {
        return osm.distanceKm(node.getLatitude(), node.getLongitude(), mt.lat, mt.lon);
    }

    static String formatDistanceKm(IOsmNode node, MinsktransStop mt) {
        if (mt.lat == 0) {
            return "???";
        }
        double d = osm.distanceKm(node.getLatitude(), node.getLongitude(), mt.lat, mt.lon);
        return new DecimalFormat("#.###").format(d);
    }

    static class Pair {
        MinsktransStop mt;
        IOsmNode node;
    }
}
