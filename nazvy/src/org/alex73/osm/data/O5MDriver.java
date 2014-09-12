package org.alex73.osm.data;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
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

import ru.dkiselev.osm.o5mreader.O5MHandler;
import ru.dkiselev.osm.o5mreader.O5MReader;

public class O5MDriver {
    
    static final String[] EMPTY_TAGS = new String[0];
public static void main(String[] aaa) throws Exception {
    long b=System.currentTimeMillis();
    process(new File("tmp/out.o5m"));
    long a=System.currentTimeMillis();
    System.out.println(a-b);
}
    
    public static MemoryStorage process(File pbf) throws Exception {
        return process(pbf, null);
    }

    public static MemoryStorage process(File file, final Filter filter) throws Exception {
        final MemoryStorage storage = new MemoryStorage();

        O5MReader rd = new O5MReader(new FileInputStream(file));
        rd.read(new O5MHandler() {
            @Override
            public void handleNode(ru.dkiselev.osm.o5mreader.datasets.Node ds) {
              //  storage.nodes.add(create(n));
            }

            @Override
            public void handleWay(ru.dkiselev.osm.o5mreader.datasets.Way ds) {
               // storage.ways.add(create((Way) e));
            }

            @Override
            public void handleRelation(ru.dkiselev.osm.o5mreader.datasets.Relation ds) {
               // storage.relations.add(create((Relation) e));
            }
        });
        
      

        if (filter != null) {
            List<WayObject> newWays = new ArrayList<WayObject>(storage.ways.size());
            for (WayObject w : storage.ways) {
                if (filter.acceptWay(storage, w)) {
                    newWays.add(w);
                }
            }
            storage.ways.clear();
            storage.ways.addAll(newWays);

            List<RelationObject> newRelations = new ArrayList<RelationObject>(storage.relations.size());
            for (RelationObject r : storage.relations) {
                if (filter.acceptRelation(storage, r)) {
                    newRelations.add(r);
                }
            }
            storage.relations.clear();
            storage.relations.addAll(newRelations);
        }

        return storage;
    }

    static String[] createTags(Collection<Tag> tags) {
        if (tags.isEmpty()) {
            return EMPTY_TAGS;
        }
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

    public interface Filter {
        boolean acceptNode(Node n);

        boolean acceptWay(MemoryStorage storage, WayObject w);

        boolean acceptRelation(MemoryStorage storage, RelationObject r);
    }
}
