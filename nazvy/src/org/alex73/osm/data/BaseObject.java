/**************************************************************************
 Some tools for OSM.

 Copyright (C) 2013 Aleś Bułojčyk <alex73mail@gmail.com>
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

    public String[] getTagNames() {
        String[] r = new String[tags.length / 2];
        for (int i = 0; i < r.length; i++) {
            r[i] = tags[i * 2];
        }
        return r;
    }

    @Override
    public int compareTo(BaseObject o) {
        return Long.compare(id, o.id);
    }
}
