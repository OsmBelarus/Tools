/**************************************************************************
 OSMemory library for OSM data processing.

 Copyright (C) 2014 Aleś Bułojčyk <alex73mail@gmail.com>
               Home page: http://www.omegat.org/
               Support center: http://groups.yahoo.com/group/OmegaT/

 This is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This software is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/

package org.alex73.osmemory;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Envelope;

import ru.dkiselev.osm.o5mreader.O5MHandler;
import ru.dkiselev.osm.o5mreader.O5MReader;
import ru.dkiselev.osm.o5mreader.datasets.Node;
import ru.dkiselev.osm.o5mreader.datasets.Relation;
import ru.dkiselev.osm.o5mreader.datasets.Relation.RelationReference;
import ru.dkiselev.osm.o5mreader.datasets.Way;

/**
 * Reader for o5m files using o5m4j library. Should be changed to own o5m parsing since o5m4j is too slow and
 * contains some bugs in number calculation.
 */
public class o5mReader extends BaseReader2 {
    public static void main(String[] aa) throws Exception {
        long b = System.currentTimeMillis();
        new o5mReader(null).read(new File("tmp/belarus-updated.o5m"));
        long a = System.currentTimeMillis();
        System.out.println(a - b);// 65 seconds
    }
    
    public o5mReader(Envelope cropBox) {
        super(cropBox);
    }

    public MemoryStorage2 read(File file) throws Exception {
        final MemoryStorage2 storage = new MemoryStorage2();

        O5MReader rd = new O5MReader(new FileInputStream(file));
        rd.read(new O5MHandler() {
            @Override
            public void handleNode(Node ds) {
                NodeObject2 n = createNode(storage, ds);
                if (n != null) {
                    storage.nodes.add(n);
                }
            }

            @Override
            public void handleWay(Way ds) {
                storage.ways.add(createWay(storage, ds));
            }

            @Override
            public void handleRelation(Relation ds) {
                storage.relations.add(createRelation(storage, ds));
            }
        });

        storage.finishLoading();
        return storage;
    }

    void applyTags(MemoryStorage2 storage, Map<String, String> tags, BaseObject2 obj) {
        if (tags.isEmpty()) {
            return;
        }

        int i = 0;
        for (Map.Entry<String, String> t : tags.entrySet()) {
            obj.tagKeys[i] = storage.getTagsPack().getTagCode(t.getKey());
            obj.tagValues[i] = t.getValue();
            i++;
        }
    }

    NodeObject2 createNode(MemoryStorage2 storage, Node in) {
        long lat = Math.round(in.getLat() / NodeObject2.DIVIDER);
        if (lat >= Integer.MAX_VALUE || lat <= Integer.MIN_VALUE) {
          //  throw new RuntimeException("Wrong value for latitude: " + lat);
        }
        long lon = Math.round(in.getLon() / NodeObject2.DIVIDER);
        if (lon >= Integer.MAX_VALUE || lon <= Integer.MIN_VALUE) {
          //  throw new RuntimeException("Wrong value for longitude: " + lon);
        }

        int intLon = (int) lon;
        int intLat = (int) lat;
        if (!isInsideCropBox(intLat, intLon)) {
            return null;
        }

        NodeObject2 result = new NodeObject2(in.getId(), in.getTags().size(), intLat, intLon);
        applyTags(storage, in.getTags(), result);
        return result;
    }

    WayObject2 createWay(MemoryStorage2 storage, Way in) {
        List<Long> wayNodes = in.getNodes();
        long[] nodes = new long[in.getNodes().size()];
        for (int i = 0; i < wayNodes.size(); i++) {
            nodes[i] = wayNodes.get(i);
        }
        WayObject2 result = new WayObject2(in.getId(), in.getTags().size(), nodes);
        applyTags(storage, in.getTags(), result);
        return result;
    }

    RelationObject2 createRelation(MemoryStorage2 storage, Relation in) {
        List<RelationReference> inMembers = in.getReferences();
        RelationObject2 result = new RelationObject2(in.getId(), in.getTags().size(), inMembers.size());
        applyTags(storage, in.getTags(), result);

        // apply members
        for (int i = 0; i < inMembers.size(); i++) {
            RelationReference m = inMembers.get(i);
            switch (m.getRefObjectType()) {
            case NODE:
                result.memberTypes[i] = BaseObject2.TYPE_NODE;
                break;
            case WAY:
                result.memberTypes[i] = BaseObject2.TYPE_WAY;
                break;
            case RELATION:
                result.memberTypes[i] = BaseObject2.TYPE_RELATION;
                break;
            default:
                throw new RuntimeException("Unknown relation member type: " + m.getRefObjectType());
            }
            result.memberRoles[i] = storage.getRelationRolesPack().getTagCode(m.getRole());
            result.memberIDs[i] = m.getRefObjectId();
        }

        return result;
    }
}
