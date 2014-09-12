package org.alex73.osm.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Class for store some frequently used strings. Each string has own short id, that used in object instead
 * direct string usage.
 */
public class StringPack {
    final protected Map<String, Short> tagCodes = new HashMap<>();

    public short getTagCode(String tagKey) {
        synchronized (tagCodes) {
            Short v = tagCodes.get(tagKey);
            short result;
            if (v == null) {
                result = (short) tagCodes.size();
                if (result >= Short.MAX_VALUE) {
                    throw new RuntimeException("Too many tag keys: more than " + Short.MAX_VALUE);
                }
                tagCodes.put(tagKey, result);
            } else {
                result = v;
            }
            return result;
        }
    }

}
