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
    public int lon;
    @XmlAttribute(name = "Lat")
    public int lat;
    @XmlAttribute(name = "Stops")
    public String stops;
    @XmlAttribute(name = "StopNum")
    public String stopNum;
    @XmlAttribute(name = "Pikas2012.11.19")
    public String pikas;
}
