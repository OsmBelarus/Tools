package org.alex73.osmemory;

/**
 * Base class for node/way/relation objects.
 * 
 * Memory and performance optimization:
 * 
 * - type defined as byte instead enum
 * 
 * - tag key stored as index instead string value
 */
public abstract class BaseObject3 {
    public static final byte TYPE_NODE = 1;
    public static final byte TYPE_WAY = 2;
    public static final byte TYPE_RELATION = 3;

    public static final short[] EMPTY_TAG_KEYS = new short[0];
    public static final int[] EMPTY_TAG_VALUEPOSITIONS = new int[0];

    protected final long id;
    protected final short[] tagKeys;
    protected final int[] tagValuePositions;

    public BaseObject3(long id, short[] tagKeys, int[] tagValuePositions) {
        this.id = id;
        this.tagKeys = tagKeys;
        this.tagValuePositions = tagValuePositions;
    }

    public long getId() {
        return id;
    }

    public boolean hasTag(short tagKey) {
        for (int i = 0; i < tagKeys.length; i++) {
            if (tagKeys[i] == tagKey) {
                return true;
            }
        }
        return false;
    }

    public String getTagValue(short tagKey) {
        for (int i = 0; i < tagKeys.length; i++) {
            if (tagKeys[i] == tagKey) {
                return tagValues[i];
            }
        }
        return null;
    }

    abstract public byte getType();

    abstract public String getCode();
}
