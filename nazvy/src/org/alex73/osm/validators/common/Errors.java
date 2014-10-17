package org.alex73.osm.validators.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.alex73.osmemory.IOsmObject;

public class Errors {
    public Map<String, Set<String>> errors = new HashMap<>();

    public void addError(String text) {
        addError(text, (String) null);
    }

    public void addError(String text, IOsmObject obj) {
        addError(text, obj.getObjectCode());
    }

    public void addError(String text, String code) {
        Set<String> objects = errors.get(text);
        if (objects == null) {
            objects = new HashSet<>();
            errors.put(text, objects);
        }
        if (code != null) {
            objects.add(code);
        }
    }

    public int getObjectsCount() {
        int r = 0;
        for (Set<String> os : errors.values()) {
            r += os.size();
        }
        return r;
    }
}
