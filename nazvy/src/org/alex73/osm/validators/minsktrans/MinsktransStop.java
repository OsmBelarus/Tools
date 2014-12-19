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

package org.alex73.osm.validators.minsktrans;

import javax.xml.bind.annotation.XmlAttribute;

public class MinsktransStop {
    @XmlAttribute(name = "ID")
    public int id;
    @XmlAttribute(name = "City")
    public int city;
    @XmlAttribute(name = "Area")
    public int area;
    @XmlAttribute(name = "Street")
    public String street;
    @XmlAttribute(name = "Name")
    public String name;
    @XmlAttribute(name = "Info")
    public String info;
    @XmlAttribute(name = "Lng")
    public Double lon;
    @XmlAttribute(name = "Lat")
    public Double lat;
    @XmlAttribute(name = "Stops")
    public String stops;
    @XmlAttribute(name = "StopNum")
    public String stopNum;
    @XmlAttribute(name = "Pikas2012.11.19")
    public String pikas;

    @XmlAttribute(name = "osm:NodeID")
    public Long osmNodeId;
    @XmlAttribute(name = "osm:name:ru")
    public String osmNameRu;

    @Override
    public String toString() {
        return "ID=" + id + "/" + name;
    }
}
