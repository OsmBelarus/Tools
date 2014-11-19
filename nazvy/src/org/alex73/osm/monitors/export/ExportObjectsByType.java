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

import gen.alex73.osm.validators.objects.GranularityType;
import gen.alex73.osm.validators.objects.ObjectTypes;
import gen.alex73.osm.validators.objects.Type;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.alex73.osm.utils.Belarus;
import org.alex73.osm.utils.CSV;
import org.alex73.osm.utils.Env;
import org.alex73.osm.utils.Lat;
import org.alex73.osm.utils.PadzielOsmNas;
import org.alex73.osm.validators.harady.Miesta;
import org.alex73.osm.validators.objects.CheckObjects;
import org.alex73.osm.validators.objects.CheckType;
import org.alex73.osmemory.IOsmNode;
import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.IOsmRelation;
import org.alex73.osmemory.IOsmWay;
import org.alex73.osmemory.geometry.OsmHelper;
import org.alex73.osmemory.geometry.ExtendedRelation;
import org.alex73.osmemory.geometry.ExtendedWay;
import org.alex73.osmemory.geometry.Fast;
import org.alex73.osmemory.geometry.FastArea;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Экспартуе аб'екты па тыпах.
 */
public class ExportObjectsByType {
    static Belarus osm;
    static Fast fast;
    static OutputFormatter formatter;

    static List<CheckType> checkTypes;
    static Map<GranularityType, List<Rehijon>[][]> rehijony = new TreeMap<>();
    static Map<GranularityType, SplitCities> rehijonySplit = new TreeMap<>();
    static Map<String, Output> outputs = new HashMap<>();

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ENGLISH);

        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(new StreamSource(new File("src/object_types.xsd")));
        JAXBContext CTX = JAXBContext.newInstance(ObjectTypes.class);
        Unmarshaller unm = CTX.createUnmarshaller();
        unm.setSchema(schema);
        ObjectTypes config = (ObjectTypes) unm.unmarshal(new File("object-types.xml"));

        osm = new Belarus();
        CheckObjects.osm = osm;
        fast = new Fast(osm.getGeometry());

        loadRehijony();

        checkTypes = new ArrayList<>();
        for (Type t : config.getType()) {
            if (t.isMain()) {
                checkTypes.add(new CheckType(osm, t));
            }
        }

        Type t = new Type();
        t.setId("other");
        t.setMonitoring(GranularityType.MIESTA);
        checkTypes.add(new CheckType(osm, t));

        System.out.println("Process...");

        osm.all(o -> osm.contains(o), o -> process(o));

        for (Output o : outputs.values()) {
            o.close();
        }
    }

    static void process(IOsmObject obj) {
        for (CheckType ct : checkTypes) {
            if (ct.matches(obj)) {
                String fn = ct.getType().getId() + ".txt";

                GranularityType g = ct.getType().getMonitoring();
                while (!store(g, obj, fn)) {
                    g = upper(g);
                    if (g == null) {
                        throw new RuntimeException();
                    }
                }
                break;
            }
        }
    }

    static boolean store(GranularityType granularity, IOsmObject obj, String fn) {
        boolean found = false;

        Geometry geo;
        if (obj.isWay()) {
            ExtendedWay w = new ExtendedWay((IOsmWay) obj, osm);
            Envelope bound = w.getBoundingBox();

            for (Rehijon r : getRehijonyForArea(granularity, bound)) {
                if (r.area.covers(obj)) {
                    found = true;
                    String path = r.path + fn;
                    storeToFile(obj, path);
                }
            }

            return found;
        } else if (obj.isNode()) {
            IOsmNode n = (IOsmNode) obj;
            for (Rehijon r : getRehijonyForNode(granularity, n.getLat(), n.getLon())) {
                if (r.area.covers(obj)) {
                    found = true;
                    String path = r.path + fn;
                    storeToFile(obj, path);
                }
            }
            return found;
        } else {
            return true;
        }
        // for (Rehijon r : getrehijony.get(granularity)) {
        // if (bound != null && !r.area.mayCovers(bound)) {
        // continue;
        // }
        // if (r.area.covers(obj)) {
        // found = true;
        // String path = r.path + fn;
        // storeToFile(obj, path);
        // }
        // }
        // return found;
    }

    // static boolean storeNode(GranularityType granularity, IOsmObject obj, String fn) {
    // boolean found = false;
    //
    // for (Rehijon r : rehijony.get(granularity)) {
    // if (r.area.covers(obj)) {
    // found = true;
    // String path = r.path + fn;
    // storeToFile(obj, path);
    // }
    // }
    // return found;
    // }

    static List<Rehijon> getRehijonyForNode(GranularityType granularity, int lat, int lon) {
        Fast.Cell c = fast.getCellForPoint(lat, lon);
        return rehijony.get(granularity)[c.getX()][c.getY()];
    }

    static List<Rehijon> getRehijonyForArea(GranularityType granularity, Envelope bound) {
        Fast.Cell c1 = fast.getCellForPoint((int) (bound.getMinY() / IOsmNode.DIVIDER),
                (int) (bound.getMinX() / IOsmNode.DIVIDER));
        Fast.Cell c2 = fast.getCellForPoint((int) (bound.getMaxY() / IOsmNode.DIVIDER),
                (int) (bound.getMaxX() / IOsmNode.DIVIDER));
        List<Rehijon> result = new ArrayList<>();
        if (c1 == null && c2 == null) {
            return result;
        }
        if (c1 == null) {
            c1 = new Fast.Cell(0, 0, false, false, null);
        }
        if (c2 == null) {
            c2 = new Fast.Cell(Fast.PARTS_COUNT_BYXY - 1, Fast.PARTS_COUNT_BYXY - 1, false, false, null);
        }

        for (int i = c1.getX(); i <= c2.getX(); i++) {
            for (int j = c1.getY(); j <= c2.getY(); j++) {
                for (Rehijon r : rehijony.get(granularity)[i][j]) {
                    if (!result.contains(r)) {
                        result.add(r);
                    }
                }
            }
        }
        return result;
    }

    static GranularityType upper(GranularityType g) {
        switch (g) {
        case MIESTA:
            return GranularityType.RAJON;
        case RAJON:
            return GranularityType.VOBLASC;
        case VOBLASC:
            return GranularityType.KRAINA;
        }
        return null;
    }

    static void storeToFile(IOsmObject obj, String path) {
        Output o = outputs.get(path);
        if (o == null) {
            o = new Output(path);
            outputs.put(path, o);
        }
        try {
            o.out(obj);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Чытаем рэгіёны рознага ўзроўня.
     */
    static void loadRehijony() throws Exception {
        for (GranularityType g : new GranularityType[] { GranularityType.KRAINA, GranularityType.VOBLASC,
                GranularityType.RAJON, GranularityType.MIESTA }) {
            List[][] list = new List[Fast.PARTS_COUNT_BYXY][Fast.PARTS_COUNT_BYXY];
            rehijony.put(g, list);
            for (int i = 0; i < Fast.PARTS_COUNT_BYXY; i++) {
                list[i] = new List[Fast.PARTS_COUNT_BYXY];
                for (int j = 0; j < Fast.PARTS_COUNT_BYXY; j++) {
                    list[i][j] = new ArrayList<Rehijon>();
                }
            }
        }

        putRehijon(GranularityType.KRAINA, new Rehijon("/", new FastArea(osm.getGeometry(), osm)));

        List<PadzielOsmNas> padziel = new CSV('\t').readCSV(Env.readProperty("dav") + "/Rehijony.csv",
                PadzielOsmNas.class);
        for (PadzielOsmNas p : padziel) {
            if (p.voblasc == null) {
                continue;
            }
            FastArea area = new FastArea(new ExtendedRelation(osm.getRelationById(p.relationID), osm).getArea(), osm);
            if (p.rajon == null) {
                // вобласьць
                String path = p.voblasc + " вобласць" + '/';
                putRehijon(GranularityType.VOBLASC, new Rehijon(path, area));
            } else {
                // раён
                String path = p.voblasc + " вобласць" + '/' + p.rajon + " раён" + '/';
                putRehijon(GranularityType.RAJON, new Rehijon(path, area));
            }
        }

        List<Miesta> miesty = new CSV('\t').readCSV(Env.readProperty("dav")
                + "/Nazvy_nasielenych_punktau.csv", Miesta.class);
        for (Miesta m : miesty) {
            if (m.osmIDother == null) {
                continue;
            }
            String path = m.voblasc + " вобласць/";
            if (!"<вобласць>".equals(m.rajon)) {
                path += m.rajon + " раён/";
            }
            path += m.nazvaNoStress + "/";

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
            } catch (Exception ex) {
                ex.printStackTrace();
                g = null;
            }
            if (g != null) {
                putRehijon(GranularityType.MIESTA, new Rehijon(path, new FastArea(g, osm)));
            }
        }
    }

    static void putRehijon(GranularityType granularity, Rehijon r) {
        List[][] lists = rehijony.get(granularity);
        for (int i = 0; i < Fast.PARTS_COUNT_BYXY; i++) {
            for (int j = 0; j < Fast.PARTS_COUNT_BYXY; j++) {
                Fast.Cell c = fast.getCell(i, j);
                if (r.area.getGeometry().intersects(c.getGeom())) {
                    lists[i][j].add(r);
                }
            }
        }
    }

    static class Output {
        final String path;
        final OutputFormatter formatter;
        PrintStream wr;

        Output(String path) {
            this.path = path;
            this.formatter = new OutputFormatter(osm);
        }

        void out(IOsmObject o) throws Exception {
            if (wr == null) {
                open();
            }
            switch (o.getType()) {
            case IOsmObject.TYPE_NODE:
                IOsmNode n = (IOsmNode) o;
                wr.println(formatter.objectName(n));
                wr.println("  other names: " + formatter.otherNames(n));
                wr.println("  other tags : " + formatter.otherTags(n));
                wr.println("    geometry : " + formatter.getGeometry(n));
                break;
            case IOsmObject.TYPE_WAY:
                IOsmWay w = (IOsmWay) o;
                wr.println(formatter.objectName(w));
                wr.println("  other names: " + formatter.otherNames(w));
                wr.println("  other tags : " + formatter.otherTags(w));
                wr.println("    geometry :" + formatter.getGeometry(w));
                break;
            case IOsmObject.TYPE_RELATION:
                IOsmRelation r = (IOsmRelation) o;
                wr.println(formatter.objectName(r));
                wr.println("  other names: " + formatter.otherNames(r));
                wr.println("  other tags : " + formatter.otherTags(r));
                for (String g : formatter.getGeometry(r)) {
                    wr.println("    geometry : " + g);
                }
                break;
            default:
                throw new RuntimeException();
            }
        }

        void open() throws Exception {
            File f = new File("/data/tmp/ooo/" + Lat.unhac(Lat.lat(path, false)).replace(' ', '_'));
            f.getParentFile().mkdirs();
            wr = new PrintStream(new BufferedOutputStream(new FileOutputStream(f)), false, "UTF-8");
        }

        void close() {
            if (wr != null) {
                wr.close();
            }
        }
    }

    static class Rehijon {
        final String path;
        final FastArea area;

        public Rehijon(String path, FastArea area) {
            this.path = path;
            this.area = area;
        }
    }
}
