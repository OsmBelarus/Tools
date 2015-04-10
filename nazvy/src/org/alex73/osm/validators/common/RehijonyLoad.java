package org.alex73.osm.validators.common;

import gen.alex73.osm.validators.rehijony.Kraina;

import java.io.File;

import javax.xml.bind.JAXBContext;

public class RehijonyLoad {
    private static JAXBContext CONTEXT;
    static {
        try {
            CONTEXT = JAXBContext.newInstance(Kraina.class);
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static Kraina kraina;

    public static void load(String file) throws Exception {
        kraina = (Kraina) CONTEXT.createUnmarshaller().unmarshal(new File(file));
    }
    
    public static void save(String file) throws Exception {
        CONTEXT.createMarshaller().marshal(kraina, new File(file));
    }

    public static void main(String[] a) throws Exception {
        load("Rehijony.xml");
    }
}
