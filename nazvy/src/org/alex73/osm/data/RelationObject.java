/**************************************************************************
 Some tools for OSM.

 Copyright (C) 2013 Aleś Bułojčyk <alex73mail@gmail.com>
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

package org.alex73.osm.data;

public class RelationObject extends BaseObject {
    final public Member[] members;

    public RelationObject(long id, String[] tags, Member[] members) {
        super(id, tags);
        this.members = members;
    }

    @Override
    public TYPE getType() {
        return TYPE.RELATION;
    }

    @Override
    public String getCode() {
        return "r" + id;
    }

    public static class Member {
        final public long id;
        final public String role;
        final public TYPE type;

        public Member(long id, String role, TYPE type) {
            this.id = id;
            this.role = role;
            this.type = type;
        }
    }
}
