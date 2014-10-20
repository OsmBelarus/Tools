
package gen.alex73.osm.validators.objects;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the gen.alex73.osm.validators.objects package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: gen.alex73.osm.validators.objects
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Trap }
     * 
     */
    public Trap createTrap() {
        return new Trap();
    }

    /**
     * Create an instance of {@link ObjectTypes }
     * 
     */
    public ObjectTypes createObjectTypes() {
        return new ObjectTypes();
    }

    /**
     * Create an instance of {@link Type }
     * 
     */
    public Type createType() {
        return new Type();
    }

    /**
     * Create an instance of {@link BaseFilter }
     * 
     */
    public BaseFilter createBaseFilter() {
        return new BaseFilter();
    }

    /**
     * Create an instance of {@link Filter }
     * 
     */
    public Filter createFilter() {
        return new Filter();
    }

    /**
     * Create an instance of {@link gen.alex73.osm.validators.objects.Required }
     * 
     */
    public gen.alex73.osm.validators.objects.Required createRequired() {
        return new gen.alex73.osm.validators.objects.Required();
    }

    /**
     * Create an instance of {@link Allow }
     * 
     */
    public Allow createAllow() {
        return new Allow();
    }

    /**
     * Create an instance of {@link Trap.Required }
     * 
     */
    public Trap.Required createTrapRequired() {
        return new Trap.Required();
    }

    /**
     * Create an instance of {@link TagList }
     * 
     */
    public TagList createTagList() {
        return new TagList();
    }

    /**
     * Create an instance of {@link Tag }
     * 
     */
    public Tag createTag() {
        return new Tag();
    }

}
