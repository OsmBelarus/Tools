
package gen.alex73.osm.validators.rehijony;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Nazva complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Nazva">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="name_be_correct" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="name_be" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="name_ru" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="int_name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="iso3166-1" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="iso3166-2" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Nazva")
@XmlSeeAlso({
    Vojtaustva.class,
    AreaObject.class
})
public class Nazva {

    @XmlAttribute(name = "name_be_correct")
    protected String nameBeCorrect;
    @XmlAttribute(name = "name_be", required = true)
    protected String nameBe;
    @XmlAttribute(name = "name_ru")
    protected String nameRu;
    @XmlAttribute(name = "int_name")
    protected String intName;
    @XmlAttribute(name = "iso3166-1")
    protected String iso31661;
    @XmlAttribute(name = "iso3166-2")
    protected String iso31662;

    /**
     * Gets the value of the nameBeCorrect property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNameBeCorrect() {
        return nameBeCorrect;
    }

    /**
     * Sets the value of the nameBeCorrect property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNameBeCorrect(String value) {
        this.nameBeCorrect = value;
    }

    /**
     * Gets the value of the nameBe property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNameBe() {
        return nameBe;
    }

    /**
     * Sets the value of the nameBe property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNameBe(String value) {
        this.nameBe = value;
    }

    /**
     * Gets the value of the nameRu property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNameRu() {
        return nameRu;
    }

    /**
     * Sets the value of the nameRu property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNameRu(String value) {
        this.nameRu = value;
    }

    /**
     * Gets the value of the intName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIntName() {
        return intName;
    }

    /**
     * Sets the value of the intName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIntName(String value) {
        this.intName = value;
    }

    /**
     * Gets the value of the iso31661 property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIso31661() {
        return iso31661;
    }

    /**
     * Sets the value of the iso31661 property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIso31661(String value) {
        this.iso31661 = value;
    }

    /**
     * Gets the value of the iso31662 property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIso31662() {
        return iso31662;
    }

    /**
     * Sets the value of the iso31662 property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIso31662(String value) {
        this.iso31662 = value;
    }

}
