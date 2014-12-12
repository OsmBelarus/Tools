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
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.alex73.osm.utils.Belarus;
import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.XMLDriver;
import org.alex73.osmemory.XMLReader;
import org.alex73.osmemory.XMLReader.UPDATE_MODE;

import osm.xmldatatypes.Node;
import osm.xmldatatypes.Relation;
import osm.xmldatatypes.Way;

/**
 * Паказвае карыстальнікаў што выпраўлялі нейкія аб'екты ў Беларусі.
 */
public class ListUsers {
    static Belarus country;
    static Set<String> users = new HashSet<>();
    static int updateCount;

    public static void main(String[] args) throws Exception {
        country = new Belarus("/data/tmp/osm-cache/BY-141121.o5m");
        File[] files = new File("/data/tmp/osm-cache/").listFiles();
        Arrays.sort(files);
        for (File f : files) {
            if (f.getName().endsWith("20.osc.gz")) {
                updateCount = 0;
                apply(f);
            }
        }
        System.out.println(users);
    }

    static void apply(final File f) throws Exception {
        XMLReader reader = new XMLReader(country, Belarus.MIN_LAT, Belarus.MAX_LAT, Belarus.MIN_LON,
                Belarus.MAX_LON);
        new XMLDriver(reader).applyOsmChange(new GZIPInputStream(new FileInputStream(f)),
                new XMLDriver.IApplyChangeCallback() {

                    @Override
                    public void beforeUpdateNode(UPDATE_MODE mode, Node node) {
                    }

                    @Override
                    public void beforeUpdateWay(UPDATE_MODE mode, Way way) {
                    }

                    @Override
                    public void beforeUpdateRelation(UPDATE_MODE mode, Relation relation) {
                    }

                    @Override
                    public void afterUpdateNode(UPDATE_MODE mode, Node node) {
                        updateCount++;
                        if (updateCount % 10000 == 0) {
                            System.out.println(f.getName() + " " + updateCount);
                        }
                        if (!users.contains(node.getUser())) {
                            IOsmObject obj = country.getNodeById(node.getId());
                            if (obj != null && country.contains(obj)) {
                                users.add(node.getUser());
                            }
                        }
                    }

                    @Override
                    public void afterUpdateWay(UPDATE_MODE mode, Way way) {
                        updateCount++;
                        if (updateCount % 10000 == 0) {
                            System.out.println(f.getName() + " " + updateCount);
                        }
                        if (!users.contains(way.getUser())) {
                            IOsmObject obj = country.getWayById(way.getId());
                            if (obj != null && country.contains(obj)) {
                                users.add(way.getUser());
                            }
                        }
                    }

                    @Override
                    public void afterUpdateRelation(UPDATE_MODE mode, Relation relation) {
                        updateCount++;
                        if (updateCount % 10000 == 0) {
                            System.out.println(f.getName() + " " + updateCount);
                        }
                        if (!users.contains(relation.getUser())) {
                            IOsmObject obj = country.getRelationById(relation.getId());
                            if (obj != null && country.contains(obj)) {
                                users.add(relation.getUser());
                            }
                        }
                    }
                });
    }
}
