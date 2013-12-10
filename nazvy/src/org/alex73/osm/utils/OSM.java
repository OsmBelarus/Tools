package org.alex73.osm.utils;

public class OSM {

    public static String browse(String code) {
        return "<a href='http://www.openstreetmap.org/"
                + code.replace("n", "node/").replace("w", "way/").replace("r", "relation/") + "'>" + code
                + "</a>";
    }

    public static String hist(String code) {
        return "<a href='http://www.openstreetmap.org/"
                + code.replace("n", "node/").replace("w", "way/").replace("r", "relation/") + "/history'>"
                + code + "</a>";
    }

}
