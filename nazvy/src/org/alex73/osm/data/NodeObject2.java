package org.alex73.osm.data;

public class NodeObject2 extends BaseObject2 {
    public static final double DIVIDER = 0.0000001;
    /**
     * Latitude and longitude stored as integer, like in o5m. It allows to minimize memory and increase
     * performance in some cases. All coordinates in OSM stored with 7 digits after point precision.
     */
    final public int lat, lon;

    public NodeObject2(MemoryStorage2 storage, long id, int tagsCount, int lat, int lon) {
        super(storage, id, tagsCount);
        this.lat = lat;
        this.lon = lon;
    }

    /**
     * Returns latitude degree.
     */
    public double getLatitude() {
        return lat / DIVIDER;
    }

    /**
     * Returns longitude degree.
     */
    public double getLongitude() {
        return lon / DIVIDER;
    }

    @Override
    public byte getType() {
        return TYPE_NODE;
    }

    @Override
    public String getCode() {
        return "n" + id;
    }

    public static String getCode(long id) {
        return "n" + id;
    }
}
