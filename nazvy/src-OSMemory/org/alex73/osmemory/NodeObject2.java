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
 * Node object representation.
 */
public class NodeObject2 extends BaseObject2 {
    public static final double DIVIDER = 0.0000001;
    /**
     * Latitude and longitude stored as integer, like in o5m. It allows to minimize memory and increase
     * performance in some cases. All coordinates in OSM stored with 7 digits after point precision.
     */
    final public int lat, lon;

    public NodeObject2(long id, int tagsCount, int lat, int lon) {
        super(id, tagsCount);
        this.lat = lat;
        this.lon = lon;
    }

    /**
     * Returns latitude degree.
     */
    public double getLatitude() {
        return lat * DIVIDER;
    }

    /**
     * Returns longitude degree.
     */
    public double getLongitude() {
        return lon * DIVIDER;
    }

    @Override
    public byte getType() {
        return TYPE_NODE;
    }

    @Override
    public String getCode() {
        return "n" + id;
    }

    public static String getCode(long id) {
        return "n" + id;
    }
}
