package org.alex73.osm.monitors.export;

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

import org.alex73.osm.utils.Belarus;
import org.alex73.osm.utils.CSV;
import org.alex73.osm.utils.Env;
import org.alex73.osm.utils.Lat;
import org.alex73.osm.utils.PadzielOsmNas;
import org.alex73.osm.validators.harady.Miesta;
import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.MemoryStorage;
import org.alex73.osmemory.geometry.ExtendedRelation;
import org.alex73.osmemory.geometry.FastArea;
import org.alex73.osmemory.geometry.GeometryHelper;
import org.alex73.osmemory.geometry.OsmHelper;

import com.vividsolutions.jts.geom.Geometry;

public class Borders {
    public List<Border> kraina = new ArrayList<>();
    public List<Border> voblasci = new ArrayList<>();
    public List<Border> rajony = new ArrayList<>();
    public List<Border> miesty = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        Belarus country = new Belarus("/data/tmp/osm/belarus-latest.o5m");
        Borders b = new Borders(country);
        b.kraina.clear();
        b.voblasci.clear();
        b.rajony.clear();
        b.miesty.clear();
        b.update(country);
        b.save();
    }

    public Borders(MemoryStorage osm) throws Exception {
        Properties props = new Properties();
        try (InputStream in = new BufferedInputStream(new FileInputStream("miezy.properties"))) {
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

    public void save() throws Exception {
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream("miezy.properties"))) {
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

    public void update(MemoryStorage osm) throws Exception {
        List<Border> oldkraina = kraina;
        List<Border> oldvoblasci = voblasci;
        List<Border> oldrajony = rajony;
        List<Border> oldmiesty = miesty;

        kraina = new ArrayList<>();
        voblasci = new ArrayList<>();
        rajony = new ArrayList<>();
        miesty = new ArrayList<>();

        // чытаем рэгіёны
        List<PadzielOsmNas> padziel = new CSV('\t').readCSV(Env.readProperty("dav") + "/Rehijony.csv",
                PadzielOsmNas.class);
        for (PadzielOsmNas p : padziel) {
            List<Border> place = kraina;
            String path = "/";
            if (p.voblasc != null) {
                path += p.voblasc + " вобласць" + '/';
                place = voblasci;
            }
            if (p.rajon != null) {
                path += p.rajon + " раён" + '/';
                place = rajony;
            }
            String nazva = Lat.unhac(Lat.lat(path, false)).replace(' ', '_');
            FastArea area = new FastArea(
                    new ExtendedRelation(osm.getRelationById(p.relationID), osm).getArea(), osm);
            place.add(new Border(nazva, area));
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
