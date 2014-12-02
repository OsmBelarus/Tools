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

package org.alex73.osm.validators.harady;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alex73.osm.utils.Belarus;
import org.alex73.osm.utils.CSV;
import org.alex73.osm.utils.Env;
import org.alex73.osm.utils.PadzielOsmNas;
import org.alex73.osm.validators.common.Errors;
import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.geometry.GeometryHelper;
import org.alex73.osmemory.geometry.OsmHelper;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Правярае вобласьці, раёны.
 */
public class CheckRehijony {
    static Belarus osm;
    static Errors errors;

    public static void main(String[] args) throws Exception {
        osm = new Belarus();
        errors = new Errors();
        check(osm, errors);
        errors.errors.keySet().forEach(o -> System.out.println(o));
    }

    static void check(Belarus posm, Errors perrors) throws Exception {
        osm = posm;
        errors = perrors;

        // правяраем існаваньне дакладна такога падзелу як у даведніку
        List<PadzielOsmNas> padziely = new CSV('\t').readCSV(Env.readProperty("dav") + "/Rehijony.csv",
                PadzielOsmNas.class);
        List<IOsmObject> level2 = new ArrayList<>();
        List<IOsmObject> level4 = new ArrayList<>();
        List<IOsmObject> level6 = new ArrayList<>();
        osm.byTag("admin_level",
                o -> o.isRelation() && o.getTag("admin_level", osm).equals("2") && osm.contains(o),
                o -> level2.add(o));
        osm.byTag("admin_level",
                o -> o.isRelation() && o.getTag("admin_level", osm).equals("4") && osm.contains(o),
                o -> level4.add(o));
        osm.byTag("admin_level",
                o -> o.isRelation() && o.getTag("admin_level", osm).equals("6") && osm.contains(o),
                o -> level6.add(o));
        check("Аб'екты з admin_level=2: ", level2, kraina(padziely));
        check("Аб'екты з admin_level=4: ", level4, voblasci(padziely));
        check("Аб'екты з admin_level=6: ", level6, rajony(padziely));

        if (errors.errors.isEmpty()) {
            // правяраем ці ствараюць вобласьці й раёны такую самую геамэтрыю як краіна
            checkGeometry("Вобласьці ў межах краіны: ", osm.getGeometry(), level4);
            checkGeometry("Раёны ў межах краіны: ", osm.getGeometry(), level6);
        }
    }

    static List<PadzielOsmNas> kraina(List<PadzielOsmNas> padziely) {
        List<PadzielOsmNas> r = new ArrayList<>();
        padziely.stream().filter(p -> p.voblasc == null && p.rajon == null).forEach(p -> {
            p.osmName = "Беларусь";
            r.add(p);
        });
        return r;
    }

    static List<PadzielOsmNas> voblasci(List<PadzielOsmNas> padziely) {
        List<PadzielOsmNas> r = new ArrayList<>();
        padziely.stream().filter(p -> p.voblasc != null && p.rajon == null).forEach(p -> {
            p.osmName = (p.osmName != null ? p.osmName : p.voblasc) + " вобласць";
            r.add(p);
        });
        return r;
    }

    static List<PadzielOsmNas> rajony(List<PadzielOsmNas> padziely) {
        List<PadzielOsmNas> r = new ArrayList<>();
        padziely.stream().filter(p -> p.rajon != null).forEach(p -> {
            p.osmName = (p.osmName != null ? p.osmName : p.rajon) + " раён";
            r.add(p);
        });
        return r;
    }

    static void check(String prefix, List<IOsmObject> osmObjects, List<PadzielOsmNas> padziely) {
        Set<String> found = new HashSet<>();
        osmObjects.forEach(o -> found.add(o.getObjectCode()));

        // гарады абласнога ці раённага падчыненьня
        for (PadzielOsmNas p : padziely) {
            if (p.harady != null) {
                String[] hs = p.harady.split(";");
                for (String h : hs) {
                    if (!found.remove(h)) {
                        errors.addError(prefix + "Няма гораду з даведніку(Rehijony.csv) '" + h
                                + "' на мапе альбо няправільны admin_level");
                    }
                }
            }
        }

        for (PadzielOsmNas p : padziely) {
            if (!found.remove("r" + p.relationID)) {
                errors.addError(prefix + "Няма аб'екту з даведніку(Rehijony.csv) 'r" + p.relationID
                        + "' на мапе альбо няправільны admin_level");
            }
        }

        // тыя што засталіся
        found.forEach(o -> errors
                .addError(prefix + "Няма ў даведніку(Rehijony.csv), але ёсьць на мапе: " + o));
    }

    static void checkGeometry(String prefix, Geometry full, List<IOsmObject> objects) throws Exception {
        Geometry check = GeometryHelper.emptyCollection();

        for (IOsmObject r : objects) {
            Geometry parea;
            try {
                parea = OsmHelper.areaFromObject(r, osm);
            } catch (Exception ex) {
                errors.addError("Памылка ў геамэтрыі", r);
                return;
            }
            Geometry is = check.difference(parea);
            if (!is.isEmpty()) {
                errors.addError(prefix + r.toString() + " перасякаецца зь іншымі рэгіёнамі(size="
                        + is.getArea() + "): " + is);
                return;
            }
            check = check.union(is);
        }
        if (!full.equals(check)) {
     //TODO: намаляваць       errors.addError(prefix + "Не пакрывае ўсю краіну");
        }
    }
}
