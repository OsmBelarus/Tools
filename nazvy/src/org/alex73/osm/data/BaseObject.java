package org.alex73.osm.data;

public abstract class BaseObject implements Comparable<BaseObject> {
    public enum TYPE {
        NODE, WAY, RELATION
    };

    final public long id;
    final private String[] tags;

    public BaseObject(long id, String[] tags) {
        this.id = id;
        this.tags = tags;
    }

    abstract public TYPE getType();

    abstract public String getCode();

    public String getTag(String tagName) {
        for (int i = 0; i < tags.length; i += 2) {
            if (tagName.equals(tags[i])) {
                return tags[i + 1];
            }
        }
        return null;
    }

    @Override
    public int compareTo(BaseObject o) {
        return Long.compare(id, o.id);
    }
}
