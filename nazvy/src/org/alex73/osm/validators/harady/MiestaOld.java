package org.alex73.osm.validators.harady;

import java.lang.reflect.Field;

import javax.xml.bind.annotation.XmlAttribute;

public class MiestaOld {
    @XmlAttribute(name = "Вобласць")
    public String voblasc;
    @XmlAttribute(name = "Раён")
    public String rajon;
    @XmlAttribute(name = "Сельсавет")
    public String sielsaviet;
    @XmlAttribute(name = "Тып")
    public String typ;
    @XmlAttribute(name = "Назва")
    public String nazva;
    @XmlAttribute(name = "Назва без націскаў")
    public String nazvaNoStress;
    @XmlAttribute(name = "Род")
    public String rod;
    @XmlAttribute(name = "Парадыгма")
    public String paradyhma;
    @XmlAttribute(name = "Транслітарацыя")
    public String translit;
    @XmlAttribute(name = "Назва па-расейску")
    public String ras;
    @XmlAttribute(name = "Варыянты назвы па-беларуску")
    public String varyjantyBel;
    @XmlAttribute(name = "Назвы што ўжываюцца да цяперашняга часу(рас.)")
    public String rasUsedAsOld;
    @XmlAttribute(name = "Заўвагі да даведніка")
    public String comments;
    @XmlAttribute(name = "osm:Тып")
    public String osmForceTyp;
    @XmlAttribute(name = "osm:Назва па-расейску")
    public String osmForceNameRu;
    @XmlAttribute(name = "osm:Хто вызначыў назву па-расейску і чаму")
    public String osmForceNameRuWhy;
    @XmlAttribute(name = "osm:Дадатковыя назвы па-расейску")
    public String osmAltNameRu;
    @XmlAttribute(name = "osm:name:be-tarask")
    public String osmNameBeTarask;
    @XmlAttribute(name = "osm:Node ID")
    public Long osmID;
    @XmlAttribute(name = "Супадзеньне ў аўтаматычным пошуку")
    public String osmComment;
    @XmlAttribute(name = "osm:Стары тып")
    public String osmType;
    @XmlAttribute(name = "osm:Дзейсная назва")
    public String osmName;
    @XmlAttribute(name = "osm:Way/Rel IDs")
    public String osmIDother;

    public String getHash() {
        return voblasc + '|' + rajon + '|' + sielsaviet + '|' + typ + '|' + nazva;
    }

    @Override
    public String toString() {
        return voblasc + '|' + rajon + '|' + sielsaviet + '|' + typ + '|' + nazva;
    }

    public Miesta cloneObject() throws Exception {
        Miesta r = new Miesta();
        for (Field f : MiestaOld.class.getFields()) {
            Field fn=Miesta.class.getField(f.getName());
            fn.set(r, f.get(this));
        }
        return r;
    }
}
