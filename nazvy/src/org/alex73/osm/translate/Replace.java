package org.alex73.osm.translate;

import javax.xml.bind.annotation.XmlAttribute;

public class Replace {
    @XmlAttribute(name = "from")
    public String from;
    @XmlAttribute(name = "to")
    public String to;
}
