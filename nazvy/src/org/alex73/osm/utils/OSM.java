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

package org.alex73.osm.utils;

public class OSM {

    public static String browse(String code) {
        return "<a href='http://www.openstreetmap.org/"
                + code.replace("n", "node/").replace("w", "way/").replace("r", "relation/") + "'>" + code + "</a>";
    }

    public static String hist(String code) {
        return "<a href='http://www.openstreetmap.org/"
                + code.replace("n", "node/").replace("w", "way/").replace("r", "relation/") + "/history'>" + code
                + "</a>";
    }
}
