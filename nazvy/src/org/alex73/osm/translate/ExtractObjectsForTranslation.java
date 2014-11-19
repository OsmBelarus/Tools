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
import java.util.regex.Pattern;

import org.alex73.osm.utils.Belarus;
import org.alex73.osm.utils.CSV;
import org.alex73.osm.utils.LettersCheck;
import org.alex73.osm.utils.POReader;
import org.alex73.osm.utils.POWriter;
import org.alex73.osm.utils.TMX;
import org.alex73.osm.utils.VelocityOutput;
import org.alex73.osm.validators.common.Errors;
import org.alex73.osm.validators.common.ResultTable;
import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.geometry.OsmHelper;
import org.alex73.osmemory.geometry.ExtendedRelation;
import org.alex73.osmemory.geometry.FastArea;

/**
 * Экспартуе некаторыя аб'екты для перакладу.
 */
public class ExtractObjectsForTranslation {
    static Belarus osm;
    static short nameTag, namebeTag;
    static Set<String> processed = new HashSet<>();

    public static void main(String[] args) throws Exception {
        osm = new Belarus();
        FastArea Miensk = new FastArea(new ExtendedRelation(osm.getRelationById(59195), osm).getArea(), osm);
        FastArea Brest = new FastArea(new ExtendedRelation(osm.getRelationById(72615), osm).getArea(), osm);
        FastArea Hrodna = new FastArea(new ExtendedRelation(osm.getRelationById(130921), osm).getArea(), osm);
        FastArea Viciebsk = new FastArea(new ExtendedRelation(osm.getRelationById(68614), osm).getArea(), osm);
        FastArea Mahilou = new FastArea(new ExtendedRelation(osm.getRelationById(62145), osm).getArea(), osm);
        FastArea Homiel = new FastArea(new ExtendedRelation(osm.getRelationById(163244), osm).getArea(), osm);

        nameTag = osm.getTagsPack().getTagCode("name");
        namebeTag = osm.getTagsPack().getTagCode("name:be");
        short highwayTag = osm.getTagsPack().getTagCode("highway");
        short waterTag = osm.getTagsPack().getTagCode("water");
        short waterwayTag = osm.getTagsPack().getTagCode("waterway");
        short naturalTag = osm.getTagsPack().getTagCode("natural");
        short shopTag = osm.getTagsPack().getTagCode("shop");
        short placeTag = osm.getTagsPack().getTagCode("place");
        short amenityTag = osm.getTagsPack().getTagCode("amenity");
        short leisureTag = osm.getTagsPack().getTagCode("leisure");

        processed.clear();
        process(o -> ("river".equals(o.getTag(waterTag)) || "river".equals(o.getTag(waterwayTag)))
                && osm.contains(o), "water_reki");
        process(o -> "lake".equals(o.getTag(waterTag)) && osm.contains(o), "water_aziory");
        process(o -> "water".equals(o.getTag(naturalTag)) && osm.contains(o), "water_other");

        processed.clear();
        process(o -> "bus_stop".equals(o.getTag(highwayTag)) && Miensk.covers(o), "bus_stop_Miensk");
        process(o -> "bus_stop".equals(o.getTag(highwayTag)) && Brest.covers(o), "bus_stop_Brest");
        process(o -> "bus_stop".equals(o.getTag(highwayTag)) && Hrodna.covers(o), "bus_stop_Hrodna");
        process(o -> "bus_stop".equals(o.getTag(highwayTag)) && Viciebsk.covers(o), "bus_stop_Viciebsk");
        process(o -> "bus_stop".equals(o.getTag(highwayTag)) && Mahilou.covers(o), "bus_stop_Mahilou");
        process(o -> "bus_stop".equals(o.getTag(highwayTag)) && Homiel.covers(o), "bus_stop_Homiel");
        process(o -> "bus_stop".equals(o.getTag(highwayTag)) && osm.contains(o), "bus_stop_other");

        processed.clear();
        process(o -> "suburb".equals(o.getTag(placeTag)) && osm.contains(o), "place_suburb");
        process(o -> "neighbourhood".equals(o.getTag(placeTag)) && osm.contains(o), "place_neighbourhood");

        processed.clear();
        process(o -> "supermarket".equals(o.getTag(shopTag)) && osm.contains(o), "shop_supermarket");

        processed.clear();
        process(o -> "place_of_worship".equals(o.getTag(amenityTag)) && osm.contains(o),
                "relihijnyja_budynki");

        processed.clear();
        process(o -> "park".equals(o.getTag(leisureTag)) && osm.contains(o), "leisure_park");
        process(o -> "stadium".equals(o.getTag(leisureTag)) && osm.contains(o), "leisure_stadium");

        processed.clear();
        process(o -> isSubway(o) && osm.contains(o), "subway");
    }

    static boolean isSubway(IOsmObject o) {
        short railwayTag = osm.getTagsPack().getTagCode("railway");
        short stationTag = osm.getTagsPack().getTagCode("station");

        if ("subway_entrance".equals(o.getTag(railwayTag))) {
            return true;
        }
        if ("subway".equals(o.getTag(stationTag)) && "station".equals(o.getTag(railwayTag))) {
            return true;
        }
        return false;
    }

    static void process(Predicate<IOsmObject> predicate, String filename) throws Exception {
        System.out.println("Out " + filename);
        Map<String, String> fixes = processMapRuRu(predicate, filename);

        POWriter po = new POWriter();
        TMX tmx = new TMX();
        osm.all(predicate, o -> addToSourcePo(po, tmx, o, fixes));
        po.write("../../OsmBelarus-Databases/Pieraklad/map/" + filename + ".po");
        tmx.save(new File("../../OsmBelarus-Databases/Pieraklad/tm/" + filename + ".tmx"));

        Errors errors = new Errors();
        ResultTable result = new ResultTable("name", "name:be");

        POReader rd = new POReader("../../OsmBelarus-Databases/Pieraklad/translation/" + filename + ".po");
        osm.all(predicate, o -> getFromTranslatedPo(rd, o, fixes, result, errors));

        result.sort();
        VelocityOutput.output("org/alex73/osm/translate/objects.velocity", "/var/www/osm/translate/"
                + filename + ".html", "table", result, "errors", errors);
    }

    /**
     * Чытае файл .csv з мэпінгам name->name, выдаляе неіснуючыя назвы і дадае новыя
     */
    static Map<String, String> processMapRuRu(Predicate<IOsmObject> predicate, String filename)
            throws Exception {
        Map<String, String> fixes = new HashMap<>();

        // чытаем з csv
        try {
            List<Replace> replaces = new CSV('\t').readCSV("../../OsmBelarus-Databases/Pieraklad/fixes/"
                    + filename + ".csv", Replace.class);
            for (Replace r : replaces) {
                if (!r.from.equals(r.to)) {// толькі тыя што не супадаюць
                    fixes.put(r.from, r.to);
                }
            }
        } catch (FileNotFoundException ex) {
        }

        // дадаем невыпраўленыя
        osm.all(predicate.and(o -> o.getTag(nameTag) != null && !fixes.containsKey(o.getTag(nameTag))),
                o -> fixes.put(o.getTag(nameTag), o.getTag(nameTag)));

        // запісваем ў csv
        List<Replace> names = new ArrayList<>();
        for (Map.Entry<String, String> en : fixes.entrySet()) {
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
        new CSV('\t').saveCSV("../../OsmBelarus-Databases/Pieraklad/fixes/" + filename + ".csv",
                Replace.class, names);

        return fixes;
    }

    static void addToSourcePo(POWriter po, TMX tmx, IOsmObject obj, Map<String, String> fixes) {
        if (processed.contains(obj.getObjectCode())) {
            return;
        }
        if (!obj.hasTag(nameTag) && !obj.hasTag(namebeTag)) {
            return;
        }

        String name = obj.getTag(nameTag);
        if (name == null) {
            name = "<null>";
        }
        name = fixes.get(name);
        if (obj.hasTag(nameTag) && obj.hasTag(namebeTag)) {
            tmx.put(name, obj.getTag(namebeTag));
        }
        po.add(name, "", obj.getObjectCode());
    }

    static void getFromTranslatedPo(POReader po, IOsmObject obj, Map<String, String> fixes,
            ResultTable table, Errors errors) {
        if (processed.contains(obj.getObjectCode())) {
            return;
        }
        if (!obj.hasTag(nameTag) && !obj.hasTag(namebeTag)) {
            return;
        }
        processed.add(obj.getObjectCode());

        String name = obj.getTag(nameTag);
        if (name == null) {
            name = "<null>";
        }
        name = fixes.get(name);

        if (name == null || name.isEmpty()) {
            errors.addError("Няма расейскай назвы", obj);
            return;
        }
        String errName = LettersCheck.checkRu(name);
        if (errName != null) {
            errors.addError("Няправільныя літары ў расейскай назьве: " + errName, obj);
            return;
        }
        String translated = po.get(name);
        if (translated == null || translated.isEmpty()) {
            errors.addError("Не перакладзена : " + name, obj);
        } else {
            String errTranslated = LettersCheck.checkBe(translated);
            if (errTranslated != null) {
                errors.addError("Няправільныя літары ў беларускай назьве: " + errTranslated, obj);
                return;
            }
        }
        ResultTable.ResultTableRow row = table.new ResultTableRow(obj.getObjectCode(), name);
        row.setAttr("name", obj.getTag(nameTag), name);
        row.setAttr("name:be", obj.getTag(namebeTag), translated);
        if (row.needChange()) {
            table.rows.add(row);
        }
    }
}
