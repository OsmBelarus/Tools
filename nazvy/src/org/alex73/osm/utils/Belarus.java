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

import java.io.File;

import org.alex73.osmemory.IOsmNode;
import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.MemoryStorage;
import org.alex73.osmemory.O5MReader;
import org.alex73.osmemory.geometry.ExtendedRelation;
import org.alex73.osmemory.geometry.FastArea;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Пашырэньне MemoryStorage менавіта для Беларусі.
 */
public class Belarus extends MemoryStorage {
    public static final long BELARUS_BORDER_RELATION_ID = 59065;

    public static double LATITUDE_SIZE = 111.321;
    public static double LONGTITUDE_BELARUS_SIZE = 67.138;

    public static double MIN_LAT = 51.2575982 - 0.001;
    public static double MAX_LAT = 56.1722235 + 0.001;
    public static double MIN_LON = 23.1783874 - 0.001;
    public static double MAX_LON = 32.7627809 + 0.001;

    private ExtendedRelation area;
    private FastArea fastArea;

    public Belarus() throws Exception {
        this(Env.readProperty("data.file"));
    }

    public Belarus(String file) throws Exception {
        new O5MReader(this, MIN_LAT, MAX_LAT, MIN_LON, MAX_LON).read(new File(file));
        showStat();

        if (nodes.size() < 250000 || ways.size() < 1400000 || relations.size() < 10000) {
            throw new Exception("Wrong o5m data");
        }

        area = new ExtendedRelation(getRelationById(BELARUS_BORDER_RELATION_ID), this);
        fastArea = new FastArea(area.getArea(), this) {
            @Override
            protected boolean isSkipped(IOsmNode node) {
                return area.getBorderNodes().contains(node.getId());
            }
        };
    }

    /**
     * Checks if object inside Belarus, but not part of border.
     */
    public boolean contains(IOsmObject obj) {
        // use special FastArea
        return fastArea.covers(obj);
    }

    public Geometry getGeometry() {
        return area.getArea();
    }

    public double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dx = Math.abs(lon1 - lon2) * LONGTITUDE_BELARUS_SIZE;
        double dy = Math.abs(lat1 - lat2) * LATITUDE_SIZE;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
