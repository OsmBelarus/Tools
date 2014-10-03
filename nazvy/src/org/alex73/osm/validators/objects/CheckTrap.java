package org.alex73.osm.validators.objects;

import gen.alex73.osm.validators.objects.Tag;
import gen.alex73.osm.validators.objects.Trap;

import java.util.List;

import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.MemoryStorage;

public class CheckTrap {
    private final MemoryStorage osm;
    private final Trap trap;

    private short[] filterTags;

    public CheckTrap(MemoryStorage osm, Trap trap) {
        this.osm = osm;
        this.trap = trap;

        filterTags = tagCodes(trap.getFilter());
    }

    short[] tagCodes(List<Tag> tags) {
        short[] result = new short[tags.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = osm.getTagsPack().getTagCode(tags.get(i).getName());
        }
        return result;
    }

    public Trap getTrap() {
        return trap;
    }


    public boolean matches(IOsmObject obj) {
        for (int i = 0; i < filterTags.length; i++) {
            if (!obj.hasTag(filterTags[i])) {
                return false;
            }
        }
        for (int i = 0; i < filterTags.length; i++) {
            String value = trap.getFilter().get(i).getValue();
            if (value != null) {
                if (!value.equals(obj.getTag(filterTags[i]))) {
                    return false;
                }
            }
        }

        return true;
    }
}
