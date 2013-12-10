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

public class PbfDriver {
    public static MemoryStorage process(File pbf) throws Exception {
        final MemoryStorage storage = new MemoryStorage();

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
                    storage.nodes.add(create((Node) e));
                    break;
                case Way:
                    storage.ways.add(create((Way) e));
                    break;
                case Relation:
                    storage.relations.add(create((Relation) e));
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

    static String[] createTags(Collection<Tag> tags) {
        String[] r = new String[tags.size() * 2];
        int i = 0;
        for (Tag t : tags) {
            r[i] = t.getKey().intern();
            i++;
            r[i] = t.getValue().intern();
            i++;
        }
        return r;
    }

    static NodeObject create(Node in) {
        return new NodeObject(in.getId(), createTags(in.getTags()), in.getLatitude(), in.getLongitude());
    }

    static WayObject create(Way in) {
        List<WayNode> wayNodes = in.getWayNodes();
        long[] nodes = new long[wayNodes.size()];
        for (int i = 0; i < wayNodes.size(); i++) {
            nodes[i] = wayNodes.get(i).getNodeId();
        }
        return new WayObject(in.getId(), createTags(in.getTags()), nodes);
    }

    static RelationObject create(Relation in) {
        List<RelationMember> inMembers = in.getMembers();
        RelationObject.Member[] members = new RelationObject.Member[inMembers.size()];
        for (int i = 0; i < inMembers.size(); i++) {
            RelationMember m = inMembers.get(i);
            BaseObject.TYPE type;
            switch (m.getMemberType()) {
            case Node:
                type = BaseObject.TYPE.NODE;
                break;
            case Way:
                type = BaseObject.TYPE.WAY;
                break;
            case Relation:
                type = BaseObject.TYPE.RELATION;
                break;
            default:
                throw new RuntimeException("Unknown member type: " + m.getMemberType());
            }
            members[i] = new RelationObject.Member(m.getMemberId(), m.getMemberRole().intern(), type);
        }

        return new RelationObject(in.getId(), createTags(in.getTags()), members);
    }
}
