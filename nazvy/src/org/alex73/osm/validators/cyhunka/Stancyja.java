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
