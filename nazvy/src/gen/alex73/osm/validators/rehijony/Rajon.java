
package gen.alex73.osm.validators.rehijony;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
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
 *     &lt;extension base="{}AreaObject">
 *       &lt;sequence>
 *         &lt;element ref="{}Vojtaustva" maxOccurs="unbounded"/>
 *         &lt;element ref="{}Horad" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "vojtaustva",
    "horad"
})
@XmlRootElement(name = "Rajon")
public class Rajon
    extends AreaObject
{

    @XmlElement(name = "Vojtaustva", required = true)
    protected List<Vojtaustva> vojtaustva;
    @XmlElement(name = "Horad", required = true)
    protected List<Horad> horad;

    /**
     * Gets the value of the vojtaustva property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the vojtaustva property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getVojtaustva().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Vojtaustva }
     * 
     * 
     */
    public List<Vojtaustva> getVojtaustva() {
        if (vojtaustva == null) {
            vojtaustva = new ArrayList<Vojtaustva>();
        }
        return this.vojtaustva;
    }

    /**
     * Gets the value of the horad property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the horad property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getHorad().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Horad }
     * 
     * 
     */
    public List<Horad> getHorad() {
        if (horad == null) {
            horad = new ArrayList<Horad>();
        }
        return this.horad;
    }

}
