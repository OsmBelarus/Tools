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

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.alex73.osm.utils.Belarus;
import org.alex73.osm.utils.Lat;
import org.alex73.osm.validators.objects.CheckType;
import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.geometry.FastArea;
import org.alex73.osmemory.geometry.IExtendedObject;
import org.alex73.osmemory.geometry.OsmHelper;

/**
 * Экспартуе аб'екты па тыпах.
 */
public class ExportObjectsByType {
    final ObjectTypes config;
    Belarus osm;
    Borders borders;

    List<CheckTypeWithFile> checkTypes;
    Map<String, ExportOutput> outputs;

    public ExportObjectsByType() throws Exception {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(new StreamSource(new File("src/object_types.xsd")));
        JAXBContext CTX = JAXBContext.newInstance(ObjectTypes.class);
        Unmarshaller unm = CTX.createUnmarshaller();
        unm.setSchema(schema);
        config = (ObjectTypes) unm.unmarshal(new File("object-types.xml"));
    }

    public void export(Belarus osm, Borders borders, GitClient git) throws Exception {
        this.osm = osm;
        this.borders = borders;

        checkTypes = new ArrayList<>();
        outputs = new HashMap<>();
        for (ExportOutput eo : ExportOutput.listExist(osm)) {
            outputs.put(eo.path, eo);
        }
        for (Type t : config.getType()) {
            if (t.isMain()) {
                checkTypes.add(new CheckTypeWithFile(osm, t));
            }
        }

        Type t = new Type();
        t.setId("other");
        t.setMonitoring(GranularityType.MIESTA);
        checkTypes.add(new CheckTypeWithFile(osm, t));

        osm.all(o -> osm.contains(o), o -> process(o));

        System.out.println(new Date() + " Write...");
        for (ExportOutput o : outputs.values()) {
            o.save(git);
        }
    }

    void process(IOsmObject obj) {
        try {
            IExtendedObject ext = OsmHelper.extendedFromObject(obj, osm);
            for (CheckTypeWithFile ct : checkTypes) {
                if (ct.matches(obj)) {
                    GranularityType g = ct.getType().getMonitoring();
                    while (!store(g, ext, ct.getFilename())) {
                        g = upper(g);
                        if (g == null) {
                            throw new RuntimeException();
                        }
                    }
                    break;
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    boolean store(GranularityType granularity, IExtendedObject ext, String fn) throws Exception {
        final StoreFound result = new StoreFound();

        for (Borders.Border b : getRehijony(granularity, borders)) {
            if (b.area.covers(ext)) {
                result.found = true;
                storeToQueue(ext.getObject(), b.name + fn);
            }
        }
        // getRehijony(granularity,
        // borders).parallelStream().filter(b->b.area.covers(ext)).forEach(b->{result.found = true;
        // storeToQueue(ext.getObject(), b.name + fn);});
        return result.found;
    }

    static class StoreFound {
        volatile boolean found = false;
    }

    static List<Borders.Border> getRehijony(GranularityType granularity, Borders borders) {
        switch (granularity) {
        case KRAINA:
            return borders.kraina;
        case VOBLASC:
            return borders.voblasci;
        case RAJON:
            return borders.rajony;
        case MIESTA:
            return borders.miesty;
        default:
            throw new RuntimeException();
        }
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

    void storeToQueue(IOsmObject obj, String path) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        ExportOutput o = outputs.get(path);
        if (o == null) {
            // new file
            o = new ExportOutput(path, osm);
            outputs.put(path, o);
        }
        o.out(obj);
    }

    class CheckTypeWithFile extends CheckType {
        String fn;

        public CheckTypeWithFile(Belarus osm, Type type) throws Exception {
            super(osm, type);
            fn = Lat.unhac(Lat.lat(getType().getId() + ".txt", false)).replace(' ', '_');
        }

        public String getFilename() {
            return fn;
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
