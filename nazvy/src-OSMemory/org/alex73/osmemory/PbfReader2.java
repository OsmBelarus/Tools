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

import com.vividsolutions.jts.geom.Envelope;

/**
 * Reader for pbf files using osmosis library.
 */
public class PbfReader2 extends BaseReader2 {
    public PbfReader2(Envelope cropBox) {
        super(cropBox);
    }

    public synchronized MemoryStorage2 read(File pbf) throws Exception {
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
                    NodeObject2 n = createNode((Node) e);
                    if (n != null) {
                        storage.nodes.add(n);
                    }
                    break;
                case Way:
                    storage.ways.add(createWay((Way) e));
                    break;
                case Relation:
                    storage.relations.add(createRelation((Relation) e));
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

    void applyTags(Collection<Tag> tags, BaseObject2 obj) {
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

    NodeObject2 createNode(Node in) {
        long lat = Math.round(in.getLatitude() / NodeObject2.DIVIDER);
        if (lat >= Integer.MAX_VALUE || lat <= Integer.MIN_VALUE) {
            throw new RuntimeException("Wrong value for latitude: " + lat);
        }
        long lon = Math.round(in.getLongitude() / NodeObject2.DIVIDER);
        if (lon >= Integer.MAX_VALUE || lon <= Integer.MIN_VALUE) {
            throw new RuntimeException("Wrong value for longitude: " + lon);
        }

        int intLon = (int) lon;
        int intLat = (int) lat;
        if (!isInsideCropBox(intLat, intLon)) {
            return null;
        }

        NodeObject2 result = new NodeObject2(in.getId(), in.getTags().size(), intLat, intLon);
        applyTags(in.getTags(), result);
        return result;
    }

    WayObject2 createWay(Way in) {
        List<WayNode> wayNodes = in.getWayNodes();
        long[] nodes = new long[wayNodes.size()];
        for (int i = 0; i < wayNodes.size(); i++) {
            nodes[i] = wayNodes.get(i).getNodeId();
        }
        WayObject2 result = new WayObject2(in.getId(), in.getTags().size(), nodes);
        applyTags(in.getTags(), result);
        return result;
    }

    RelationObject2 createRelation(Relation in) {
        List<RelationMember> inMembers = in.getMembers();
        RelationObject2 result = new RelationObject2(in.getId(), in.getTags().size(), inMembers.size());
        applyTags(in.getTags(), result);

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
