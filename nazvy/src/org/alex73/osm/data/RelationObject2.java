package org.alex73.osm.data;

public class RelationObject2 extends BaseObject2 {
    final long memberIDs[];
    final byte memberTypes[];
    final short memberRoles[];

    public RelationObject2(MemoryStorage2 storage, long id, int tagsCount, int memberCount) {
        super(storage, id, tagsCount);
        memberIDs = new long[memberCount];
        memberTypes = new byte[memberCount];
        memberRoles = new short[memberCount];
    }

    @Override
    public byte getType() {
        return TYPE_RELATION;
    }

    @Override
    public String getCode() {
        return "r" + id;
    }

    public static String getCode(long id) {
        return "r" + id;
    }
}
