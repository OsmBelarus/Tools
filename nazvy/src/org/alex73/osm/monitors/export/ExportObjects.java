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

import org.alex73.osm.utils.Env;
import org.alex73.osmemory.FastPolygon;
import org.alex73.osmemory.MemoryStorage2;
import org.alex73.osmemory.O5MReaderMy;
import org.alex73.osmemory.PbfReader2;
import org.alex73.osmemory.Polygon;
import org.alex73.osmemory.o5mReader;
import org.apache.commons.io.FileUtils;

/**
 * This class exports some critical objects to text file for commit to git. It allows to monitor changes of
 * these objects.
 */
public class ExportObjects {
    static MemoryStorage2 osm;
    static OutputFormatter formatter;

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ENGLISH);
        Env.load();

        JAXBContext CTX = JAXBContext.newInstance(Config.class);
        Config config = (Config) CTX.createUnmarshaller().unmarshal(new File("monitor-config.xml"));

        File outdir = new File("../../OsmBelarus-Monitoring/");

        String borderWKT = FileUtils.readFileToString(new File(Env.readProperty("coutry.border.wkt")),
                "UTF-8");
        Polygon Belarus = Polygon.fromWKT(borderWKT);

        osm = new O5MReaderMy(Belarus.getBoundEnvelope()).read(new File(Env.readProperty("data.file")));
        osm.showStat();

        List<MonitorContext> monitors = new ArrayList<>();
        for (Monitor m : config.getMonitor()) {
            monitors.add(new MonitorContext(osm, m, new FastPolygon(Belarus, osm)));
        }

        formatter = new OutputFormatter(osm);

        for (MonitorContext m : monitors) {
            osm.all().processAll(o -> m.process(o));
        }

        for (int i = 0; i < monitors.size(); i++) {
            monitors.get(i).dump(outdir);
        }
    }
}
