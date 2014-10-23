package org.alex73.osm.utils;


public class GeoUtils {
    public static double LATITUDE_SIZE = 111.321;
    public static double LONGTITUDE_BELARUS_SIZE = 67.138;

    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dx = Math.abs(lon1 - lon2) * LONGTITUDE_BELARUS_SIZE;
        double dy = Math.abs(lat1 - lat2) * LATITUDE_SIZE;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
