/**************************************************************************
 Some tools for OSM.

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

package org.alex73.osm.validators.cyhunka;

import javax.xml.bind.annotation.XmlAttribute;

public class Stancyja {
    @XmlAttribute(name = "Вобласць")
    public String voblasc;
    @XmlAttribute(name = "Раён")
    public String rajon;
    @XmlAttribute(name = "ЕСР код")
    public Integer esr;
    @XmlAttribute(name = "OSM node")
    public Long nodeID;
    @XmlAttribute(name = "Тып")
    public String typ;
    @XmlAttribute(name = "Па-расейску старое")
    public String nameRuOld;
    @XmlAttribute(name = "Па-расейску правільнае")
    public String nameRu;
    @XmlAttribute(name = "osm:name:ru force")
    public String nameRuForce;
    @XmlAttribute(name = "Па-беларуску правільнае")
    public String nameBe;
    @XmlAttribute(name = "lat")
    public Double lat;
    @XmlAttribute(name = "lon")
    public Double lon;
    @XmlAttribute(name = "comment")
    public String comment;

    @Override
    public String toString() {
        return voblasc + "/" + rajon + "/" + nameBe;
    }
}
