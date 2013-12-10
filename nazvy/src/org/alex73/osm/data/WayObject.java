package org.alex73.osm.data;

public class WayObject extends BaseObject {
    final public long[] nodeIds;

    public WayObject(long id, String[] tags, long[] nodeIds) {
        super(id, tags);
        this.nodeIds = nodeIds;
    }

    @Override
    public TYPE getType() {
        return TYPE.WAY;
    }

    @Override
    public String getCode() {
        return "w" + id;
    }
}
