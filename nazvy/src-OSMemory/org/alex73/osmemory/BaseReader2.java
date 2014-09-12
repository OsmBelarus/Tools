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

import com.vividsolutions.jts.geom.Envelope;

/**
 * Base class for readers from all formats.
 */
public class BaseReader2 {
    protected MemoryStorage2 storage;
    protected int minx, maxx, miny, maxy;

    /**
     * Reader can skip nodes outside of cropbox. If cropbox is not defined, all nodes will be loaded.
     */
    protected BaseReader2(Envelope cropBox) {
        storage = new MemoryStorage2();
        if (cropBox != null) {
            minx = (int) (cropBox.getMinX() / NodeObject2.DIVIDER) - 1;
            maxx = (int) (cropBox.getMaxX() / NodeObject2.DIVIDER) + 1;
            miny = (int) (cropBox.getMinY() / NodeObject2.DIVIDER) - 1;
            maxy = (int) (cropBox.getMaxY() / NodeObject2.DIVIDER) + 1;
        } else {
            minx = Integer.MIN_VALUE;
            maxx = Integer.MAX_VALUE;
            miny = Integer.MIN_VALUE;
            maxy = Integer.MAX_VALUE;
        }
    }

    /**
     * Check if node inside crop box.
     */
    protected boolean isInsideCropBox(int lat, int lon) {
        return lon >= minx && lon <= maxx && lat >= miny && lat <= maxy;
    }
}
