package org.alex73.osm.data;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.pbf2.v0_6.PbfReader;

public class PdfReader2 {
    static final String[] EMPTY_TAGS = new String[0];

    public static void main(String[] aaaa) throws Exception {
        long b = System.currentTimeMillis();
        read(new File("tmp/belarus-latest.osm.pbf"));
        long a = System.currentTimeMillis();
        System.out.println(a - b);
    }

    public static MemoryStorage2 read(File pbf) throws Exception {
        final MemoryStorage2 storage = new MemoryStorage2();

        PbfReader rd = new PbfReader(pbf, 1);
        rd.setSink(new Sink() {
            @Override
            public void initialize(Map<String, Object> arg0) {
            }

            @Override
            public void process(EntityContainer arg0) {
                Entity e = arg0.getEntity();

                switch (e.getType()) {
                case Node:
                    storage.nodes.add(createNode(storage, (Node) e));
                    break;
                case Way:
                    storage.ways.add(createWay(storage, (Way) e));
                    break;
                case Relation:
                    storage.relations.add(createRelation(storage, (Relation) e));
                    break;
                case Bound:
                    break;
                }
            }

            @Override
            public void complete() {
            }

            @Override
            public void release() {
            }
        });
        rd.run();
        storage.finishLoading();
        return storage;
    }

    static void applyTags(MemoryStorage2 storage, Collection<Tag> tags, BaseObject2 obj) {
        if (tags.isEmpty()) {
            return;
        }

        int i = 0;
        for (Tag t : tags) {
            obj.tagKeys[i] = storage.getTagsPack().getTagCode(t.getKey());
            obj.tagValues[i] = t.getValue();
            i++;
        }
    }

    static NodeObject2 createNode(MemoryStorage2 storage, Node in) {
        long lat = Math.round(in.getLatitude() * NodeObject2.DIVIDER);
        if (lat >= Integer.MAX_VALUE || lat <= Integer.MIN_VALUE) {
            throw new RuntimeException("Wrong value for latitude: " + lat);
        }
        long lon = Math.round(in.getLongitude() * NodeObject2.DIVIDER);
        if (lon >= Integer.MAX_VALUE || lon <= Integer.MIN_VALUE) {
            throw new RuntimeException("Wrong value for longitude: " + lon);
        }

        NodeObject2 result = new NodeObject2(storage, in.getId(), in.getTags().size(), (int) lat, (int) lon);
        applyTags(storage, in.getTags(), result);
        return result;
    }

    static WayObject2 createWay(MemoryStorage2 storage, Way in) {
        List<WayNode> wayNodes = in.getWayNodes();
        long[] nodes = new long[wayNodes.size()];
        for (int i = 0; i < wayNodes.size(); i++) {
            nodes[i] = wayNodes.get(i).getNodeId();
        }
        WayObject2 result = new WayObject2(storage, in.getId(), in.getTags().size(), nodes);
        applyTags(storage, in.getTags(), result);
        return result;
    }

    static RelationObject2 createRelation(MemoryStorage2 storage, Relation in) {
        List<RelationMember> inMembers = in.getMembers();
        RelationObject2 result = new RelationObject2(storage, in.getId(), in.getTags().size(),
                inMembers.size());
        applyTags(storage, in.getTags(), result);

        // apply members
        for (int i = 0; i < inMembers.size(); i++) {
            RelationMember m = inMembers.get(i);
            switch (m.getMemberType()) {
            case Node:
                result.memberTypes[i] = BaseObject2.TYPE_NODE;
                break;
            case Way:
                result.memberTypes[i] = BaseObject2.TYPE_WAY;
                break;
            case Relation:
                result.memberTypes[i] = BaseObject2.TYPE_RELATION;
                break;
            default:
                throw new RuntimeException("Unknown relation member type: " + m.getMemberType());
            }
            result.memberRoles[i] = storage.getRelationRolesPack().getTagCode(m.getMemberRole());
            result.memberIDs[i] = m.getMemberId();
        }

        return result;
    }
}
