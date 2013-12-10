package org.alex73.osm.data;

import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alex73.osm.utils.Geo;

public class MemoryStorage {
    static final Pattern RE_OBJECT_CODE = Pattern.compile("([nwr])([0-9]+)");
    public static final long rel_Belarus = 79842;

    final public List<NodeObject> nodes = new ArrayList<>(9000000);
    final public List<WayObject> ways = new ArrayList<>(1400000);
    final public List<RelationObject> relations = new ArrayList<>(20000);
    public List<BaseObject> allObjects;
    public Area Belarus;

    void finishLoading() {
        if (nodes.size() < 7000000 || ways.size() < 900000 || relations.size() < 2000) {
            throw new RuntimeException("Not enough data for Belarus in .pbf file");
        }
        Collections.sort(nodes);
        Collections.sort(ways);
        Collections.sort(relations);
        allObjects = new ArrayList<>(nodes.size() + ways.size() + relations.size());
        allObjects.addAll(nodes);
        allObjects.addAll(ways);
        allObjects.addAll(relations);

        Belarus = Geo.rel2area(this, rel_Belarus);
    }

    private static <T extends BaseObject> T getById(List<T> en, long id) {
        int low = 0;
        int high = en.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midvalue = en.get(mid).id;

            if (midvalue < id)
                low = mid + 1;
            else if (midvalue > id)
                high = mid - 1;
            else
                return en.get(mid);
        }
        return null;
    }

    public NodeObject getNodeById(long id) {
        return getById(nodes, id);
    }

    public WayObject getWayById(long id) {
        return getById(ways, id);
    }

    public RelationObject getRelationById(long id) {
        return getById(relations, id);
    }

    /**
     * Get object by code like n123, w456, r789.
     */
    public BaseObject getObject(String code) throws Exception {
        Matcher m = RE_OBJECT_CODE.matcher(code.trim());
        if (!m.matches()) {
            throw new Exception("Няправільны фарматы code: " + code);
        }
        long idl = Long.parseLong(m.group(2));
        switch (m.group(1)) {
        case "n":
            return getNodeById(idl);
        case "w":
            return getWayById(idl);
        case "r":
            return getRelationById(idl);
        default:
            throw new Exception("Няправільны фарматы code: " + code);
        }
    }

    public boolean isInsideBelarus(BaseObject o) {
        switch (o.getType()) {
        case NODE:
            return isInsideBelarus((NodeObject) o);
        case WAY:
            return isInsideBelarus((WayObject) o);
        case RELATION:
            return isInsideBelarus((RelationObject) o);
        default:
            throw new RuntimeException("Unknown type of object " + o.getCode());
        }
    }

    public boolean isInsideBelarus(NodeObject o) {
        Point2D p = Geo.node2point(this, o.id);
        if (p == null) {
            return false;
        }
        return Belarus.contains(p);
    }

    public boolean isInsideBelarus(WayObject o) {
        Path2D path = Geo.way2path(this, o.id);
        if (path == null) {
            return false;
        }
        PathIterator it = path.getPathIterator(null);
        double[] c = new double[2];
        while (!it.isDone()) {
            it.currentSegment(c);
            if (Belarus.contains(c[0], c[1])) {
                return true;
            }
            it.next();
        }
        return false;
    }

    public boolean isInsideBelarus(RelationObject o) {
        Area p = Geo.rel2area(this, o.id);
        if (p == null) {
            return false;
        }
        p.intersect(Belarus);
        return !p.isEmpty();
    }
}
