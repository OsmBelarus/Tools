
package gen.alex73.osm.validators.objects;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for GranularityType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="GranularityType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="KRAINA"/>
 *     &lt;enumeration value="VOBLASC"/>
 *     &lt;enumeration value="RAJON"/>
 *     &lt;enumeration value="MIESTA"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "GranularityType")
@XmlEnum
public enum GranularityType {

    KRAINA,
    VOBLASC,
    RAJON,
    MIESTA;

    public String value() {
        return name();
    }

    public static GranularityType fromValue(String v) {
        return valueOf(v);
    }

}
