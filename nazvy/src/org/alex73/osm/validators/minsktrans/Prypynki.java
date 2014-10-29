package org.alex73.osm.validators.minsktrans;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.alex73.osm.utils.Belarus;
import org.alex73.osm.utils.CSV;
import org.alex73.osm.utils.Env;
import org.alex73.osm.utils.GeoUtils;
import org.alex73.osm.utils.VelocityOutput;
import org.alex73.osm.validators.common.Errors;
import org.alex73.osm.validators.common.ResultTable;
import org.alex73.osm.validators.common.ResultTable.ResultTableRow;
import org.alex73.osmemory.IOsmNode;
import org.alex73.osmemory.geometry.Area;
import org.alex73.osmemory.geometry.FastArea;

public class Prypynki {

    static Belarus osm;
    static FastArea Miensk;

    static List<MinsktransStop> minsktrans;
    static List<IOsmNode> map = new ArrayList<IOsmNode>();
    static ResultTable table;
    static Errors errors = new Errors();

    public static void main(String[] args) throws Exception {
        osm = new Belarus();
        Miensk = new FastArea(new Area(osm, osm.getRelationById(59195)).getGeometry(), osm);

        readMinsktrans();
        readMap();

        table = new ResultTable("name", "name:be");
        compare(0.01);
        compare(0.03);
        compare(0.05);
        compare(0.08);
        compare(0.1);

        none();

        String out = Env.readProperty("out.dir") + "/prypynkiMiensk.html";
        VelocityOutput.output("org/alex73/osm/validators/minsktrans/prypynki.velocity", out, "table", table,
                "errors", errors);
    }

    static void compare(double maxDistance) {
        short nameTag = osm.getTagsPack().getTagCode("name");
        short namebeTag = osm.getTagsPack().getTagCode("name:be");
        for (MinsktransStop mt : new ArrayList<>(minsktrans)) {
            if (mt.lat == 0) {
                continue;
            }
            Pair p = new Pair();
            p.mt = mt;
            double minDistance = Double.MAX_VALUE;
            IOsmNode minNode = null;
            for (IOsmNode n : map) {
                double dist = distanceKm(n, mt);
                if (dist < minDistance && dist < maxDistance) {
                    minDistance = dist;
                    minNode = n;
                }
            }
            if (minNode != null) {
                p.node = minNode;
                map.remove(p.node);
                minsktrans.remove(mt);
                String d = new DecimalFormat("#.###").format(distanceKm(p.node, mt));
                ResultTableRow row = table.new ResultTableRow(p.node.getObjectCode(), mt.name + " (" + d
                        + ")");
                String n = p.node.getTag(nameTag) + "=>" + mt.name;
                row.setAttr("name", n, n);
                row.setAttr("name:be", p.node.getTag(namebeTag), p.node.getTag(namebeTag));
                table.rows.add(row);
            }
        }

    }

    static void none() {
        short nameTag = osm.getTagsPack().getTagCode("name");
        for (MinsktransStop mt : minsktrans) {
            if (mt.lat == 0) {
                errors.addError("Няма прыпынку '" + mt.name + "' на мапе");
            } else {
                errors.addError("Няма прыпынку '" + mt.name
                        + "' на <a href='https://www.openstreetmap.org/#map=18/" + mt.lat / 100000.0 + "/"
                        + mt.lon / 100000.0 + "'>мапе</a>");
            }
        }
        for (IOsmNode n : map) {
            errors.addError("Няма прыпынку '" + n.getTag(nameTag) + "' у табліцы Мінсктранса", n);
        }
    }

    static void readMinsktrans() throws Exception {
        String davDirectory = Env.readProperty("dav");

        minsktrans = new CSV(';').readCSV(davDirectory + "/../Minsktrans/stops.txt", MinsktransStop.class);
        String prevName = null;
        for (MinsktransStop stop : minsktrans) {
            // прапушчаныя назвы - гэта такія самыя як папярэднія
            if (stop.name == null) {
                stop.name = prevName;
            } else {
                prevName = stop.name;
            }
        }
    }

    static void readMap() throws Exception {
        short highwayTag = osm.getTagsPack().getTagCode("highway");
        map = new ArrayList<IOsmNode>();
        osm.byTag("highway", o -> o.isNode() && "bus_stop".equals(o.getTag(highwayTag)) && Miensk.covers(o),
                o -> map.add((IOsmNode) o));
    }

    static double distanceKm(IOsmNode node, MinsktransStop mt) {
        return GeoUtils.distanceKm(node.getLatitude(), node.getLongitude(), mt.lat / 100000.0,
                mt.lon / 100000.0);
    }

    static class Pair {
        MinsktransStop mt;
        IOsmNode node;
    }
}
