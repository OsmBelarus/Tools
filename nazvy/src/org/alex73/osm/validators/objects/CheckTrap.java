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

import gen.alex73.osm.validators.objects.Trap;

import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.IOsmWay;
import org.alex73.osmemory.MemoryStorage;
import org.alex73.osmemory.geometry.OsmHelper;
import org.alex73.osmemory.geometry.ExtendedWay;

/**
 * Сюды трапляюць аб'екты нявызначанага тыпу. Толькі каб паказаць іх як памылку, альбо спраўдзіць геамэтрыю.
 */
public class CheckTrap extends BaseCheck {
    private final Trap trap;

    public CheckTrap(MemoryStorage osm, Trap trap) throws Exception {
        super(osm, trap);
        this.trap = trap;
    }

    public Trap getTrap() {
        return trap;
    }

    public void getErrors(IOsmObject obj) {
        if (trap.getRequired() == null) {
            CheckObjects.addError(obj, trap.getMessage());
        } else {
            switch (trap.getRequired().getGeometryType()) {
            case LINE:
                if (!obj.isWay()) {
                    CheckObjects.addError(obj, "Чакаецца way");
                } else {
                    try {
                        new ExtendedWay((IOsmWay) obj, osm).getLine();
                    } catch (Exception ex) {
                        CheckObjects.addError(obj, trap.getMessage());
                    }
                }
                break;
            case AREA:
                if (obj.isNode()) {
                    CheckObjects.addError(obj, "Чакаецца way альбо relation");
                } else {
                    try {
                        OsmHelper.areaFromObject(obj, osm);
                    } catch (Exception ex) {
                        CheckObjects.addError(obj, trap.getMessage());
                    }
                }
                break;
            case POINT:
                if (!obj.isNode()) {
                    CheckObjects.addError(obj, "Чакаецца node");
                }
                break;
            default:
                throw new RuntimeException();
            }
        }
    }
}
