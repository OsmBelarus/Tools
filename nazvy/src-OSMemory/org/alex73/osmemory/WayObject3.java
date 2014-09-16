package org.alex73.osmemory;

/**
 * Way object representation.
 */
public class WayObject3 extends BaseObject3 {
    final public int nodesListStart;

    public WayObject3(long id, short[] tagKeys, int[] tagValuePositions, int nodesListStart) {
        super(id, tagKeys,  tagValuePositions);
        this.nodesListStart = nodesListStart;
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