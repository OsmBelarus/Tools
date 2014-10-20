package org.alex73.osm.validators.objects;

import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.MemoryStorage;

public class CustomCheckRelations implements ICustomClass {
    MemoryStorage osm;

    @Override
    public void init() throws Exception {
        osm = CheckObjects.osm;
    }

    @Override
    public void finish() throws Exception {
    }

    public boolean checkNoType(IOsmObject obj) throws Exception {
        return !obj.hasTag("type", osm);
    }
}
