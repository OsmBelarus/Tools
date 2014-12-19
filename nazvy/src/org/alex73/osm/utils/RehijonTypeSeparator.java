package org.alex73.osm.utils;

import gen.alex73.osm.validators.objects.GranularityType;
import gen.alex73.osm.validators.objects.ObjectTypes;
import gen.alex73.osm.validators.objects.Type;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.alex73.osm.monitors.export.Borders;
import org.alex73.osm.validators.objects.CheckType;
import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.geometry.IExtendedObject;
import org.alex73.osmemory.geometry.OsmHelper;

public abstract class RehijonTypeSeparator {
    protected final Belarus osm;
    protected final Borders borders;
    protected final List<CheckType> checkTypes;

    public RehijonTypeSeparator(Belarus osm, Borders borders) throws Exception {
        this.osm = osm;
        this.borders = borders;
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(new StreamSource(new File("src/object_types.xsd")));
        JAXBContext CTX = JAXBContext.newInstance(ObjectTypes.class);
        Unmarshaller unm = CTX.createUnmarshaller();
        unm.setSchema(schema);

        checkTypes = new ArrayList<>();

        File[] configs = new File("object-types").listFiles();
        Arrays.sort(configs);
        for (File f : configs) {
            ObjectTypes config = (ObjectTypes) unm.unmarshal(f);
            for (Type t : config.getType()) {
                checkTypes.add(new CheckType(osm, t));
            }
        }

        Type t = new Type();
        t.setId("other");
        t.setFile("insyja");
        t.setImportance(GranularityType.MIESTA);
        checkTypes.add(new CheckType(osm, t));
    }

    /**
     * Апрацоўвае 1 аб'ект.
     */
    public void process(IOsmObject obj) {
        try {
            IExtendedObject ext = OsmHelper.extendedFromObject(obj, osm);
            for (CheckType ct : checkTypes) {
                if (ct.matches(obj)) {
                    GranularityType g = ct.getType().getImportance();
                    while (!store(g, ext, ct.getType().getFile())) {
                        g = upper(g);
                        if (g == null) {
                            // па-за зьмененымі межамі
                            break;
                        }
                    }
                    break;
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    protected boolean store(GranularityType granularity, IExtendedObject ext, String typ) throws Exception {
        boolean found = false;

        for (Borders.Border b : getRehijony(granularity, borders)) {
            if (b.area.covers(ext)) {
                found = true;
                storeToOutput(ext.getObject(), typ, b.name);
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

    abstract protected void storeToOutput(IOsmObject obj, String typ, String rehijon) throws Exception;
}
