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

package org.alex73.osm.translate;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alex73.osm.utils.Belarus;
import org.alex73.osm.utils.OSM;
import org.alex73.osm.utils.POReader;
import org.alex73.osm.utils.POWriter;
import org.alex73.osm.utils.VelocityOutput;
import org.alex73.osm.validators.harady.ResultTable;
import org.alex73.osmemory.IOsmObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Экспартуе некаторыя аб'екты для перакладу.
 */
public class ExtractObjectsForTranslation {
    static Belarus osm;
    static short nameTag, namebeTag;
    static Map<String, String> nameReplace = new HashMap<>();

    public static void main(String[] args) throws Exception {
        osm = new Belarus();

        nameTag = osm.getTagsPack().getTagCode("name");
        namebeTag = osm.getTagsPack().getTagCode("name:be");

        out("water", "lake");
        out("water", "river");
        out("waterway", "river");
        out("shop", "supermarket");
        out("place", "suburb");
        out("place", "neighbourhood");
        out("amenity", "place_of_worship");
    }

    static void out(String tag, String value) throws Exception {
        POWriter po = new POWriter();

        short tagCode = osm.getTagsPack().getTagCode(tag);

        osm.byTag(tag, o -> value.equals(o.getTag(tagCode)), o -> out(po, o));

        po.write("../../OsmBelarus-Databases/Pieraklad/map/" + tag + "_" + value + ".po");

        List<String> names = new ArrayList<>(po.getData().keySet());
        Collections.sort(names, POWriter.COMPARATOR);
        for (int i = 0; i < names.size(); i++) {
            names.set(i, names.get(i) + '\t' + names.get(i));
        }
        FileUtils.writeLines(new File("../../OsmBelarus-Databases/Pieraklad/map/" + tag + "_" + value
                + ".csv"), "UTF-8", names);

        ResultTable result = new ResultTable("name", "name:be");

        nameReplace.clear();
        File csvIn = new File("../../OsmBelarus-Databases/Pieraklad/translation/" + tag + "_" + value
                + ".csv");
        if (csvIn.exists()) {
            names = FileUtils.readLines(csvIn, "UTF-8");
            for (String n : names) {
                String[] ns = n.split("\t");
                if (ns.length == 2) {
                    nameReplace.put(ns[0], ns[1]);
                }
            }
        }
        POReader rd = new POReader("../../OsmBelarus-Databases/Pieraklad/translation/" + tag + "_" + value
                + ".po");
        osm.byTag(tag, o -> value.equals(o.getTag(tagCode)), o -> in(rd, o, result));

        result.sort();
        VelocityOutput.output("org/alex73/osm/translate/objects.velocity", "/var/www/osm/translate/" + tag
                + "_" + value + ".html", "table", result, "OSM", OSM.class);
    }

    static void out(POWriter po, IOsmObject obj) {
        if (osm.contains(obj)) {
            if (obj.hasTag(nameTag) || obj.hasTag(namebeTag)) {
                po.add(obj.getTag(nameTag), obj.getTag(namebeTag), obj.getObjectCode());
            }
        }
    }

    static void in(POReader po, IOsmObject obj, ResultTable table) {
        if (osm.contains(obj)) {
            String name = obj.getTag(nameTag);
            if (name == null) {
                name = "<null>";
            }
            String nru = nameReplace.containsKey(name) ? nameReplace.get(name) : name;
            String namebe = obj.getTag(namebeTag);
            String translated = po.get(nru);
            if (!StringUtils.equals(namebe, translated)) {
                ResultTable.ResultTableRow row = table.new ResultTableRow(obj.getObjectCode(), name);
                row.setAttr("name", name, nru);
                row.setAttr("name:be", namebe, translated);
                table.rows.add(row);
            }
        }
    }
}
