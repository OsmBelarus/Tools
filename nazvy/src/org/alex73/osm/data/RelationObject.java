package org.alex73.osm.data;

public class RelationObject extends BaseObject {
    final public Member[] members;

    public RelationObject(long id, String[] tags, Member[] members) {
        super(id, tags);
        this.members = members;
    }

    @Override
    public TYPE getType() {
        return TYPE.RELATION;
    }

    @Override
    public String getCode() {
        return "r" + id;
    }

    public static class Member {
        final public long id;
        final public String role;
        final public TYPE type;

        public Member(long id, String role, TYPE type) {
            this.id = id;
            this.role = role;
            this.type = type;
        }
    }
}
