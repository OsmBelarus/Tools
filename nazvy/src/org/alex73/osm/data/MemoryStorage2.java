package org.alex73.osm.data;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MemoryStorage2 {
    static final Pattern RE_OBJECT_CODE = Pattern.compile("([nwr])([0-9]+)");

    final protected List<NodeObject2> nodes = new ArrayList<>();
    final protected List<WayObject2> ways = new ArrayList<>();
    final protected List<RelationObject2> relations = new ArrayList<>();

    final private StringPack tagsPack = new StringPack();
    final private StringPack relationRolesPack = new StringPack();

    void finishLoading() throws Exception {
        // check ID order
        long prev = 0;
        for (int i = 0; i < nodes.size(); i++) {
            long id = nodes.get(i).id;
            if (id < prev) {
                throw new Exception("Nodes must be ordered by ID");
            }
        }
        prev = 0;
        for (int i = 0; i < ways.size(); i++) {
            long id = ways.get(i).id;
            if (id < prev) {
                throw new Exception("Ways must be ordered by ID");
            }
        }
        prev = 0;
        for (int i = 0; i < relations.size(); i++) {
            long id = relations.get(i).id;
            if (id < prev) {
                throw new Exception("Relations must be ordered by ID");
            }
        }
    }

    public StringPack getTagsPack() {
        return tagsPack;
    }

    public StringPack getRelationRolesPack() {
        return relationRolesPack;
    }

    private static <T extends BaseObject2> T getById(List<T> en, long id) {
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

    public NodeObject2 getNodeById(long id) {
        return getById(nodes, id);
    }

    public WayObject2 getWayById(long id) {
        return getById(ways, id);
    }

    public RelationObject2 getRelationById(long id) {
        return getById(relations, id);
    }

    /**
     * Get object by code like n123, w456, r789.
     */
    public BaseObject2 getObject(String code) throws Exception {
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
}
