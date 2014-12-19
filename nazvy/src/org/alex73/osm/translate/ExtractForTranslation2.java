package org.alex73.osm.translate;

import gen.alex73.osm.validators.objects.GranularityType;
import gen.alex73.osm.validators.objects.ObjectTypes;
import gen.alex73.osm.validators.objects.Type;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.alex73.osm.monitors.export.Borders;
import org.alex73.osm.utils.Belarus;
import org.alex73.osm.utils.Env;
import org.alex73.osm.validators.objects.CheckType;
import org.alex73.osmemory.IOsmObject;

public class ExtractForTranslation2 {
    static Belarus osm;
    static Borders borders;
    static List<CheckType> checkTypes;

    public static void main(String[] args) throws Exception {
        Locale.setDefault(new Locale("en", "US"));

        init();

        TranslationProcessor proc = new TranslationProcessor(osm, borders);

        List<IOsmObject> queue = new ArrayList<>();
        osm.all(o -> queue.add(o));
        queue.parallelStream().filter(o -> o.getTags().length > 0 && osm.contains(o))
                .forEach(o -> proc.process(o));

        proc.save();
    }

    static void init() throws Exception {
        String borderFile = Env.readProperty("monitoring.gitdir") + "/miezy.properties";
        osm = new Belarus();
        borders = new Borders(borderFile, osm);

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
}
