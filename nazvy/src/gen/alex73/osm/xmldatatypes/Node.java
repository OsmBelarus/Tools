
package gen.alex73.osm.xmldatatypes;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;extension base="{}osmBasicType">
 *       &lt;sequence>
 *         &lt;element ref="{}tag" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="lat" use="required" type="{}Lat" />
 *       &lt;attribute name="lon" use="required" type="{}Lon" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "tag"
})
@XmlRootElement(name = "node")
public class Node
    extends OsmBasicType
{

    protected List<Tag> tag;
    @XmlAttribute(name = "lat", required = true)
    protected double lat;
    @XmlAttribute(name = "lon", required = true)
    protected double lon;

    /**
     * Gets the value of the tag property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the tag property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTag().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Tag }
     * 
     * 
     */
    public List<Tag> getTag() {
        if (tag == null) {
            tag = new ArrayList<Tag>();
        }
        return this.tag;
    }

    /**
     * Gets the value of the lat property.
     * 
     */
    public double getLat() {
        return lat;
    }

    /**
     * Sets the value of the lat property.
     * 
     */
    public void setLat(double value) {
        this.lat = value;
    }

    /**
     * Gets the value of the lon property.
     * 
     */
    public double getLon() {
        return lon;
    }

    /**
     * Sets the value of the lon property.
     * 
     */
    public void setLon(double value) {
        this.lon = value;
    }

}
