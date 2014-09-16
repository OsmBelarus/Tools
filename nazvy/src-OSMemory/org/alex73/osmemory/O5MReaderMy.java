package org.alex73.osmemory;

import java.io.File;

import com.vividsolutions.jts.geom.Envelope;

public class O5MReaderMy extends BaseReader2 {

    public static void main(String[] aa) throws Exception {
        long b = System.currentTimeMillis();
        new O5MReaderMy(null).read(new File("tmp/belarus-updated.o5m"));
        long a = System.currentTimeMillis();
        System.out.println(a - b); // 47
    }

    public O5MReaderMy(Envelope cropBox) {
        super(cropBox);
    }

    public MemoryStorage2 read(File file) throws Exception {
        new O5MDriver(this).read(file);
        storage.finishLoading();
        return storage;
    }

    void applyTags(O5MDriver driver, BaseObject2 obj) {
        for (int i = 0; i < driver.getObjectTagsCount(); i++) {
            obj.tagKeys[i] = storage.getTagsPack().getTagCode(driver.getObjectTagKeyString(i));
            obj.tagValues[i] = driver.getObjectTagValueString(i);
        }
    }

    void createNode(O5MDriver driver, long id, int lat, int lon) {
        if (lat > 900000000 || lat < -900000000) {
            throw new RuntimeException("Wrong value for latitude: " + lat);
        }
        if (lon > 1800000000 || lon < -1800000000) {
            throw new RuntimeException("Wrong value for longitude: " + lon);
        }

        int intLon = (int) lon;
        int intLat = (int) lat;
        if (!isInsideCropBox(intLat, intLon)) {
            return;
        }

        NodeObject2 result = new NodeObject2(id, driver.getObjectTagsCount(), intLat, intLon);
        applyTags(driver, result);
        storage.nodes.add(result);
    }

    void createWay(O5MDriver driver, long id, long[] nodes) {
        WayObject2 result = new WayObject2(id, driver.getObjectTagsCount(), nodes);
        boolean inside = false;
        for (int i = 0; i < nodes.length; i++) {
            if (storage.getNodeById(nodes[i]) != null) {
                inside = true;
                break;
            }
        }
        if (inside) {
            applyTags(driver, result);
            storage.ways.add(result);
        }
    }

    void createRelation(O5MDriver driver, long id, long[] memberIds, byte[] memberTypes) {
        RelationObject2 result = new RelationObject2(id, driver.getObjectTagsCount(), memberIds, memberTypes);
        for (int i = 0; i < result.memberRoles.length; i++) {
            result.memberRoles[i] = storage.getRelationRolesPack().getTagCode(driver.getMemberRoleString(i));
        }
        applyTags(driver, result);
        storage.relations.add(result);
    }
}
