
package gen.alex73.osm.validators.objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for filter complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="filter">
 *   &lt;complexContent>
 *     &lt;extension base="{}TagList">
 *       &lt;attribute name="osmTypes" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="customMethod" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "filter")
public class Filter
    extends TagList
{

    @XmlAttribute(name = "osmTypes")
    protected String osmTypes;
    @XmlAttribute(name = "customMethod")
    protected String customMethod;

    /**
     * Gets the value of the osmTypes property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOsmTypes() {
        return osmTypes;
    }

    /**
     * Sets the value of the osmTypes property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOsmTypes(String value) {
        this.osmTypes = value;
    }

    /**
     * Gets the value of the customMethod property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCustomMethod() {
        return customMethod;
    }

    /**
     * Sets the value of the customMethod property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCustomMethod(String value) {
        this.customMethod = value;
    }

}
