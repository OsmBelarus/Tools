/**************************************************************************
 OSMemory library for OSM data processing.

 Copyright (C) 2014 Aleś Bułojčyk <alex73mail@gmail.com>
               Home page: http://www.omegat.org/
               Support center: http://groups.yahoo.com/group/OmegaT/

 This is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This software is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/

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
public abstract class BaseObject2 {
    public static final byte TYPE_NODE = 1;
    public static final byte TYPE_WAY = 2;
    public static final byte TYPE_RELATION = 3;

    private static final short[] EMPTY_TAG_KEYS = new short[0];
    private static final String[] EMPTY_TAG_VALUES = new String[0];

    protected final long id;
    protected final short[] tagKeys;
    protected final String[] tagValues;

    public BaseObject2(long id, int tagsCount) {
        this.id = id;

        if (tagsCount == 0) {
            tagKeys = EMPTY_TAG_KEYS;
            tagValues = EMPTY_TAG_VALUES;
        } else {
            tagKeys = new short[tagsCount];
            tagValues = new String[tagsCount];
        }
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
