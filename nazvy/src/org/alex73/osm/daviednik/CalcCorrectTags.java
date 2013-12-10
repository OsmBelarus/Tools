package org.alex73.osm.daviednik;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.alex73.osm.data.MemoryStorage;
import org.alex73.osm.data.NodeObject;

/**
 * Вызначае правільныя тэгі для назваў населеных пунктаў.
 */
public class CalcCorrectTags {
    static public Map<String, String> calc(Miesta m, MemoryStorage osm) throws Exception {
        String typ;
        switch (m.typ) {
        case "г.":
            if (m.osmID == null) {
                throw new Exception("Node ID не вызначана для горада: " + m);
            }
            NodeObject n = osm.getNodeById(m.osmID);
            if (n == null) {
                throw new Exception("Няма горада: " + m);
            }
            String population = n.getTag("population");
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
            throw new RuntimeException(m.typ + " for " + m.osmID);
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

        Map<String, String> result = new TreeMap<>();
        result.put("name", name_ru);
        result.put("name:ru", name_ru);
        result.put("name:be", name_be);
        result.put("int_name", int_name);
        result.put("name:be-tarask", name_be_tarask);
        result.put("name:be-x-old", null);
        if (typ != null) {
            result.put("place", typ);
        }
        result.put("alt_name:ru", variants_ru);
        result.put("alt_name:be", variants_be);
        result.put("alt_name", null);
        result.put("alt_name:en", null);

        return result;
    }

    static String variantsToString(List<String> variants) {
        if (variants.isEmpty()) {
            return null;
        }
        StringBuilder out = new StringBuilder(200);
        for (String v : variants) {
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
}
