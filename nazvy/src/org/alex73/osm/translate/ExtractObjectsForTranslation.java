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

import java.io.FileNotFoundException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.alex73.osm.utils.Belarus;
import org.alex73.osm.utils.OSM;
import org.alex73.osm.utils.POReader;
import org.alex73.osm.utils.POWriter;
import org.alex73.osm.utils.TSV;
import org.alex73.osm.utils.VelocityOutput;
import org.alex73.osm.validators.harady.ResultTable;
import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.geometry.Area;
import org.alex73.osmemory.geometry.FastArea;

/**
 * Экспартуе некаторыя аб'екты для перакладу.
 */
public class ExtractObjectsForTranslation {
    static Belarus osm;
    static short nameTag, namebeTag;
    static Map<String, String> oldReplace = new HashMap<>();
    static Map<String, String> nameReplace = new HashMap<>();
    static Set<String> processed = new HashSet<>();

    public static void main(String[] args) throws Exception {
        osm = new Belarus();
        FastArea Miensk = new FastArea(new Area(osm, osm.getRelationById(59195)).getGeometry(), osm);
        FastArea Brest = new FastArea(new Area(osm, osm.getRelationById(72615)).getGeometry(), osm);
        FastArea Hrodna = new FastArea(new Area(osm, osm.getRelationById(130921)).getGeometry(), osm);
        FastArea Viciebsk = new FastArea(new Area(osm, osm.getRelationById(68614)).getGeometry(), osm);
        FastArea Mahilou = new FastArea(new Area(osm, osm.getRelationById(62145)).getGeometry(), osm);
        FastArea Homiel = new FastArea(new Area(osm, osm.getRelationById(163244)).getGeometry(), osm);

        nameTag = osm.getTagsPack().getTagCode("name");
        namebeTag = osm.getTagsPack().getTagCode("name:be");
        short highwayTag = osm.getTagsPack().getTagCode("highway");
        short railwayTag = osm.getTagsPack().getTagCode("railway");
        short waterTag = osm.getTagsPack().getTagCode("water");
        short waterwayTag = osm.getTagsPack().getTagCode("waterway");
        short naturalTag = osm.getTagsPack().getTagCode("natural");
        short shopTag = osm.getTagsPack().getTagCode("shop");
        short placeTag = osm.getTagsPack().getTagCode("place");
        short amenityTag = osm.getTagsPack().getTagCode("amenity");
        short leisureTag = osm.getTagsPack().getTagCode("leisure");

        processed.clear();
        out(o -> ("river".equals(o.getTag(waterTag)) || "river".equals(o.getTag(waterwayTag)))
                && osm.contains(o), "water_reki");
        out(o -> "lake".equals(o.getTag(waterTag)) && osm.contains(o), "water_aziory");
        out(o -> "water".equals(o.getTag(naturalTag)) && osm.contains(o), "water_other");

        processed.clear();
        out(o -> "bus_stop".equals(o.getTag(highwayTag)) && Miensk.covers(o), "bus_stop_Miensk");
        out(o -> "bus_stop".equals(o.getTag(highwayTag)) && Brest.covers(o), "bus_stop_Brest");
        out(o -> "bus_stop".equals(o.getTag(highwayTag)) && Hrodna.covers(o), "bus_stop_Hrodna");
        out(o -> "bus_stop".equals(o.getTag(highwayTag)) && Viciebsk.covers(o), "bus_stop_Viciebsk");
        out(o -> "bus_stop".equals(o.getTag(highwayTag)) && Mahilou.covers(o), "bus_stop_Mahilou");
        out(o -> "bus_stop".equals(o.getTag(highwayTag)) && Homiel.covers(o), "bus_stop_Homiel");
        out(o -> "bus_stop".equals(o.getTag(highwayTag)) && osm.contains(o), "bus_stop_other");

        processed.clear();
        out(o -> "suburb".equals(o.getTag(placeTag)) && osm.contains(o), "place_suburb");
        out(o -> "neighbourhood".equals(o.getTag(placeTag)) && osm.contains(o), "place_neighbourhood");

        processed.clear();
        out(o -> "supermarket".equals(o.getTag(shopTag)) && osm.contains(o), "shop_supermarket");

        processed.clear();
        out(o -> "place_of_worship".equals(o.getTag(amenityTag)) && osm.contains(o), "relihijnyja_budynki");

        processed.clear();
        out(o -> "halt".equals(o.getTag(railwayTag)) && osm.contains(o), "railway_halt");
        out(o -> "station".equals(o.getTag(railwayTag)) && osm.contains(o), "railway_station");

        processed.clear();
        out(o -> "park".equals(o.getTag(leisureTag)) && osm.contains(o), "leisure_park");
        out(o -> "stadium".equals(o.getTag(leisureTag)) && osm.contains(o), "leisure_stadium");
    }

    static void out(Predicate<IOsmObject> predicate, String filename) throws Exception {
        oldReplace.clear();
        nameReplace.clear();

        try {
            List<Replace> replaces = new TSV('\t').readCSV("../../OsmBelarus-Databases/Pieraklad/map/"
                    + filename + ".csv", Replace.class);
            for (Replace r : replaces) {
                if (!r.from.equals(r.to)) {
                    oldReplace.put(r.from, r.to);
                }
            }
        } catch (FileNotFoundException ex) {
        }

        POWriter po = new POWriter();

        osm.all(predicate, o -> out(po, o));

        po.write("../../OsmBelarus-Databases/Pieraklad/map/" + filename + ".po");

        List<Replace> names = new ArrayList<>();
        for (Map.Entry<String, String> en : nameReplace.entrySet()) {
            Replace r = new Replace();
            r.from = en.getKey();
            r.to = en.getValue();
            names.add(r);
        }
        Collections.sort(names, new Comparator<Replace>() {
            Locale RU = new Locale("ru");
            Collator RUC = Collator.getInstance(RU);

            @Override
            public int compare(Replace o1, Replace o2) {
                return RUC.compare(o1.from, o2.from);
            }
        });
        new TSV('\t').saveCSV("../../OsmBelarus-Databases/Pieraklad/map/" + filename + ".csv", Replace.class,
                names);

        ResultTable result = new ResultTable("name", "name:be");

        POReader rd = new POReader("../../OsmBelarus-Databases/Pieraklad/translation/" + filename + ".po");
        osm.all(predicate, o -> in(rd, o, result));

        result.sort();
        VelocityOutput.output("org/alex73/osm/translate/objects.velocity", "/var/www/osm/translate/"
                + filename + ".html", "table", result, "OSM", OSM.class);
    }

    static void out(POWriter po, IOsmObject obj) {
        if (processed.contains(obj.getObjectCode())) {
            return;
        }
        if (obj.hasTag(nameTag) || obj.hasTag(namebeTag)) {
            String name = obj.getTag(nameTag);
            if (name == null) {
                name = "<null>";
            }
            String old = oldReplace.remove(name);
            if (old != null) {
                nameReplace.put(name, old);
            }
            if (!nameReplace.containsKey(name)) {
                nameReplace.put(name, name);
            }
            String nru = nameReplace.get(name);

            po.add(nru, obj.getTag(namebeTag), obj.getObjectCode());
        }
    }

    static void in(POReader po, IOsmObject obj, ResultTable table) {
        if (processed.contains(obj.getObjectCode())) {
            return;
        }
        processed.add(obj.getObjectCode());

        String name = obj.getTag(nameTag);
        if (name == null) {
            name = "<null>";
        }
        String nru = nameReplace.containsKey(name) ? nameReplace.get(name) : name;
        String namebe = obj.getTag(namebeTag);
        String translated = po.get(nru);
        if (translated != null && translated.isEmpty()) {
            translated = null;
        }
        ResultTable.ResultTableRow row = table.new ResultTableRow(obj.getObjectCode(), name);
        row.setAttr("name", name, nru);
        row.setAttr("name:be", namebe, translated);
        if (row.needChange()) {
            table.rows.add(row);
        }
    }
}
