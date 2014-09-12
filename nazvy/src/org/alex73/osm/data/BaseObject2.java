package org.alex73.osm.data;

/**
 * Base class for node/way/relation objects. Type defined as byte instead enum for minimize memory usage.
 */
public abstract class BaseObject2 {
    public static final byte TYPE_NODE = 1;
    public static final byte TYPE_WAY = 2;
    public static final byte TYPE_RELATION = 3;

    private static final short[] EMPTY_TAG_KEYS = new short[0];
    private static final String[] EMPTY_TAG_VALUES = new String[0];

    final public long id;
    protected final short[] tagKeys;
    protected final String[] tagValues;

    public BaseObject2(MemoryStorage2 storage, long id, int tagsCount) {
        this.id = id;

        if (tagsCount == 0) {
            tagKeys = EMPTY_TAG_KEYS;
            tagValues = EMPTY_TAG_VALUES;
        } else {
            tagKeys = new short[tagsCount];
            tagValues = new String[tagsCount];
        }
    }

    abstract public byte getType();

    abstract public String getCode();
}
