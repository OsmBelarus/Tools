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
package org.alex73.osm.validators.vulicy;

import java.util.ArrayList;
import java.util.List;

public abstract class Group<T> {
    final private String key;
    List<T> objects = new ArrayList<>();

    public Group(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void add(T data) {
        objects.add(data);
    }

    public List<T> getObjects() {
        return objects;
    }

    interface Keyer<T> {
        String getKey(T data);
    }

    interface Creator<T> {
        Group<T> create(T data);
    }
}
