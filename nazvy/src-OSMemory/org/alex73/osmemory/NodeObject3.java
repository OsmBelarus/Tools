package org.alex73.osmemory;

/**
 * Node object representation.
 */
public class NodeObject3 extends BaseObject3 {
    public static final double DIVIDER = 0.0000001;
    /**
     * Latitude and longitude stored as integer, like in o5m. It allows to minimize memory and increase
     * performance in some cases. All coordinates in OSM stored with 7 digits after point precision.
     */
    final public int lat, lon;

    public NodeObject3(long id, short[] tagKeys, int[] tagValuePositions,int lat, int lon) {
        super(id,  tagKeys,  tagValuePositions);
        this.lat = lat;
        this.lon = lon;
    }

    /**
     * Returns latitude degree.
     */
    public double getLatitude() {
        return lat * DIVIDER;
    }

    /**
     * Returns longitude degree.
     */
    public double getLongitude() {
        return lon * DIVIDER;
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
