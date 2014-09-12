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

/**
 * Relation object representation.
 */
public class RelationObject2 extends BaseObject2 {
    final long memberIDs[];
    final byte memberTypes[];
    final short memberRoles[];

    public RelationObject2(long id, int tagsCount, int memberCount) {
        super(id, tagsCount);
        memberIDs = new long[memberCount];
        memberTypes = new byte[memberCount];
        memberRoles = new short[memberCount];
    }

    @Override
    public byte getType() {
        return TYPE_RELATION;
    }

    @Override
    public String getCode() {
        return "r" + id;
    }

    public static String getCode(long id) {
        return "r" + id;
    }

    public int getMembersCount() {
        return memberIDs.length;
    }

    public BaseObject2 getMemberObject(MemoryStorage2 storage, int memberIndex) {
        switch (memberTypes[memberIndex]) {
        case TYPE_NODE:
            return storage.getNodeById(memberIDs[memberIndex]);
        case TYPE_WAY:
            return storage.getWayById(memberIDs[memberIndex]);
        case TYPE_RELATION:
            return storage.getRelationById(memberIDs[memberIndex]);
        default:
            throw new RuntimeException("Unknown member type");
        }
    }

    public String getMemberRole(MemoryStorage2 storage, int memberIndex) {
        return storage.getRelationRolesPack().getTagName(memberRoles[memberIndex]);
    }

    public byte getMemberType(int memberIndex) {
        return memberTypes[memberIndex];
    }

    public long getMemberID(int memberIndex) {
        return memberIDs[memberIndex];
    }
}
