/**************************************************************************
 
Some tools for OSM.

 Copyright (C) 2013 Aleś Bułojčyk <alex73mail@gmail.com>
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

package org.alex73.osm.monitors.export;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.xml.bind.JAXBContext;

import org.alex73.osm.utils.Belarus;
import org.alex73.osm.utils.Env;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import osm.xmldatatypes.Changeset;
import osm.xmldatatypes.Osm;
import osm.xmldatatypes.OsmChange;

/**
 * Чытае усе файлы з http://planet.openstreetmap.org/replication/changesets/ і выбірае тыя што датычацца
 * Беларусі і паміж 2 датамі.
 */
public class ReadChangesets {
    static JAXBContext CONTEXT;

    /**
     * Дастаем changeset'ы паміж before і after. Калі after==null, дастаем усе вядомые пасьля before.
     */
    public static List<Changeset> retrieve(Set<Long> knownChangesetsBefore, Set<Long> knownChangesetsAfter)
            throws Exception {
        System.out.println("Retrieve changesets");
        CONTEXT = JAXBContext.newInstance(OsmChange.class, Osm.class);

        List<Changeset> forProcess = new ArrayList<>();

        read: for (long i = readLast(); i >= 0; i--) {
            List<Changeset> changesets = readSeq(i);

            changesets.removeIf(ch -> ch.getClosedAt() == null); // выдаляем яшчэ незавершаныя

            if (changesets.isEmpty()) {
                continue;
            }

            // changesets from latest to previous
            sort(changesets);
            Collections.reverse(changesets);

            boolean beforeAfter = knownChangesetsAfter == null;
            for (Changeset ch : changesets) {
                if (!beforeAfter && knownChangesetsAfter.contains(ch.getId())) {
                    // калі changeset ёсьць у after, усё што да яго - апрацаўваем
                    beforeAfter = true;
                }
                if (knownChangesetsBefore.contains(ch.getId())) {
                    // калі changeset ёсьць у before, больш старыя не шукаем
                    break read;
                }
                if (beforeAfter && !isOutside(ch)) {
                    forProcess.add(ch);
                }
            }
        }

        sort(forProcess);
        return forProcess;
    }

    public static void sort(List<Changeset> changesets) {
        Collections.sort(changesets, new Comparator<Changeset>() {
            @Override
            public int compare(Changeset o1, Changeset o2) {
                long t1 = o1.getClosedAt().toGregorianCalendar().getTimeInMillis();
                long t2 = o2.getClosedAt().toGregorianCalendar().getTimeInMillis();
                return Long.compare(t1, t2);
            }
        });
    }

    /**
     * Чытае changeset па ID.
     */
    public static byte[] download(Changeset ch) throws Exception {
        File cache = new File(Env.readProperty("data.cache") + "/changesets/" + ch.getId() + ".xml");
        byte[] xml;
        if (cache.exists()) {
            xml = FileUtils.readFileToByteArray(cache);
        } else {
            xml = get("http://www.openstreetmap.org/api/0.6/changeset/" + ch.getId() + "/download");
            cache.getParentFile().mkdirs();
            FileUtils.writeByteArrayToFile(cache, xml);
        }
        return xml;
    }

    static Pattern RE_YAML = Pattern.compile("sequence: ([0-9]+)");

    /**
     * Чытае нумар апошняга файлу.
     */
    static long readLast() throws Exception {
        byte[] yaml = get("http://planet.openstreetmap.org/replication/changesets/state.yaml");
        BufferedReader rd = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(yaml)));

        Long result = null;
        String s;
        while ((s = rd.readLine()) != null) {
            Matcher m = RE_YAML.matcher(s);
            if (m.matches()) {
                result = Long.parseLong(m.group(1));
            }
        }
        if (result == null) {
            throw new Exception("No sequence");
        }
        return result;
    }

    /**
     * Чытае osm з сьпісам changeset'аў.
     */
    static List<Changeset> readSeq(long index) throws Exception {
        String s = new DecimalFormat("000000000").format(index);
        File cache = new File(Env.readProperty("data.cache") + "/changesets/" + s + ".osm.gz");
        byte[] xml;
        if (cache.exists()) {
            xml = FileUtils.readFileToByteArray(cache);
        } else {
            xml = get("http://planet.openstreetmap.org/replication/changesets/" + s.substring(0, 3) + "/"
                    + s.substring(3, 6) + "/" + s.substring(6) + ".osm.gz");
            cache.getParentFile().mkdirs();
            FileUtils.writeByteArrayToFile(cache, xml);
        }
        Osm result = (Osm) CONTEXT.createUnmarshaller().unmarshal(
                new GZIPInputStream(new ByteArrayInputStream(xml)));
        return result.getChangeset();
    }

    /**
     * Ці па-за межамі Беларусі ?
     */
    static boolean isOutside(Changeset ch) {
        if (ch.getMinLat() > Belarus.MAX_LAT || ch.getMaxLat() < Belarus.MIN_LAT
                || ch.getMinLon() > Belarus.MAX_LON || ch.getMaxLon() < Belarus.MIN_LON) {
            return true;
        }
        return false;
    }

    /**
     * Чытае URL.
     */
    static byte[] get(String url) throws Exception {
        System.out.println("Load " + url);
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        if (conn.getResponseCode() != 200) {
            throw new IOException("Call " + url + ": " + conn.getResponseCode() + " "
                    + conn.getResponseMessage());
        }
        try {
            try (InputStream in = conn.getInputStream()) {
                return IOUtils.toByteArray(in);
            }
        } finally {
            conn.disconnect();
        }
    }
}
