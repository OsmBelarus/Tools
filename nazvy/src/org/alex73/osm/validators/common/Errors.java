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

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.alex73.osmemory.IOsmObject;

/**
 * Памылкі з кодамі аб'ектаў.
 */
public class Errors {
    public static Locale BE = new Locale("be");
    public static Collator BEL = Collator.getInstance(BE);

    transient private Map<String, Err> index = new HashMap<>();
    public List<Err> errors = new ArrayList<>();
    private int rowsCount;
    private int objectsCount;

    public void addError(String text) {
        addError(text, (String) null);
    }

    public void addError(String text, IOsmObject obj) {
        addError(text, obj.getObjectCode());
    }

    public void addError(String text, String code) {
        Err err = index.get(text);
        if (err == null) {
            err = new Err();
            err.name = text;
            errors.add(err);
            index.put(text, err);
        }
        if (code != null) {
            err.objects.add(code);
            objectsCount++;
        }
    }

    public int getObjectsCount() {
        return objectsCount;
    }

    public int getRowsCount() {
        return errors.size();
    }

    public Object getJS() {
        Collections.sort(errors, new Comparator<Err>() {
            @Override
            public int compare(Err o1, Err o2) {
                return BEL.compare(o1.name, o2.name);
            }
        });
        rowsCount = errors.size();

        return this;
    }

    public static class Err {
        public String name;
        public Set<String> objects = new TreeSet<>();
    }
}
