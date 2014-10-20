
package gen.alex73.osm.validators.objects;

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
 *     &lt;extension base="{}BaseFilter">
 *       &lt;sequence>
 *         &lt;element name="required" type="{}required" minOccurs="0"/>
 *         &lt;element name="allow" type="{}allow" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="main" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *       &lt;attribute name="additions" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "required",
    "allow"
})
@XmlRootElement(name = "type")
public class Type
    extends BaseFilter
{

    protected Required required;
    protected Allow allow;
    @XmlAttribute(name = "id", required = true)
    protected String id;
    @XmlAttribute(name = "main")
    protected Boolean main;
    @XmlAttribute(name = "additions")
    protected String additions;

    /**
     * Gets the value of the required property.
     * 
     * @return
     *     possible object is
     *     {@link Required }
     *     
     */
    public Required getRequired() {
        return required;
    }

    /**
     * Sets the value of the required property.
     * 
     * @param value
     *     allowed object is
     *     {@link Required }
     *     
     */
    public void setRequired(Required value) {
        this.required = value;
    }

    /**
     * Gets the value of the allow property.
     * 
     * @return
     *     possible object is
     *     {@link Allow }
     *     
     */
    public Allow getAllow() {
        return allow;
    }

    /**
     * Sets the value of the allow property.
     * 
     * @param value
     *     allowed object is
     *     {@link Allow }
     *     
     */
    public void setAllow(Allow value) {
        this.allow = value;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * Gets the value of the main property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isMain() {
        if (main == null) {
            return false;
        } else {
            return main;
        }
    }

    /**
     * Sets the value of the main property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setMain(Boolean value) {
        this.main = value;
    }

    /**
     * Gets the value of the additions property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAdditions() {
        return additions;
    }

    /**
     * Sets the value of the additions property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAdditions(String value) {
        this.additions = value;
    }

}
