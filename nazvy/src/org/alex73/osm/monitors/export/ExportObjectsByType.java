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
import org.alex73.osmemory.geometry.AdaptiveFastArea;
import org.alex73.osmemory.geometry.Area;
import org.alex73.osmemory.geometry.Way;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Экспартуе аб'екты па тыпах.
 */
public class ExportObjectsByType {
    static Belarus osm;
    static OutputFormatter formatter;

    static List<CheckType> checkTypes;
    static Map<GranularityType, List<Rehijon>> rehijony = new TreeMap<>();
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

        Envelope bound;
        if (obj.isWay()) {
            bound = new Way((IOsmWay) obj, osm).getBoundingBox();
        } else {
            bound = null;
        }
        for (Rehijon r : rehijony.get(granularity)) {
            if (bound != null && !r.area.mayCovers(bound)) {
                continue;
            }
            if (r.area.covers(obj)) {
                found = true;
                String path = r.path + fn;
                storeToFile(obj, path);
            }
        }
        return found;
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
        rehijony.put(GranularityType.KRAINA, new ArrayList<>());
        rehijony.put(GranularityType.VOBLASC, new ArrayList<>());
        rehijony.put(GranularityType.RAJON, new ArrayList<>());
        rehijony.put(GranularityType.MIESTA, new ArrayList<>());

        rehijony.get(GranularityType.KRAINA).add(
                new Rehijon("/", new AdaptiveFastArea(osm.getGeometry(), osm)));

        List<PadzielOsmNas> padziel = new CSV('\t').readCSV(Env.readProperty("dav") + "/Rehijony.csv",
                PadzielOsmNas.class);
        for (PadzielOsmNas p : padziel) {
            if (p.voblasc == null) {
                continue;
            }
            AdaptiveFastArea area = new AdaptiveFastArea(
                    new Area(osm, osm.getRelationById(p.relationID)).getGeometry(), osm);
            if (p.rajon == null) {
                // вобласьць
                String path = p.voblasc + " вобласць" + '/';
                rehijony.get(GranularityType.VOBLASC).add(new Rehijon(path, area));
            } else {
                // раён
                String path = p.voblasc + " вобласць" + '/' + p.rajon + " раён" + '/';
                rehijony.get(GranularityType.RAJON).add(new Rehijon(path, area));
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
            for (String oc : m.osmIDother.split(";")) {
                IOsmObject o = osm.getObject(oc);
                if (o == null) {
                    continue;
                }
                if (g == null) {
                    g = new Area(osm, o).getGeometry();
                } else {
                    g = g.union(new Area(osm, o).getGeometry());
                }
            }
            if (g != null) {
                rehijony.get(GranularityType.MIESTA).add(new Rehijon(path, new AdaptiveFastArea(g, osm)));
            }
        }
        for (GranularityType g : rehijony.keySet()) {
            System.out.println(g + " " + rehijony.get(g).size());
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
        final AdaptiveFastArea area;

        public Rehijon(String path, AdaptiveFastArea area) {
            this.path = path;
            this.area = area;
        }
    }
}
