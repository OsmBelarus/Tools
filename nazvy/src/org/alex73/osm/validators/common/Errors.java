/**************************************************************************
 Some tools for OSM.

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

package org.alex73.osm.validators.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.alex73.osmemory.IOsmObject;

/**
 * Памылкі з кодамі аб'ектаў.
 */
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
