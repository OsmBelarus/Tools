package org.alex73.osm.daviednik;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.alex73.osm.validators.vulicy2.OsmPlace;
import org.alex73.osmemory.MemoryStorage2;
import org.alex73.osmemory.NodeObject2;

public class CalcCorrectTags2 {
    static public OsmPlace calc(Miesta m,MemoryStorage2 storage, NodeObject2 node) throws Exception {
        String typ;
        switch (m.typ) {
        case "г.":
            if (node == null) {
                throw new Exception("Няма горада: " + m);
            }
            String population =storage.getTag(node, "population");
            if (population != null && Integer.parseInt(population) >= 50000) {
                typ = "city";
            } else {
                typ = "town";
            }
            break;
        case "г.п.":
        case "г. п.":
            typ = "village";
            break;
        case "в.":
        case "п.":
        case "р.п.":
        case "р. п.":
        case "аг.":
            typ = "hamlet";
            break;
        case "х.":
            typ = "isolated_dwelling";
            break;
        case "ст.":
        case "с.":
        case "раз’езд":
            typ = null;
            break;
        default:
            throw new RuntimeException("Невядомы тып " + m.typ + " для " + m.osmID);
        }

        String name_be = m.nazvaNoStress;
        String name_be_tarask = unstress(m.osmNameBeTarask);
        String variants_be = variantsToString(splitVariants(m.varyjantyBel));
        String int_name = m.translit;

        String name_ru = m.osmForceNameRu != null ? m.osmForceNameRu : m.ras;
        List<String> vru = new ArrayList<>();
        sadd(vru, m.rasUsedAsOld);
        sadd(vru, m.ras);
        sadd(vru, m.osmAltNameRu);
        sdel(vru, name_ru);
        String variants_ru = variantsToString(vru);

        OsmPlace result = new OsmPlace();
        result.name= name_ru;
        result.name_ru= name_ru;
        result.name_be= name_be;
        result.int_name=int_name;
        result.name_be_tarask= name_be_tarask;
        if (typ != null) {
            result.place= typ;
        }
        result.alt_name_ru= variants_ru;
        result.alt_name_be= variants_be;
        result.alt_name= null;

        return result;
    }

    static String variantsToString(List<String> variants) {
        if (variants.isEmpty()) {
            return null;
        }
        StringBuilder out = new StringBuilder(200);
        Set<String> set = new TreeSet<>(COMPARATOR);
        set.addAll(variants);
        for (String v : set) {
            out.append(';').append(v);
        }
        return out.substring(1).toString();
    }

    static String unstress(String s) {
        if (s == null) {
            return null;
        }
        return s.replace("\u0301", "");
    }

    static List<String> splitVariants(String s) {
        if (s == null) {
            return Collections.emptyList();
        }
        s = unstress(s).replaceAll("\\(.+\\)", "");
        List<String> out = new ArrayList<>();
        for (String w : s.split("[,;]")) {
            w = w.trim();
            if (w.startsWith("пад ") || w.startsWith("за ") || w.startsWith("з ")) {
                continue;
            }
            switch (w) {
            case "м.":
            case "ж.":
            case "н.":
            case "мн.":
                break;
            default:
                out.add(w);
            }
        }
        return out;
    }

    static void sadd(List<String> set, String add) {
        if (add != null && !set.contains(add)) {
            set.add(add);
        }
    }

    static void sdel(List<String> set, String del) {
        if (del != null) {
            set.remove(del);
        }
    }

    static Comparator<String> COMPARATOR = new Comparator<String>() {
        Locale BE = new Locale("be");
        Collator BEL = Collator.getInstance(BE);

        @Override
        public int compare(String o1, String o2) {
            return BEL.compare(o1, o2);
        }
    };
}
