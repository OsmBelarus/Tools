/**************************************************************************
 Some tools for OSM.

 Copyright (C) 2013-2014 Aleś Bułojčyk <alex73mail@gmail.com>
               Home page: http://www.omegat.org/
               Support center: http://groups.yahoo.com/group/OmegaT/

 This is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This software is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/

package org.alex73.osm.validators.objects;

import gen.alex73.osm.validators.objects.ObjectTypes;
import gen.alex73.osm.validators.objects.Trap;
import gen.alex73.osm.validators.objects.Type;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.alex73.osm.utils.Belarus;
import org.alex73.osm.utils.Env;
import org.alex73.osm.utils.Lat;
import org.alex73.osm.utils.OSM;
import org.alex73.osm.utils.VelocityOutput;
import org.alex73.osmemory.IOsmObject;

/**
 * Правярае ўсе аб'екты Беларусі па object-types.xml.
 */
public class CheckObjects {

    static List<CheckType> knownTypes;
    static List<CheckTrap> traps;

    public static Belarus osm;
    static Map<String, Set<String>> errors = new HashMap<>();
    static Map<String, Map<String, Set<String>>> errorsByUser = new HashMap<>();
    static Map<String, Integer> objectsCount = new HashMap<>();
    static int[] tagsCount = new int[Short.MAX_VALUE];

    public static void main(String[] args) throws Exception {
        String out = Env.readProperty("out.dir") + "/pamylki.html";

        osm = new Belarus();

        readConfig();

        osm.all(o -> check(o));
        osm.all(o -> osm.contains(o), o -> addToStatistics(o));
        for (CheckType ct : knownTypes) {
            ct.finish();
        }
        for (CheckTrap ct : traps) {
            ct.finish();
        }

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
                OSM.sort(errors.keySet()), "errors", errors, "OSM", OSM.class, "users",
                OSM.sort(errorsByUser.keySet()), "this", CheckObjects.class, "objects",
                OSM.sort(objectsCount.keySet()), "objectsCount", objectsCount, "tagsCount",
                getTagsCountByNames());
        for (String user : errorsByUser.keySet()) {
            String uout = Env.readProperty("out.dir") + "/pamylki-" + lat(user) + ".html";
            Map<String, Set<String>> e = errorsByUser.get(user);
            VelocityOutput.output("org/alex73/osm/validators/objects/pamylki.velocity", uout, "errorsList",
                    OSM.sort(e.keySet()), "errors", e, "OSM", OSM.class, "user", user);
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
        if (!osm.contains(obj)) {
            return;
        }

        if (!matches) {
            for (CheckTrap ct : traps) {
                if (ct.matches(obj)) {
                    if (osm.contains(obj)) {
                        ct.getErrors(obj);
                    }
                    break;
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

    static void addToStatistics(IOsmObject o) {
        for (short t : o.getTags()) {
            tagsCount[t]++;
        }
    }

    static Map<String, Integer> getTagsCountByNames() {
        Map<String, Integer> result = new HashMap<>();
        for (short i = 0; i < tagsCount.length; i++) {
            if (tagsCount[i] > 0) {
                result.put(osm.getTagsPack().getTagName(i), tagsCount[i]);
            }
        }
        return result;
    }

    static void readConfig() throws Exception {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(new StreamSource(new File("src/object_types.xsd")));
        JAXBContext CTX = JAXBContext.newInstance(ObjectTypes.class);
        Unmarshaller unm = CTX.createUnmarshaller();
        unm.setSchema(schema);
        ObjectTypes types = (ObjectTypes) unm.unmarshal(new File("object-types.xml"));
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

    static void addError(String objectCode, String errorText) {
        getSet(errors, errorText).add(objectCode);
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
