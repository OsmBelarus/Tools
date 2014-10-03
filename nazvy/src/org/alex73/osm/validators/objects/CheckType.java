package org.alex73.osm.validators.objects;

import gen.alex73.osm.validators.objects.Tag;
import gen.alex73.osm.validators.objects.Type;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.IOsmWay;
import org.alex73.osmemory.MemoryStorage;
import org.alex73.osmemory.geometry.Way;

public class CheckType {
    public static final String OK = "OK";

    private final MemoryStorage osm;
    private final Type type;

    private boolean allowNode, allowWay, allowRelation;
    private short[] filterTags, requiredTags, possibleTags;
    private Set<String> additions;

    public CheckType(MemoryStorage osm, Type type) {
        this.osm = osm;
        this.type = type;

        for (String ot : type.getOsmTypes().split(",")) {
            switch (ot) {
            case "node":
                allowNode = true;
                break;
            case "way":
                allowWay = true;
                break;
            case "relation":
                allowRelation = true;
                break;
            default:
                throw new RuntimeException("Невядомы osmTypes: " + ot);
            }
        }

        filterTags = tagCodes(type.getFilter());
        requiredTags = tagCodes(type.getRequired());
        possibleTags = tagCodes(type.getPossible());

        if (type.getAdditions() != null) {
            additions = new HashSet<>(Arrays.asList(type.getAdditions().split(",")));
        } else {
            additions = Collections.emptySet();
        }
    }

    short[] tagCodes(List<Tag> tags) {
        short[] result = new short[tags.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = osm.getTagsPack().getTagCode(tags.get(i).getName());
        }
        return result;
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

    public boolean matches(IOsmObject obj) {
        for (int i = 0; i < filterTags.length; i++) {
            if (!obj.hasTag(filterTags[i])) {
                return false;
            }
        }
        for (int i = 0; i < filterTags.length; i++) {
            String value = type.getFilter().get(i).getValue();
            if (value != null) {
                if (!value.equals(obj.getTag(filterTags[i]))) {
                    return false;
                }
            }
        }

        return true;
    }

    public void getErrors(IOsmObject obj) {
        if (type.getOsmTypes() != null) {
            switch (obj.getType()) {
            case IOsmObject.TYPE_NODE:
                if (!allowNode) {
                    CheckObjects.addError(obj, "'" + type.getId() + "' можа быць " + type.getOsmTypes()
                            + ", але не node");
                }
                break;
            case IOsmObject.TYPE_WAY:
                if (!allowWay) {
                    CheckObjects.addError(obj, "'" + type.getId() + "' можа быць " + type.getOsmTypes()
                            + ", але не way");
                } else {
                    checkGeo(obj);
                }
                break;
            case IOsmObject.TYPE_RELATION:
                if (!allowRelation) {
                    CheckObjects.addError(obj, "'" + type.getId() + "' можа быць " + type.getOsmTypes()
                            + ", але не relation");
                }
                break;
            }
        }

        // абавязковыя тэгі
        for (int i = 0; i < requiredTags.length; i++) {
            String expected = type.getRequired().get(i).getValue();
            if (!obj.hasTag(requiredTags[i])) {
                if (expected != null) {
                    CheckObjects.addError(obj, "'" + type.getId() + "' павінен утрымліваць тэг '"
                            + type.getRequired().get(i).getName() + "'='" + expected + "'");
                } else {
                    CheckObjects.addError(obj, "'" + type.getId() + "' павінен утрымліваць тэг '"
                            + type.getRequired().get(i).getName() + "'");
                }
            }
        }
    }

    public String matchTag(IOsmObject obj, short tagCode) {
        int p = indexOf(filterTags, tagCode);
        if (p >= 0) {
            return OK;
        }

        String value = obj.getTag(tagCode);

        p = indexOf(requiredTags, tagCode);
        if (p >= 0) {
            String expected = type.getRequired().get(p).getValue();
            if (expected != null && !expected.equals(value)) {
                return "'" + type.getId() + "' павінен утрымліваць тэг '"
                        + osm.getTagsPack().getTagName(tagCode) + "'='" + expected + "'";
            }
            return OK;
        }

        p = indexOf(possibleTags, tagCode);
        if (p >= 0) {
            String expected = type.getPossible().get(p).getValue();
            if (expected != null && !expected.equals(value)) {
                return "'" + type.getId() + "' павінен утрымліваць тэг '"
                        + osm.getTagsPack().getTagName(tagCode) + "'='" + expected + "'";
            }
            return OK;
        }

        return null;
    }

    void checkGeo(IOsmObject obj) {
        if (!obj.isWay() || type.getWayType() == null) {
            return;
        }
        Way way = new Way((IOsmWay) obj, osm);
        switch (type.getWayType()) {
        case CLOSED:
            if (!way.isClosed()) {
                CheckObjects.addError(obj, "'" + type.getId() + "' мае няправільную геамэтрыю");
            }
            break;
        case LINE:
            if (!way.isLine()) {
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
