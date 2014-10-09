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

package org.alex73.osm.validators.changesets;

import gen.alex73.osm.xmldatatypes.Changeset;
import gen.alex73.osm.xmldatatypes.Node;
import gen.alex73.osm.xmldatatypes.Osm;
import gen.alex73.osm.xmldatatypes.OsmBasicChange;
import gen.alex73.osm.xmldatatypes.OsmBasicType;
import gen.alex73.osm.xmldatatypes.OsmChange;
import gen.alex73.osm.xmldatatypes.Relation;
import gen.alex73.osm.xmldatatypes.Tag;
import gen.alex73.osm.xmldatatypes.Way;

import java.awt.geom.Area;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javax.xml.bind.JAXBContext;

import org.alex73.osm.utils.Env;
import org.alex73.osm.utils.Geo;
import org.alex73.osm.utils.VelocityOutput;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * Паказвае апошнія changeset'ы як rss.
 */
public class ListChangeSets {

    static JAXBContext CONTEXT;

    static Set<Long> CHANGESETS = new HashSet<>();

    public static void main(String[] args) throws Exception {
        CONTEXT = JAXBContext.newInstance(OsmChange.class, Osm.class);

        Env.load();

        String out = Env.readProperty("out.dir") + "/changes.xml";

        OsmChange change = (OsmChange) CONTEXT.createUnmarshaller().unmarshal(
                new GZIPInputStream(new FileInputStream(new File("tmp-changesets/updates.osc.gz"))));
        for (OsmBasicChange ch : change.getCreate()) {
            processChange(ch);
        }
        for (OsmBasicChange ch : change.getModify()) {
            processChange(ch);
        }
        for (OsmBasicChange ch : change.getDelete()) {
            processChange(ch);
        }

        List<Changeset> list = new ArrayList<>();
        for (long id : CHANGESETS) {
            for (Changeset ch : getChansegetInfo(id).getChangeset()) {
                Area bb = Geo.box2area(ch.getMinLon(), ch.getMinLat(), ch.getMaxLon(), ch.getMaxLat());
                if (Geo.isInside(Geo.BELARUS, bb)) {
                    // inside - process
                    list.add(ch);
                }
            }
        }

        Collections.sort(list, new Comparator<Changeset>() {
            public int compare(Changeset o1, Changeset o2) {
                Calendar c1 = o1.getClosedAt().toGregorianCalendar();
                Calendar c2 = o2.getClosedAt().toGregorianCalendar();
                int c = -c1.compareTo(c2);
                if (c == 0) {
                    c = -Long.compare(o1.getId(), o2.getId());
                }
                return c;
            }
        });

        SimpleDateFormat SHORT_TIME = new SimpleDateFormat("HH:mm");

        List<Change> changes = new ArrayList<>();
        for (Changeset ch : list) {
            System.out.println(ch.getUser() + " at " + ch.getClosedAt());
            Change c = new Change();
            c.changesetId = ch.getId();
            c.author = ch.getUser();
            c.closedDate = ch.getClosedAt().toXMLFormat();
            c.comment = getComment(ch);

            Calendar time = ch.getClosedAt().toGregorianCalendar();
            c.shortTime = SHORT_TIME.format(time.getTime());

            OsmChange content = getChangesetContent(ch.getId());
            c.countM = content.getModify().size();
            c.countC = content.getCreate().size();
            c.countD = content.getDelete().size();

            changes.add(c);
        }

        VelocityOutput.output("org/alex73/osm/validators/changesets/changes.velocity", out, "changes",
                changes);
    }

    public static class Change {
        public long changesetId;
        public String author;
        public String closedDate;
        public String shortTime;
        public String comment;
        public int countM, countC, countD;
    }

    static String getComment(Changeset ch) {
        for (Tag t : ch.getTag()) {
            if ("comment".equals(t.getK())) {
                return t.getV();
            }
        }
        return "no comments";
    }

    static Osm getChansegetInfo(long id) throws Exception {
        File f1 = new File("tmp-changesets/" + id + "-info.xml");
        byte[] chdata;
        if (f1.exists()) {
            chdata = FileUtils.readFileToByteArray(f1);
        } else {
            chdata = get("/api/0.6/changesets?closed=true&changesets=" + id);
            f1.getParentFile().mkdirs();
            FileUtils.writeByteArrayToFile(f1, chdata);
        }
        return (Osm) CONTEXT.createUnmarshaller().unmarshal(new ByteArrayInputStream(chdata));
    }

    static OsmChange getChangesetContent(long id) throws Exception {
        File f1 = new File("tmp-changesets/" + id + ".xml");
        byte[] chdata;
        if (f1.exists()) {
            chdata = FileUtils.readFileToByteArray(f1);
        } else {
            chdata = get("/api/0.6/changeset/" + id + "/download");
            f1.getParentFile().mkdirs();
            FileUtils.writeByteArrayToFile(f1, chdata);
        }
        return (OsmChange) CONTEXT.createUnmarshaller().unmarshal(new ByteArrayInputStream(chdata));
    }

    static void processChange(OsmBasicChange ch) {
        for (Node e : ch.getNode()) {
            processElement(e);
        }
        for (Way e : ch.getWay()) {
            processElement(e);
        }
        for (Relation e : ch.getRelation()) {
            processElement(e);
        }
    }

    static void processElement(OsmBasicType e) {
        CHANGESETS.add(e.getChangeset());
    }

    static final String API_URL = "http://api.openstreetmap.org";

    static byte[] get(String apiCall) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(API_URL + apiCall).openConnection();
        if (conn.getResponseCode() != 200) {
            throw new IOException("Call " + apiCall + ": " + conn.getResponseCode() + " "
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
