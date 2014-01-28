package org.alex73.osm.validators.ahulnaje;

public class Error {
    public String error;
    public String type;
    public long id;

    public String getCode() {
        return type + id;
    }
}
