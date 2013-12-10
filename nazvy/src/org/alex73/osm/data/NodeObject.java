package org.alex73.osm.data;


public class NodeObject extends BaseObject {
    final public double lat, lon;

    public NodeObject(long id, String[] tags, double lat, double lon) {
        super(id, tags);
        this.lat = lat;
        this.lon = lon;
    }

    @Override
    public TYPE getType() {
        return TYPE.NODE;
    }

    @Override
    public String getCode() {
        return "n" + id;
    }
}
