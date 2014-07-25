package org.alex73.osm.validators.vulicy2;

import org.alex73.osm.validators.vulicy2.OsmNamed.TYPE;

public class OsmHouseStreet {
    public TYPE htype;
    public long hid;
    public TYPE type;
    public Long rid;
    public String name;
    public String name_be;
    public double xmin, xmax, ymin, ymax;
    public Object tags;

    public String getHouseCode() {
        return OsmNamed.getCode(htype, hid);
    }
}
