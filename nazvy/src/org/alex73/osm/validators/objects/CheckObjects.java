package org.alex73.osm.validators.objects;

import gen.alex73.osm.validators.objects.ObjectTypes;
import gen.alex73.osm.validators.objects.Trap;
import gen.alex73.osm.validators.objects.Type;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;

import org.alex73.osm.utils.Env;
import org.alex73.osm.utils.Lat;
import org.alex73.osm.utils.OSM;
import org.alex73.osm.utils.VelocityOutput;
import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.MemoryStorage;
import org.alex73.osmemory.O5MReader;
import org.alex73.osmemory.geometry.Area;
import org.alex73.osmemory.geometry.FastArea;
import org.apache.commons.io.FileUtils;

public class CheckObjects {

    static FastArea Belarus;
    static List<CheckType> knownTypes;
    static List<CheckTrap> traps;
    static MemoryStorage osm;
    static Map<String, Set<String>> errors = new HashMap<>();
    static Map<String, Map<String, Set<String>>> errorsByUser = new HashMap<>();
    static Map<String, Integer> objectsCount = new HashMap<>();

    public static void main(String[] args) throws Exception {
        Env.load();
        String out = Env.readProperty("out.dir") + "/pamylki2.html";

        String borderWKT = FileUtils.readFileToString(new File(Env.readProperty("coutry.border.wkt")),
                "UTF-8");
        Area BelarusBorder = Area.fromWKT(borderWKT);

        osm = new O5MReader(BelarusBorder.getBoundingBox()).read(new File(Env.readProperty("data.file")));
        osm.showStat();

        Belarus = new FastArea(BelarusBorder.getGeometry(), osm);

        readConfig();

        osm.all(o -> check(o));

        File[] oldFiles = new File(out).getParentFile().listFiles();
        if (oldFiles != null) {
            for (File f : oldFiles) {
                if (f.getName().startsWith("pamylki-")) {
                    f.delete();
                }
            }
        }
        new File(out).getParentFile().mkdirs();
        VelocityOutput.output("org/alex73/osm/validators/objects/pamylki.velocity", out, "errorsList",
                sort(errors.keySet()), "errors", errors, "OSM", OSM.class, "users",
                sort(errorsByUser.keySet()), "this", CheckObjects.class, "objects",
                sort(objectsCount.keySet()), "objectsCount", objectsCount);
        for (String user : errorsByUser.keySet()) {
            String uout = Env.readProperty("out.dir") + "/pamylki-" + lat(user) + ".html";
            Map<String, Set<String>> e = errorsByUser.get(user);
            VelocityOutput.output("org/alex73/osm/validators/objects/pamylki.velocity", uout, "errorsList",
                    sort(e.keySet()), "errors", e, "OSM", OSM.class, "user", user);
        }
    }

    static List<CheckType> otypes = new ArrayList<>();

    static void check(IOsmObject obj) {
        otypes.clear();
        // фільтраваньне тыпаў
        boolean matches = false;
        for (CheckType ct : knownTypes) {
            if (ct.matches(obj)) {
                if (ct.getType().isMain()) {
                    matches = true;
                }
                otypes.add(ct);
            }
        }
        if (!Belarus.contains(obj)) {
            return;
        }

        if (!matches) {
            for (CheckTrap ct : traps) {
                if (ct.matches(obj)) {
                    CheckObjects.addError(obj, ct.getTrap().getMessage());
                }
            }
            return;
        }

        incMap(objectsCount, otypes.get(0).getId());

        // магчымыя спалучэньні тыпаў
        Set<String> allowedTypes = otypes.get(0).getAdditions();
        for (int i = 1; i < otypes.size(); i++) {
            String thisAddition = otypes.get(i).getId();
            if (!allowedTypes.contains(thisAddition)) {
                otypes.remove(i);
                i--;
            }
        }

        // памылкі
        for (CheckType ct : otypes) {
            ct.getErrors(obj);
        }

        // непатрэбныя тэгі
        for (short t : obj.getTags()) {
            boolean has = false;
            for (CheckType ct : otypes) {
                String m = ct.matchTag(obj, t);
                if (m == CheckType.OK) {
                    has = true;
                } else if (m != null) {
                    has = true;
                    CheckObjects.addError(obj, m);
                }
            }
            if (!has) {
                CheckObjects.addError(obj, "'" + otypes.get(0).getId() + "' ня можа ўтрымліваць тэг '"
                        + osm.getTagsPack().getTagName(t) + "'");
            }
        }
    }

    static void readConfig() throws Exception {
        JAXBContext CTX = JAXBContext.newInstance(ObjectTypes.class);
        ObjectTypes types = (ObjectTypes) CTX.createUnmarshaller().unmarshal(new File("object-types.xml"));
        knownTypes = new ArrayList<>();
        for (Type t : types.getType()) {
            knownTypes.add(new CheckType(osm, t));
        }
        traps = new ArrayList<>();
        for (Trap t : types.getTrap()) {
            traps.add(new CheckTrap(osm, t));
        }
    }

    static void addError(IOsmObject obj, String errorText) {
        getSet(errors, errorText).add(obj.getObjectCode());

        String user = obj.getUser(osm);
        getSet(getMap(errorsByUser, user), errorText).add(obj.getObjectCode());
    }

    static <T> Set<T> getSet(Map<String, Set<T>> map, String key) {
        Set<T> s = map.get(key);
        if (s == null) {
            s = new HashSet<>();
            map.put(key, s);
        }
        return s;
    }

    static <T> Map<String, T> getMap(Map<String, Map<String, T>> map, String key) {
        Map<String, T> s = map.get(key);
        if (s == null) {
            s = new HashMap<>();
            map.put(key, s);
        }
        return s;
    }

    static void incMap(Map<String, Integer> map, String key) {
        Integer v = map.get(key);
        if (v == null) {
            v = 0;
        }
        map.put(key, v + 1);
    }

    static List<String> sort(Set<String> list) {
        List<String> result = new ArrayList<>(list);
        final Locale BE = new Locale("be");
        final Collator BEL = Collator.getInstance(BE);
        Collections.sort(result, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return BEL.compare(o1, o2);
            }
        });
        return result;
    }

    static public int getErrorsCount(String user) {
        Map<String, Set<String>> e = errorsByUser.get(user);
        int count = 0;
        for (Set<String> s : e.values()) {
            count += s.size();
        }
        return count;
    }

    static public String lat(String text) {
        text = text.replace('и', 'і').replace('И', 'І').replace("щ", "шч").replace("Щ", "Шч");
        String r = Lat.unhac(Lat.lat(text, true));
        r = r.replace(' ', '_').replace('"', '_').replace('\'', '_').replace('.', '_').replace('/', '_')
                .replace('\\', '_');
        return r;
    }
}
