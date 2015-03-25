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

package org.alex73.osm.validators.cyhunka;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alex73.osm.utils.Belarus;
import org.alex73.osm.utils.CSV;
import org.alex73.osm.utils.Env;
import org.alex73.osm.utils.Lat;
import org.alex73.osm.utils.VelocityOutput;
import org.alex73.osm.validators.common.Errors;
import org.alex73.osm.validators.common.JS;
import org.alex73.osm.validators.common.ResultTable2;
import org.alex73.osmemory.IOsmNode;
import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.geometry.GeometryHelper;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Валідатар чыгуначных станцый.
 */
public class CheckStancyi {
    public static double LATITUDE_SIZE = 111.321;
    public static double LONGTITUDE_BELARUS_SIZE = 67.138;

    static Belarus osm;
    static Map<String, IOsmObject> stations = new HashMap<>();
    static Map<String, IOsmObject> esrs = new HashMap<>();

    public static void main(String[] args) throws Exception {
        List<Stancyja> stansyji = new CSV('\t').readCSV(Env.readProperty("dav") + "/Cyhunka.csv",
                Stancyja.class);

        osm = new Belarus();

        // шукаем існуючыі станцыі і прыпынкі
        short railwayTag = osm.getTagsPack().getTagCode("railway");
        short stationTag = osm.getTagsPack().getTagCode("station");
        short esrUserTag = osm.getTagsPack().getTagCode("esr:user");
        osm.byTag("railway",
                o -> osm.contains(o)
                        && (o.getTag(railwayTag).equals("station") || o.getTag(railwayTag).equals("halt"))
                        && !"subway".equals(o.getTag(stationTag)), o -> stations.put(o.getObjectCode(), o));
        osm.byTag("esr:user", o -> osm.contains(o), o -> esrs.put(o.getTag(esrUserTag), o));

        Errors errors = new Errors();

        ResultTable2 table = new ResultTable2("name", "name:ru", "name:be", "int_name", "esr:user", "railway");
        for (Stancyja st : stansyji) {
            if (st.nodeID == null || st.nodeID == 0) {
                errors.addError("Невядомы аб'ект OSM для станцыі " + st);
                continue;
            }
            IOsmObject obj = stations.remove("n" + st.nodeID);
            if (obj == null) {
                errors.addError("Няма аб'екту OSM для станцыі " + st, "n" + st.nodeID);
                continue;
            }
            if (obj.getTag(railwayTag) == null) {
                errors.addError("Аб'ект OSM для станцыі " + st + " зьмяніў тып", obj);
                continue;
            }

            if (st.esr!=null) {
                esrs.remove(st.esr.toString());
            }

            String name;
            if (st.nameRuForce != null) {
                name = st.nameRuForce;
            } else if (st.nameRu != null) {
                name = st.nameRu;
            } else {
                name = st.nameRuOld;
            }

            String distance;
            double dist;
            if (obj.isNode()) {
                IOsmNode node = (IOsmNode) obj;
                dist = distanceKm(GeometryHelper.coord(node.getLongitude(), node.getLatitude()),
                        GeometryHelper.coord(st.lon, st.lat));
                distance = new DecimalFormat("0.###").format(dist) + " км";
                if (dist > 3) {
                    distance = "<span style='background: red'>" + distance + "</span>";
                } else if (dist > 0.5) {
                    distance = "<span style='background: yellow'>" + distance + "</span>";
                }
            } else {
                distance = "";
                dist = 0;
            }
            ResultTable2.ResultTableRow row = table.new ResultTableRow("", obj.getObjectCode(), distance);

            row.setAttr("name", obj.getTag("name", osm), name);
            row.setAttr("name:ru", obj.getTag("name:ru", osm), name);
            row.setAttr("name:be", obj.getTag("name:be", osm), st.nameBe);
            row.setAttr("int_name", obj.getTag("int_name", osm), Lat.lat(st.nameBe, false));
            row.setAttr("esr:user", obj.getTag("esr:user", osm), "" + st.esr);
            row.setAttr("railway", obj.getTag("railway", osm), "ст".equals(st.typ) ? "station" : "halt");
            row.addChanged();
        }
        for (IOsmObject obj : stations.values()) {
            errors.addError("Невядомая станцыя ў OSM : " + obj.getTag("name", osm) + " / " + obj.getId(), obj);
        }
        for (IOsmObject obj : esrs.values()) {
            errors.addError("Невядомыя ESR на мапе : " + obj.getTag("name", osm) + " / " + obj.getId(), obj);
        }

        String out = Env.readProperty("out.dir") + "/cyhunka.html";
        String outJS = Env.readProperty("out.dir") + "/cyhunka.js";
        VelocityOutput.output("org/alex73/osm/validators/cyhunka/cyhunka.velocity", out, "table", table,
                "errors", errors);
        JS js = new JS(outJS);
        js.add("errors", errors.getJS());
        js.add("table", table.getJS());
    }

    static double distanceKm(Coordinate p1, Coordinate p2) {
        double dx = Math.abs(p1.x - p2.x) * LONGTITUDE_BELARUS_SIZE;
        double dy = Math.abs(p1.y - p2.y) * LATITUDE_SIZE;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
