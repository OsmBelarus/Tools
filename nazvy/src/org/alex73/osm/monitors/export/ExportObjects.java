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
import java.util.Map;

import javax.xml.bind.JAXBContext;

import org.alex73.osmemory.MemoryStorage;
import org.alex73.osmemory.geometry.FastArea;

/**
 * This class exports some critical objects to text file for commit to git. It allows to monitor changes of
 * these objects.
 * 
 * @deprecated use ExportObjectByType
 */
public class ExportObjects {

    OutputFormatter formatter;
    Config config;

    public ExportObjects() throws Exception {
        JAXBContext CTX = JAXBContext.newInstance(Config.class);
        config = (Config) CTX.createUnmarshaller().unmarshal(new File("monitor-config.xml"));
    }

    public void export(Map<String, FastArea> borders, MemoryStorage osm) throws Exception {
        for (Map.Entry<String, FastArea> en : borders.entrySet()) {
            List<MonitorContext> monitors = new ArrayList<>();
            for (Monitor m : config.getMonitor()) {
                monitors.add(new MonitorContext(osm, m, en.getValue()));
            }

            // formatter = new OutputFormatter(osm);

            for (MonitorContext m : monitors) {
                osm.all(o -> m.process(o));
            }

            File outdir = new File("../../OsmBelarus-Monitoring/" + en.getKey());
            outdir.mkdirs();
            for (int i = 0; i < monitors.size(); i++) {
                monitors.get(i).dump(outdir);
            }
        }
    }
}
