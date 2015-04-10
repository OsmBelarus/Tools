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

package org.alex73.osm.monitors.export;

import gen.alex73.osm.validators.rehijony.Rajon;
import gen.alex73.osm.validators.rehijony.Voblasc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.alex73.osm.utils.CSV;
import org.alex73.osm.utils.Env;
import org.alex73.osm.utils.Lat;
import org.alex73.osm.validators.common.RehijonyLoad;
import org.alex73.osm.validators.harady.Miesta;
import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.MemoryStorage;
import org.alex73.osmemory.geometry.FastArea;
import org.alex73.osmemory.geometry.GeometryHelper;
import org.alex73.osmemory.geometry.OsmHelper;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Межы рэгіёнаў для экспарту.
 */
public class Borders {
    public List<Border> kraina = new ArrayList<>();
    public List<Border> voblasci = new ArrayList<>();
    public List<Border> rajony = new ArrayList<>();
    public List<Border> miesty = new ArrayList<>();

    /**
     * Чытае межы з файлу.
     */
    public Borders(String file, MemoryStorage osm) throws Exception {
        Properties props = new Properties();
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            props.load(in);
            for (String p : ((Set<String>) (Set) props.keySet())) {
                FastArea area = new FastArea(GeometryHelper.fromWkt(props.getProperty(p)), osm);
                if (p.startsWith("KRAINA")) {
                    kraina.add(new Border(p.substring(6), area));
                } else if (p.startsWith("VOBLASC")) {
                    voblasci.add(new Border(p.substring(7), area));
                } else if (p.startsWith("RAJON")) {
                    rajony.add(new Border(p.substring(5), area));
                } else if (p.startsWith("MIESTA")) {
                    miesty.add(new Border(p.substring(6), area));
                }
            }
        } catch (FileNotFoundException ex) {
        }
    }

    /**
     * Запісвае межы ў файл.
     */
    public void save(String file) throws Exception {
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
            Properties props = new Properties();
            for (Border b : kraina) {
                props.put("KRAINA" + b.name, GeometryHelper.toWkt(b.area.getGeometry()));
            }
            for (Border b : voblasci) {
                props.put("VOBLASC" + b.name, GeometryHelper.toWkt(b.area.getGeometry()));
            }
            for (Border b : rajony) {
                props.put("RAJON" + b.name, GeometryHelper.toWkt(b.area.getGeometry()));
            }
            for (Border b : miesty) {
                props.put("MIESTA" + b.name, GeometryHelper.toWkt(b.area.getGeometry()));
            }
            props.store(out, null);
        }
    }

    /**
     * Абнаўляе межы з мапы.
     */
    public void update(MemoryStorage osm) throws Exception {
        List<Border> oldkraina = kraina;
        List<Border> oldvoblasci = voblasci;
        List<Border> oldrajony = rajony;
        List<Border> oldmiesty = miesty;

        kraina = new ArrayList<>();
        voblasci = new ArrayList<>();
        rajony = new ArrayList<>();
        miesty = new ArrayList<>();

        RehijonyLoad.load(Env.readProperty("dav") + "/Rehijony.xml");
        
        FastArea area ;
        String nazva;
        
        area = new FastArea(osm.getObject(RehijonyLoad.kraina.getOsmID()), osm);
        kraina.add(new Border("/", area));
        for (Voblasc v : RehijonyLoad.kraina.getVoblasc()) {
            nazva = Lat.unhac(Lat.lat('/' + v.getNameBe() + '/', false)).replace(' ', '_');
            area = new FastArea(osm.getObject(v.getOsmID()), osm);
            voblasci.add(new Border(nazva, area));
            for (Rajon r : v.getRajon()) {
                String rn = r.getNameBeCorrect() != null ? r.getNameBeCorrect() : r.getNameBe();
                nazva = Lat.unhac(Lat.lat('/' + v.getNameBe() + '/' + rn + '/', false)).replace(' ', '_');
                area = new FastArea(osm.getObject(r.getOsmID()), osm);
                rajony.add(new Border(nazva, area));
            }
        }

        // чытаем гарады і вёскі
        List<Miesta> dav = new CSV('\t').readCSV(Env.readProperty("dav") + "/Nazvy_nasielenych_punktau.csv",
                Miesta.class);
        for (Miesta m : dav) {
            if (m.osmIDother == null) {
                continue;
            }
            String path = "/" + m.voblasc + " вобласць/";
            if (!"<вобласць>".equals(m.rajon)) {
                path += m.rajon + " раён/";
            }
            path += m.nazvaNoStress + "/";
            path = Lat.unhac(Lat.lat(path, false)).replace(' ', '_');
            switch (m.typ) {
            case "г.":
            case "г. п.":
            case "к. п.":
                break;
            default:
                continue;
            }

            Geometry g = null;
            try {
                for (String oc : m.osmIDother.split(";")) {
                    IOsmObject o = osm.getObject(oc);
                    if (o == null) {
                        continue;
                    }
                    if (g == null) {
                        g = OsmHelper.areaFromObject(o, osm);
                    } else {
                        g = g.union(OsmHelper.areaFromObject(o, osm));
                    }
                }
                if (g != null) {
                    miesty.add(new Border(path, new FastArea(g, osm)));
                }
            } catch (Exception ex) {
            }
        }
        applyOld(oldkraina, kraina);
        applyOld(oldvoblasci, voblasci);
        applyOld(oldrajony, rajony);
        applyOld(oldmiesty, miesty);
    }

    private void applyOld(List<Border> oldBorders, List<Border> newBorders) {
        Map<String, Border> old = new HashMap<>();
        for (Border b : oldBorders) {
            old.put(b.name, b);
        }
        for (Border b : newBorders) {
            old.remove(b.name);
        }
        newBorders.addAll(old.values());
    }

    public static class Border {
        public final String name;
        public final FastArea area;

        public Border(String name, FastArea area) {
            this.name = name;
            this.area = area;
        }
    }
}
