/**************************************************************************
 Some tools for OSM.

 Copyright (C) 2013-2014 Aleś Bułojčyk <alex73mail@gmail.com>
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

/**
 * Зьвесткі з файла rehijony.csv.
 */
public class PadzielOsmNas {
    public String iso_3166_2;
    public long relationID;
    public String voblasc;
    public String rajon;
    public String osmName;
    public String osmNameRu;
    public String harady;

    @Override
    public String toString() {
        if (rajon != null) {
            return voblasc + "/" + rajon;
        } else if (voblasc != null) {
            return voblasc;
        } else {
            return "Беларусь";
        }
    }
}
