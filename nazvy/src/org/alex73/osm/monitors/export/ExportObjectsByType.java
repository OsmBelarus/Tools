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
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.alex73.osm.utils.Belarus;
import org.alex73.osm.validators.objects.CheckType;
import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.OsmSimpleNode;
import org.alex73.osmemory.XMLDriver;
import org.alex73.osmemory.geometry.FastArea;
import org.alex73.osmemory.geometry.IExtendedObject;
import org.alex73.osmemory.geometry.OsmHelper;

/**
 * Экспартуе аб'екты па тыпах.
 */
public class ExportObjectsByType implements XMLDriver.IApplyChangeCallback {
    final ObjectTypes config;
    final Belarus osm;
    Borders borders;

    List<CheckType> checkTypes;
    Map<String, ExportOutput> outputs;
    List<IOsmObject> queue = new ArrayList<>();

    public ExportObjectsByType(Belarus osm, Borders borders, Set<String> unusedFiles) throws Exception {
        this.osm = osm;
        this.borders = borders;
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(new StreamSource(new File("src/object_types.xsd")));
        JAXBContext CTX = JAXBContext.newInstance(ObjectTypes.class);
        Unmarshaller unm = CTX.createUnmarshaller();
        unm.setSchema(schema);
        config = (ObjectTypes) unm.unmarshal(new File("object-types.xml"));

        checkTypes = new ArrayList<>();
        for (Type t : config.getType()) {
            checkTypes.add(new CheckType(osm, t));
        }

        Type t = new Type();
        t.setId("other");
        t.setFile("insyja");
        t.setImportance(GranularityType.MIESTA);
        checkTypes.add(new CheckType(osm, t));

        outputs = new HashMap<>();
        for (ExportOutput eo : ExportOutput.list(checkTypes, borders, unusedFiles)) {
            outputs.put(eo.key(), eo);
        }
    }

    /**
     * Апрацоўвае ўсе аб'екты Беларусі.
     */
    public void collectData() throws Exception {
        System.out.println(new Date() + " Collect data...");

        outputs.values().forEach(o -> o.clear());
        queue.clear();
        osm.all(o -> queue.add(o));
        processQueue();
    }

    /**
     * Апрацоўвае толькі аб'екты ў чарзе.
     */
    void processQueue() {
        queue.parallelStream().filter(o -> osm.contains(o)).forEach(o -> process(o));
        queue.clear();
        fixOutput();
    }

    public void beforeUpdateNode(long id) {
        outputs.values().parallelStream().forEach(o -> o.forgetNode(id));
    }

    public void afterUpdateNode(long id) {
        IOsmObject o = osm.getNodeById(id);
        if (o != null && !(o instanceof OsmSimpleNode)) {
            queue.add(o);
        }
    }

    public void beforeUpdateWay(long id) {
        outputs.values().parallelStream().forEach(o -> o.forgetWay(id));
    }

    public void afterUpdateWay(long id) {
        IOsmObject o = osm.getWayById(id);
        if (o != null) {
            queue.add(o);
        }
    }

    public void beforeUpdateRelation(long id) {
        outputs.values().parallelStream().forEach(o -> o.forgetRelation(id));
    }

    public void afterUpdateRelation(long id) {
        IOsmObject o = osm.getRelationById(id);
        if (o != null) {
            queue.add(o);
        }
    }

    public void saveExport(GitClient git) {
        System.out.println(new Date() + " Write...");
        outputs.values().parallelStream().forEach(o -> o.save(git, osm));
    }

    public void fixOutput() {
        outputs.values().parallelStream().forEach(o -> o.finishUpdate());
    }

    /**
     * Апрацоўвае 1 аб'ект.
     */
    void process(IOsmObject obj) {
        try {
            IExtendedObject ext = OsmHelper.extendedFromObject(obj, osm);
            for (CheckType ct : checkTypes) {
                if (ct.matches(obj)) {
                    GranularityType g = ct.getType().getImportance();
                    while (!store(g, ext, ct.getType().getFile())) {
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

    boolean store(GranularityType granularity, IExtendedObject ext, String typ) throws Exception {
        boolean found = false;

        for (Borders.Border b : getRehijony(granularity, borders)) {
            if (b.area.covers(ext)) {
                found = true;
                storeToQueue(ext.getObject(), typ, b.name);
            }
        }
        return found;
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

    /**
     * Захоўвае ў адпаведны ExportOutput.
     */
    synchronized void storeToQueue(IOsmObject obj, String typ, String rehijon) throws Exception {
        String key = ExportOutput.key(typ, rehijon);
        ExportOutput o = outputs.get(key);
        if (o == null) {
            // new file
            o = new ExportOutput(typ, rehijon);
            outputs.put(key, o);
        }
        o.out(obj);
    }
}
