
package gen.alex73.osm.validators.objects;

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
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="filter" type="{}Tag" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="required" type="{}Tag" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="possible" type="{}Tag" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="osmTypes" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="wayType" type="{}GeometryType" />
 *       &lt;attribute name="main" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *       &lt;attribute name="additions" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "filter",
    "required",
    "possible"
})
@XmlRootElement(name = "type")
public class Type {

    protected List<Tag> filter;
    protected List<Tag> required;
    protected List<Tag> possible;
    @XmlAttribute(name = "id", required = true)
    protected String id;
    @XmlAttribute(name = "osmTypes")
    protected String osmTypes;
    @XmlAttribute(name = "wayType")
    protected GeometryType wayType;
    @XmlAttribute(name = "main")
    protected Boolean main;
    @XmlAttribute(name = "additions")
    protected String additions;

    /**
     * Gets the value of the filter property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the filter property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFilter().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Tag }
     * 
     * 
     */
    public List<Tag> getFilter() {
        if (filter == null) {
            filter = new ArrayList<Tag>();
        }
        return this.filter;
    }

    /**
     * Gets the value of the required property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the required property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRequired().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Tag }
     * 
     * 
     */
    public List<Tag> getRequired() {
        if (required == null) {
            required = new ArrayList<Tag>();
        }
        return this.required;
    }

    /**
     * Gets the value of the possible property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the possible property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPossible().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Tag }
     * 
     * 
     */
    public List<Tag> getPossible() {
        if (possible == null) {
            possible = new ArrayList<Tag>();
        }
        return this.possible;
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
     * Gets the value of the wayType property.
     * 
     * @return
     *     possible object is
     *     {@link GeometryType }
     *     
     */
    public GeometryType getWayType() {
        return wayType;
    }

    /**
     * Sets the value of the wayType property.
     * 
     * @param value
     *     allowed object is
     *     {@link GeometryType }
     *     
     */
    public void setWayType(GeometryType value) {
        this.wayType = value;
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
