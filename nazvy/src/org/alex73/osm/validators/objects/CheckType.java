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

import gen.alex73.osm.validators.objects.Type;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.IOsmWay;
import org.alex73.osmemory.MemoryStorage;
import org.alex73.osmemory.geometry.OsmHelper;
import org.alex73.osmemory.geometry.ExtendedWay;

/**
 * Правярае аб'екты й тэгі для вызначанага тыпу.
 */
public class CheckType extends BaseCheck {
    public static final String OK = "OK";

    private final Type type;

    private boolean requiredNode, requiredWay, requiredRelation;
    private TagCodeValues requiredTags, possibleTags;
    private Set<String> additions;

    public CheckType(MemoryStorage osm, Type type) throws Exception {
        super(osm, type);
        this.type = type;

        if (type.getRequired() == null || type.getRequired().getOsmTypes() == null) {
            requiredNode = true;
            requiredWay = true;
            requiredRelation = true;
        } else {
            for (String ot : type.getRequired().getOsmTypes().split(",")) {
                switch (ot) {
                case "node":
                    requiredNode = true;
                    break;
                case "way":
                    requiredWay = true;
                    break;
                case "relation":
                    requiredRelation = true;
                    break;
                default:
                    throw new RuntimeException("Невядомы osmTypes: " + ot);
                }
            }
        }

        requiredTags = tagsCompile(type.getRequired());
        possibleTags = tagsCompile(type.getAllow());

        if (type.getAdditions() != null) {
            additions = new HashSet<>(Arrays.asList(type.getAdditions().split(",")));
        } else {
            additions = Collections.emptySet();
        }
    }

    public String getId() {
        return type.getId();
    }

    public Type getType() {
        return type;
    }

    public Set<String> getAdditions() {
        return additions;
    }


    public void getErrors(IOsmObject obj) {
        switch (obj.getType()) {
        case IOsmObject.TYPE_NODE:
            if (!requiredNode) {
                CheckObjects.addError(obj, "'" + type.getId() + "' можа быць "
                        + type.getRequired().getOsmTypes() + ", але не node");
            } else {
                checkGeo(obj);
            }
            break;
        case IOsmObject.TYPE_WAY:
            if (!requiredWay) {
                CheckObjects.addError(obj, "'" + type.getId() + "' можа быць "
                        + type.getRequired().getOsmTypes() + ", але не way");
            } else {
                checkGeo(obj);
            }
            break;
        case IOsmObject.TYPE_RELATION:
            if (!requiredRelation) {
                CheckObjects.addError(obj, "'" + type.getId() + "' можа быць "
                        + type.getRequired().getOsmTypes() + ", але не relation");
            } else {
                checkGeo(obj);
            }
            break;
        }

        // абавязковыя тэгі
        for (int i = 0; i < requiredTags.length(); i++) {
            if (!obj.hasTag(requiredTags.codes[i])
                    || !tagValueAllowed(requiredTags.codes[i], requiredTags.values[i], obj)) {
                String expected = type.getRequired().getTag().get(i).getValue();
                if (expected != null) {
                    CheckObjects.addError(obj, "'" + type.getId() + "' павінен утрымліваць тэг '"
                            + type.getRequired().getTag().get(i).getName() + "'='" + expected + "'");
                } else {
                    CheckObjects.addError(obj, "'" + type.getId() + "' павінен утрымліваць тэг '"
                            + type.getRequired().getTag().get(i).getName() + "'");
                }
            }
        }
        if (type.getRequired() != null && type.getRequired().getRequiredRelationType() != null
                && obj.isRelation()) {
            // тэг у requiredRelationType
            String requiredType = type.getRequired().getRequiredRelationType();
            if (!requiredType.equals(obj.getTag("type", osm))) {
                CheckObjects.addError(obj, "'" + type.getId() + "' павінен мець 'type=" + requiredType + "'");
            }
        }

        if (type.getRequired() != null && type.getRequired().getCustomMethod() != null) {
            callCustom(type.getRequired().getCustomMethod(), obj);
        }
    }

    public String matchTag(IOsmObject obj, short tagCode) {
        int p = indexOf(filterTags.codes, tagCode);
        if (p >= 0) {
            return OK;
        }

        p = indexOf(requiredTags.codes, tagCode);
        if (p >= 0) {
            if (!tagValueAllowed(requiredTags.codes[p], requiredTags.values[p], obj)) {
                return "'" + type.getId() + "' павінен утрымліваць тэг '"
                        + osm.getTagsPack().getTagName(tagCode) + "'='"
                        + type.getRequired().getTag().get(p).getValue() + "'";
            }
            return OK;
        }

        p = indexOf(possibleTags.codes, tagCode);
        if (p >= 0) {
            if (!tagValueAllowed(possibleTags.codes[p], possibleTags.values[p], obj)) {
                return "'" + type.getId() + "' павінен утрымліваць тэг '"
                        + osm.getTagsPack().getTagName(tagCode) + "'='"
                        + type.getAllow().getTag().get(p).getValue() + "'";
            }
            return OK;
        }

        return null;
    }

    void checkGeo(IOsmObject obj) {
        if (type.getRequired() == null || type.getRequired().getGeometryType() == null) {
            return;
        }

        switch (type.getRequired().getGeometryType()) {
        case AREA:
            try {
                OsmHelper.areaFromObject(obj, osm);
            } catch (Exception ex) {
                CheckObjects.addError(obj, "'" + type.getId() + "' мае няправільную геамэтрыю");
            }
            break;
        case LINE:
            if (obj.isWay()) {
                ExtendedWay way = new ExtendedWay((IOsmWay) obj, osm);
                try {
                    way.getLine();
                } catch (Exception ex) {
                    CheckObjects.addError(obj, "'" + type.getId() + "' мае няправільную геамэтрыю");
                }
            } else {
                CheckObjects.addError(obj, "'" + type.getId() + "' мае няправільную геамэтрыю");
            }
            break;
        case POINT:
            if (!obj.isNode()) {
                CheckObjects.addError(obj, "'" + type.getId() + "' мае няправільную геамэтрыю");
            }
            break;
        default:
            throw new RuntimeException("Unknown geometry type");
        }
    }

    int indexOf(short[] list, short value) {
        for (int i = 0; i < list.length; i++) {
            if (value == list[i]) {
                return i;
            }
        }
        return -1;
    }
}
