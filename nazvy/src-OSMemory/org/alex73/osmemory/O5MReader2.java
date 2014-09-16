package org.alex73.osmemory;

import java.io.File;

import com.vividsolutions.jts.geom.Envelope;

public class O5MReader2 {
    public static void main(String[] aa) throws Exception {
        long b = System.currentTimeMillis();
        new O5MReader2(null).read(new File("tmp/belarus-updated.o5m"));
        long a = System.currentTimeMillis();
        System.out.println(a - b); // 50
    }

    protected MemoryStorage3 storage;
    protected int minx, maxx, miny, maxy;

    /**
     * Reader can skip nodes outside of cropbox. If cropbox is not defined, all nodes will be loaded.
     */
    protected O5MReader2(Envelope cropBox) {

        if (cropBox != null) {
            minx = (int) (cropBox.getMinX() / NodeObject2.DIVIDER) - 1;
            maxx = (int) (cropBox.getMaxX() / NodeObject2.DIVIDER) + 1;
            miny = (int) (cropBox.getMinY() / NodeObject2.DIVIDER) - 1;
            maxy = (int) (cropBox.getMaxY() / NodeObject2.DIVIDER) + 1;
        } else {
            minx = Integer.MIN_VALUE;
            maxx = Integer.MAX_VALUE;
            miny = Integer.MIN_VALUE;
            maxy = Integer.MAX_VALUE;
        }
    }

    /**
     * Check if node inside crop box.
     */
    protected boolean isInsideCropBox(int lat, int lon) {
        return lon >= minx && lon <= maxx && lat >= miny && lat <= maxy;
    }

    public MemoryStorage3 read(File file) throws Exception {
        O5MDriver2 driver = new O5MDriver2(this);
        storage = new MemoryStorage3(driver);
        driver.read(file);
        return storage;
    }

    short[] tagKeys;
    int[] tagValuePositions;

    void parseObjectTagsPositions(O5MDriver2 driver) {
        if (driver.objectTagsCount >= 0) {
            tagKeys = new short[driver.objectTagsCount];
            tagValuePositions = new int[driver.objectTagsCount];
            for (int i = 0; i < tagKeys.length; i++) {
              //  String key = driver.getString(driver.objectTagPositions[i]);
              //  tagKeys[i] = storage.getTagsPack().getTagCode(key);
                tagValuePositions[i] = driver.skipString(driver.objectTagPositions[i]);
            }
        } else {
            tagKeys = BaseObject3.EMPTY_TAG_KEYS;
            tagValuePositions = BaseObject3.EMPTY_TAG_VALUEPOSITIONS;
        }
    }

    void createNode(O5MDriver2 driver, long id, long lat, long lon) {
        if (lat >= Integer.MAX_VALUE || lat <= Integer.MIN_VALUE) {
            // throw new RuntimeException("Wrong value for latitude: " + lat);
        }
        if (lon >= Integer.MAX_VALUE || lon <= Integer.MIN_VALUE) {
            // throw new RuntimeException("Wrong value for longitude: " + lon);
        }

        int intLon = (int) lon;
        int intLat = (int) lat;
        if (!isInsideCropBox(intLat, intLon)) {
            return;
        }

        parseObjectTagsPositions(driver);
        NodeObject3 result = new NodeObject3(id, tagKeys, tagValuePositions, intLat, intLon);
        storage.nodes.add(result);
    }

    void createWay(O5MDriver2 driver, long id, int nodesListStart) {
        parseObjectTagsPositions(driver);
        WayObject3 result = new WayObject3(id, tagKeys, tagValuePositions, nodesListStart);
        storage.ways.add(result);
    }

    void createRelation(O5MDriver2 driver, long id, long[] memberIds, byte[] memberTypes,
            int[] memberRolePositions) {
        parseObjectTagsPositions(driver);
        RelationObject3 result = new RelationObject3(id, tagKeys, tagValuePositions, memberIds, memberTypes);
        for (int i = 0; i < result.memberRoles.length; i++) {
            result.memberRoles[i] = storage.getRelationRolesPack().getTagCode(
                    driver.getString(memberRolePositions[i]));
        }
        storage.relations.add(result);
    }
}
