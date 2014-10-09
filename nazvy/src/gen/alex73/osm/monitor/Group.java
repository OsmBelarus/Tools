package gen.alex73.osm.monitor;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for anonymous complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}attr" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *       &lt;attribute name="inNodes" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" />
 *       &lt;attribute name="inWays" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" />
 *       &lt;attribute name="inRelations" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "attr" })
@XmlRootElement(name = "group")
public class Group {

    @XmlElement(required = true)
    protected List<Attr> attr;
    @XmlAttribute(name = "inNodes")
    protected Boolean inNodes;
    @XmlAttribute(name = "inWays")
    protected Boolean inWays;
    @XmlAttribute(name = "inRelations")
    protected Boolean inRelations;

    /**
     * Gets the value of the attr property.
     * 
     * <p>
     * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification
     * you make to the returned list will be present inside the JAXB object. This is why there is not a
     * <CODE>set</CODE> method for the attr property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * 
     * <pre>
     * getAttr().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link Attr }
     * 
     * 
     */
    public List<Attr> getAttr() {
        if (attr == null) {
            attr = new ArrayList<Attr>();
        }
        return this.attr;
    }

    /**
     * Gets the value of the inNodes property.
     * 
     * @return possible object is {@link Boolean }
     * 
     */
    public boolean isInNodes() {
        if (inNodes == null) {
            return true;
        } else {
            return inNodes;
        }
    }

    /**
     * Sets the value of the inNodes property.
     * 
     * @param value
     *            allowed object is {@link Boolean }
     * 
     */
    public void setInNodes(Boolean value) {
        this.inNodes = value;
    }

    /**
     * Gets the value of the inWays property.
     * 
     * @return possible object is {@link Boolean }
     * 
     */
    public boolean isInWays() {
        if (inWays == null) {
            return true;
        } else {
            return inWays;
        }
    }

    /**
     * Sets the value of the inWays property.
     * 
     * @param value
     *            allowed object is {@link Boolean }
     * 
     */
    public void setInWays(Boolean value) {
        this.inWays = value;
    }

    /**
     * Gets the value of the inRelations property.
     * 
     * @return possible object is {@link Boolean }
     * 
     */
    public boolean isInRelations() {
        if (inRelations == null) {
            return true;
        } else {
            return inRelations;
        }
    }

    /**
     * Sets the value of the inRelations property.
     * 
     * @param value
     *            allowed object is {@link Boolean }
     * 
     */
    public void setInRelations(Boolean value) {
        this.inRelations = value;
    }

}
