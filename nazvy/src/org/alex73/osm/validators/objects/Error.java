package org.alex73.osm.validators.objects;

import org.alex73.osmemory.IOsmObject;

public class Error {
    public String objectCode;
    public String text;

    public Error(IOsmObject obj, String error) {
        objectCode = obj.getObjectCode();
        text = error;
    }
}
