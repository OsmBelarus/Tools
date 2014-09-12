package org.alex73.osm.data;

public class WayObject2 extends BaseObject2 {
    final public long[] nodeIds;

    public WayObject2(MemoryStorage2 storage, long id, int tagsCount, long[] nodeIds) {
        super(storage, id, tagsCount);
        this.nodeIds = nodeIds;
    }

    @Override
    public byte getType() {
        return TYPE_WAY;
    }

    @Override
    public String getCode() {
        return "w" + id;
    }

    public static String getCode(long id) {
        return "w" + id;
    }
}
