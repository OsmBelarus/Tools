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

package org.alex73.osm.validators.objects;

import org.alex73.osm.utils.Belarus;
import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.MemoryStorage;
import org.alex73.osmemory.geometry.OsmHelper;

import com.vividsolutions.jts.geom.Geometry;

public class CustomCheckHouse implements ICustomClass {
    public static double LATITUDE_SIZE = 111.321;
    public static double LONGTITUDE_BELARUS_SIZE = 67.138;

    MemoryStorage osm;

    @Override
    public void init(Belarus osm) throws Exception {
        this.osm = osm;
    }

    public void checkSize(IOsmObject obj) throws Exception {
        Geometry geo;
        try {
            geo = OsmHelper.areaFromObject(obj, osm);
        } catch (Exception ex) {
            CheckObjects.addError(obj, "Няправільная геамэтрыя будынку");
            return;
        }
        if (geo.getEnvelopeInternal().getWidth() * LONGTITUDE_BELARUS_SIZE > 0.4) {
            CheckObjects.addError(obj, "Будынак большы за 400 м");
        }
        if (geo.getEnvelopeInternal().getHeight() * LATITUDE_SIZE > 0.4) {
            CheckObjects.addError(obj, "Будынак большы за 400 м");
        }
    }

    @Override
    public void finish() throws Exception {
    }
}
