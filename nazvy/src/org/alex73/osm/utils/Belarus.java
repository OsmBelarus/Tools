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
import org.alex73.osmemory.geometry.Area;
import org.alex73.osmemory.geometry.FastArea;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Пашырэньне MemoryStorage менавіта для Беларусі. 
 */
public class Belarus extends MemoryStorage {
    public static final long BELARUS_BORDER_RELATION_ID = 59065;

    private Area area;
    private FastArea fastArea;

    public Belarus() throws Exception {
        new O5MReader(this, 51.2575982 - 0.001, 56.1722235 + 0.001, 23.1783874 - 0.001, 32.7627809 + 0.001)
                .read(new File(Env.readProperty("data.file")));
        showStat();

        if (nodes.size() < 250000 || ways.size() < 1400000 || relations.size() < 10000) {
            throw new Exception("Wrong o5m data");
        }

        area = new Area(this, getRelationById(BELARUS_BORDER_RELATION_ID));
        fastArea = new FastArea(area.getGeometry(), this) {
            @Override
            protected boolean coversNode(IOsmNode node) {
                if (area.getBorderNodes().contains(node.getId())) {
                    // border
                    return false;
                }
                return super.coversNode(node);
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
        return area.getGeometry();
    }
}
