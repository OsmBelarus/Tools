package org.alex73.osmemory;

/**
 * Relation object representation.
 */
public class RelationObject3 extends BaseObject3 {
    final long memberIDs[];
    final byte memberTypes[];
    final short memberRoles[];

    public RelationObject3(long id, short[] tagKeys, int[] tagValuePositions, long[] memberIDs,
            byte[] memberTypes) {
        super(id, tagKeys, tagValuePositions);
        this.memberIDs = memberIDs;
        this.memberTypes = memberTypes;
        memberRoles = new short[memberIDs.length];
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

    public int getMembersCount() {
        return memberIDs.length;
    }

    public BaseObject2 getMemberObject(MemoryStorage2 storage, int memberIndex) {
        switch (memberTypes[memberIndex]) {
        case TYPE_NODE:
            return storage.getNodeById(memberIDs[memberIndex]);
        case TYPE_WAY:
            return storage.getWayById(memberIDs[memberIndex]);
        case TYPE_RELATION:
            return storage.getRelationById(memberIDs[memberIndex]);
        default:
            throw new RuntimeException("Unknown member type");
        }
    }

    public String getMemberRole(MemoryStorage2 storage, int memberIndex) {
        return storage.getRelationRolesPack().getTagName(memberRoles[memberIndex]);
    }

    public byte getMemberType(int memberIndex) {
        return memberTypes[memberIndex];
    }

    public long getMemberID(int memberIndex) {
        return memberIDs[memberIndex];
    }
}
