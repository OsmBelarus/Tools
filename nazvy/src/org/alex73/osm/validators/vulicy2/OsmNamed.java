package org.alex73.osm.validators.vulicy2;

public class OsmNamed {
    enum TYPE {
        NODE, WAY, RELATION
    };

    public long id;
    public TYPE type;
    public String name;
    public String name_ru;
    public String name_be;
    public String name_be_tarask;
    public String int_name;
    public String alt_name;
    public String alt_name_ru;
    public String alt_name_be;

    public String getCode() {
        return getCode(type, id);
    }

    static String getCode(TYPE type, long id) {
        switch (type) {
        case NODE:
            return "n" + id;
        case WAY:
            return "w" + id;
        case RELATION:
            return "r" + id;
        }
        throw new RuntimeException();
    }
}
