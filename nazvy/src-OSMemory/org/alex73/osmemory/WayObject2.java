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
 * Way object representation.
 */
public class WayObject2 extends BaseObject2 {
    final public long[] nodeIds;

    public WayObject2(long id, int tagsCount, long[] nodeIds) {
        super(id, tagsCount);
        this.nodeIds = nodeIds;
    }

    @Override
    public byte getType() {
        return TYPE_WAY;
    }

    @Override
    public String getCode() {
        return "w" + id;
    }

    public static String getCode(long id) {
        return "w" + id;
    }
}
