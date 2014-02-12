package org.alex73.osm.converters.bel;

import gen.alex73.osm.xmldatatypes.Node;
import gen.alex73.osm.xmldatatypes.Relation;
import gen.alex73.osm.xmldatatypes.Tag;
import gen.alex73.osm.xmldatatypes.Way;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.alex73.osm.utils.Env;
import org.alex73.osm.utils.Lat;
import org.alex73.osm.validators.vulicy2.OsmHouseStreet;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;



/**
 * Пераносіць беларускія назвы ў стандартныя каб стварыць мапу для OsmAnd.
 */
public class Convert {
    static final int BUFFER_SIZE = 1024 * 1024;
    static Set<String> uniqueTranslatedTags = new TreeSet<>();
    static Map<Long,String> houseStreetBe = new HashMap<>();

    public static void main(String[] args) throws Exception {
        loadStreetNamesForHouses();

        InputStream in = new BZip2CompressorInputStream(new BufferedInputStream(new FileInputStream(
                "tmp/belarus-latest.osm.bz2"), BUFFER_SIZE));

        // create xml event reader for input stream
        XMLEventFactory eventFactory = XMLEventFactory.newInstance();
        XMLEvent newLine = eventFactory.createCharacters("\n");
        XMLInputFactory xif = XMLInputFactory.newInstance();
        XMLOutputFactory xof = XMLOutputFactory.newInstance();
        XMLEventReader reader = xif.createXMLEventReader(in);
        XMLEventWriter wrCyr = xof.createXMLEventWriter(new BufferedOutputStream(new FileOutputStream(
                "tmp/belarus-bel.osm"), BUFFER_SIZE));
        XMLEventWriter wrInt = xof.createXMLEventWriter(new BufferedOutputStream(new FileOutputStream(
                "tmp/belarus-intl.osm"), BUFFER_SIZE));
 
        // initialize jaxb
        JAXBContext jaxbCtx = JAXBContext.newInstance(Node.class, Way.class, Relation.class);
        Unmarshaller um = jaxbCtx.createUnmarshaller();
        Marshaller m = jaxbCtx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FRAGMENT, true);
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        XMLEvent e = null;
        while ((e = reader.peek()) != null) {
            boolean processed = false;
            if (e.isStartElement()) {
                StartElement se = (StartElement) e;
                switch (se.getName().getLocalPart()) {
                case "way":
                    Way way = um.unmarshal(reader, Way.class).getValue();
                    if (way.getId()==25439425) {
                        System.out.println();
                    }
                    fixBel(way.getTag(), "name:be", "name");
                    String nameBeHouse = houseStreetBe.get(way.getId());
                    if (nameBeHouse != null) {
                        setTag(way.getTag(), "addr:street", nameBeHouse);
                    }
                    m.marshal(way, wrCyr);
                    fixInt(way.getTag());
                    m.marshal(way, wrInt);
                    wrCyr.add(newLine);
                    wrInt.add(newLine);
                    processed = true;
                    break;
                case "node":
                    Node node = um.unmarshal(reader, Node.class).getValue();
                    fixBel(node.getTag(),"name:be","name");
                    //fixBel(node.getTag(),"addr:street:be","addr:street");
                    m.marshal(node, wrCyr);
                    fixInt(node.getTag());
                    m.marshal(node, wrInt);
                    wrCyr.add(newLine);
                    wrInt.add(newLine);
                    processed = true;
                    break;
                case "relation":
                    Relation relation = um.unmarshal(reader, Relation.class).getValue();
                    fixBel(relation.getTag(),"name:be","name");
                    //fixBel(relation.getTag(),"addr:street:be","addr:street");
                    m.marshal(relation, wrCyr);
                    fixInt(relation.getTag());
                    m.marshal(relation, wrInt);
                    wrCyr.add(newLine);
                    wrInt.add(newLine);
                    processed = true;
                    break;
                }
            }
            if (!processed) {
                wrCyr.add(e);
                wrInt.add(e);
            }
            reader.next();
        }

        wrCyr.flush();
        wrCyr.close();
        wrInt.flush();
        wrInt.close();
        System.out.println("UniqueTranslatedTags: " + uniqueTranslatedTags);
    }

    static void loadStreetNamesForHouses() throws Exception {
        Env.load("../nazvy/env.properties");
        SqlSession db;
        String resource = "osm.xml";
        SqlSessionFactory sqlSessionFactory;
        InputStream inputStream = Resources.getResourceAsStream(resource);
        try {
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream, Env.env);
        } finally {
            inputStream.close();
        }
        db = sqlSessionFactory.openSession();
        List<OsmHouseStreet> list = db.selectList("getStreetNameBeForAllHouses");
        for (OsmHouseStreet h : list) {
            if (h.name_be != null) {
                houseStreetBe.put(h.hid, h.name_be);
            }
        }
    }

    static void fixInt(List<Tag> tags) {
        for (int i = 0; i < tags.size(); i++) {
            Tag tag = tags.get(i);
            if ("name".equals(tag.getK())) {
                tag.setV(Lat.lat(tag.getV(), false));
            }
        }
    }

    static void setTag(List<Tag> tags, String tagName, String tagValue) {
        for (int i = 0; i < tags.size(); i++) {
            if (tagName.equals(tags.get(i).getK())) {
                tags.get(i).setV(tagValue);
                break;
            }
        }
    }

    static void fixBel(List<Tag> tags, String name_be_tag, String name_tag) {
        // find name:be
        Tag name_be = null;
        for (int i = 0; i < tags.size(); i++) {
            if (name_be_tag.equals(tags.get(i).getK())) {
                name_be = tags.remove(i);
                i--;
            }
        }
        if (name_be != null) {
            for (int i = 0; i < tags.size(); i++) {
                if (name_tag.equals(tags.get(i).getK())) {
                    tags.remove(i);
                    i--;
                }
            }
            name_be.setK(name_tag);
            tags.add(name_be);
        }
    }
}
