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

import java.util.HashSet;
import java.util.Set;

import org.alex73.osm.utils.Belarus;
import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.geometry.OsmHelper;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Правярае :
 * 
 * 1) ці ўсе вобласьці знаходзяцца ў межах краіны
 * 
 * 2) ці ўсе раёны знаходзяцца ў межах краіны і вобласьці
 */
public class CustomCheckRehijony implements ICustomClass {
    static Geometry BelarusGeometry;
    static short nameTag;
    Belarus osm;
    static Set<String> processed= new HashSet<>();

    public boolean filter(IOsmObject obj) {
//        if (!rehijony.contains(obj.getObjectCode())) {
//            return false;
//        }
        return true;
    }

    public void init(Belarus osm) {
        this.osm = osm;
        nameTag = osm.getTagsPack().getTagCode("name:be");
        BelarusGeometry = osm.getGeometry();
      
    }

    @Override
    public void finish() throws Exception {
        Set<String> diff = new HashSet<>();//rehijony);
        diff.removeAll(processed);
        for (String c : diff) {
            CheckObjects.addError(c, "Неапрацаваная вобласьць ці раён");
        }
    }

    public void checkKraina(IOsmObject o) {
        processed.add(o.getObjectCode());
    }

    public void checkVoblasc(IOsmObject o) {
        processed.add(o.getObjectCode());//TODO check by xml
        Geometry voblascGeometry;
        try {
            voblascGeometry = OsmHelper.areaFromObject(o, osm);
        } catch (Exception ex) {
            CheckObjects.addError(o, "Няправільная геамэтрыя вобласьці");
            return;
        }
        if (!BelarusGeometry.contains(voblascGeometry)) {
            CheckObjects.addError(o, "Вобласьць па-за межамі краіны");
        }
    }

    public void checkRajon(IOsmObject o) {//TODO check by xml
        processed.add(o.getObjectCode());
//        Geometry voblascGeometry = voblasci.get(o.getTag("name:be", osm)).getArea();
//        if (voblascGeometry == null) {
//            CheckObjects.addError(o, "Невядомая вобласьць для раёну (мо няправільная назва раёну ?)");
//            return;
//        }
//        Geometry rajonGeometry;
//        try {
//            rajonGeometry = OsmHelper.areaFromObject(o, osm);
//        } catch (Exception ex) {
//            CheckObjects.addError(o, "Няправільная геамэтрыя раёну");
//            return;
//        }
//        if (!BelarusGeometry.contains(rajonGeometry)) {
//            CheckObjects.addError(o, "Раён па-за межамі краіны");
//        }
//
//        if (!voblascGeometry.contains(rajonGeometry)) {
//            CheckObjects.addError(o, "Раён па-за межамі вобласьці");
//        }
    }
}
