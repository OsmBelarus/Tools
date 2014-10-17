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

import gen.alex73.osm.monitor.Config;
import gen.alex73.osm.monitor.Monitor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.bind.JAXBContext;

import org.alex73.osm.utils.Belarus;
import org.alex73.osm.utils.Env;
import org.alex73.osm.utils.Lat;
import org.alex73.osm.utils.PadzielOsmNas;
import org.alex73.osm.utils.CSV;
import org.alex73.osmemory.geometry.Area;
import org.alex73.osmemory.geometry.FastArea;

/**
 * This class exports some critical objects to text file for commit to git. It allows to monitor changes of
 * these objects.
 */
public class ExportObjects {
    static Belarus osm;
    static OutputFormatter formatter;
    static List<Rehijon> rehijony = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ENGLISH);

        JAXBContext CTX = JAXBContext.newInstance(Config.class);
        Config config = (Config) CTX.createUnmarshaller().unmarshal(new File("monitor-config.xml"));

        osm = new Belarus();

        loadRehijony();
        for (Rehijon r : rehijony) {
            List<MonitorContext> monitors = new ArrayList<>();
            for (Monitor m : config.getMonitor()) {
                monitors.add(new MonitorContext(osm, m, r.area));
            }

            // formatter = new OutputFormatter(osm);

            for (MonitorContext m : monitors) {
                osm.all(o -> m.process(o));
            }

            File outdir = new File("../../OsmBelarus-Monitoring/" + r.nazva);
            outdir.mkdirs();
            for (int i = 0; i < monitors.size(); i++) {
                monitors.get(i).dump(outdir);
            }
        }
    }

    static void loadRehijony() throws Exception {
        List<PadzielOsmNas> padziel = new CSV('\t').readCSV(Env.readProperty("dav") + "/Rehijony.csv",
                PadzielOsmNas.class);
        for (PadzielOsmNas p : padziel) {
            String path = "/";
            if (p.voblasc != null) {
                path += p.voblasc + " вобласць" + '/';
            }
            if (p.rajon != null) {
                path += p.rajon + " раён" + '/';
            }
            Rehijon rehijon = new Rehijon();
            rehijon.nazva = Lat.unhac(Lat.lat(path, false)).replace(' ', '_');
            rehijon.area = new FastArea(new Area(osm, osm.getRelationById(p.relationID)).getGeometry(), osm);
            rehijony.add(rehijon);
        }
    }

    public static class Rehijon {
        public String nazva;
        public FastArea area;
    }
}
