
package gen.alex73.osm.validators.rehijony;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java class for AreaObject complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="AreaObject">
 *   &lt;complexContent>
 *     &lt;extension base="{}Nazva">
 *       &lt;attribute name="osmID" type="{}OsmIDarea" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AreaObject")
@XmlSeeAlso({
    Kraina.class,
    Rajon.class,
    Voblasc.class,
    Horad.class
})
public class AreaObject
    extends Nazva
{

    @XmlAttribute(name = "osmID")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String osmID;

    /**
     * Gets the value of the osmID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOsmID() {
        return osmID;
    }

    /**
     * Sets the value of the osmID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOsmID(String value) {
        this.osmID = value;
    }

}
