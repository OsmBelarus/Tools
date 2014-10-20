
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
 *         &lt;element name="required" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="geometryType" type="{}GeometryType" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *       &lt;attribute name="message" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "required"
})
@XmlRootElement(name = "trap")
public class Trap
    extends BaseFilter
{

    protected Trap.Required required;
    @XmlAttribute(name = "message", required = true)
    protected String message;

    /**
     * Gets the value of the required property.
     * 
     * @return
     *     possible object is
     *     {@link Trap.Required }
     *     
     */
    public Trap.Required getRequired() {
        return required;
    }

    /**
     * Sets the value of the required property.
     * 
     * @param value
     *     allowed object is
     *     {@link Trap.Required }
     *     
     */
    public void setRequired(Trap.Required value) {
        this.required = value;
    }

    /**
     * Gets the value of the message property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the value of the message property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMessage(String value) {
        this.message = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;attribute name="geometryType" type="{}GeometryType" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class Required {

        @XmlAttribute(name = "geometryType")
        protected GeometryType geometryType;

        /**
         * Gets the value of the geometryType property.
         * 
         * @return
         *     possible object is
         *     {@link GeometryType }
         *     
         */
        public GeometryType getGeometryType() {
            return geometryType;
        }

        /**
         * Sets the value of the geometryType property.
         * 
         * @param value
         *     allowed object is
         *     {@link GeometryType }
         *     
         */
        public void setGeometryType(GeometryType value) {
            this.geometryType = value;
        }

    }

}
