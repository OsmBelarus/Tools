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

package org.alex73.osm.monitors.export;

import java.io.File;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.alex73.osmemory.O5MDriver;
import org.alex73.osmemory.O5MReader;

/**
 * Зьвесткі аб файле o5m.
 */
public class DataFileStatus {

    public long initialDate;
    public Set<Long> knownChangesets = new HashSet<>();

    public DataFileStatus(String file) throws Exception {
        new O5MDriver(new O5MReader() {
            @Override
            protected void fileTimestamp(long timestamp) {
                initialDate = timestamp;
            }

            @Override
            protected void createNode(O5MDriver driver, long id, int lat, int lon, String user) {
                knownChangesets.add(driver.getCurrentChangeset());
            }

            @Override
            protected void createWay(O5MDriver driver, long id, long[] nodes, String user) {
                knownChangesets.add(driver.getCurrentChangeset());
            }

            @Override
            protected void createRelation(O5MDriver driver, long id, long[] memberIds, byte[] memberTypes,
                    String user) {
                knownChangesets.add(driver.getCurrentChangeset());
            }
        }).read(new File(file));
    }

    public void dump() {
        System.out.println("Initial date: " + new Date(initialDate).toGMTString());
        System.out.println("Known changesets count: " + knownChangesets.size());
    }
}
