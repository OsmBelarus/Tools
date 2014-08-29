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

import javax.xml.bind.JAXBContext;

import org.alex73.osm.data.MemoryStorage;
import org.alex73.osm.data.NodeObject;
import org.alex73.osm.data.PbfDriver;
import org.alex73.osm.data.RelationObject;
import org.alex73.osm.data.WayObject;
import org.alex73.osm.utils.Env;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;

/**
 * This class exports some critical objects to text file for commit to git. It
 * allows to monitor changes of these objects.
 */
public class ExportObjects {
    static MemoryStorage osm;
    static OutputFormatter formatter;

    public static void main(String[] args) throws Exception {
        Env.load();

        JAXBContext CTX = JAXBContext.newInstance(Config.class);
        Config config = (Config) CTX.createUnmarshaller().unmarshal(new File("monitor-config.xml"));

        File outdir = new File("../../OsmBelarus-Monitoring/");
        List<MonitorContext> monitors = new ArrayList<>();
        for (Monitor m : config.getMonitor()) {
            monitors.add(new MonitorContext(m, outdir));
        }

        final CoordCache cache = new CoordCache();

        osm = PbfDriver.process(new File("tmp/belarus-latest.osm.pbf"), new PbfDriver.Filter() {
            public boolean acceptNode(Node n) {
                return cache.isInside(n.getLongitude(), n.getLatitude());
            }

            public boolean acceptWay(MemoryStorage storage, WayObject w) {
                for (int i = 0; i < w.nodeIds.length; i++) {
                    long nid = w.nodeIds[i];
                    if (storage.getNodeById(nid) != null) {
                        return true;
                    }
                }
                return false;
            }

            public boolean acceptRelation(MemoryStorage storage, RelationObject r) {
                return true;
            }
        });
        formatter = new OutputFormatter(osm);

        for (NodeObject n : osm.nodes) {
            for (int i = 0; i < monitors.size(); i++) {
                monitors.get(i).process(n);
            }
        }
        for (WayObject w : osm.ways) {
            for (int i = 0; i < monitors.size(); i++) {
                monitors.get(i).process(w);
            }
        }
        for (RelationObject r : osm.relations) {
            for (int i = 0; i < monitors.size(); i++) {
                monitors.get(i).process(r);
            }
        }

        for (int i = 0; i < monitors.size(); i++) {
            monitors.get(i).dump(osm);
        }
    }
}
