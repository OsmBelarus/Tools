/**************************************************************************
 
Some tools for OSM.

 Copyright (C) 2014 Aleś Bułojčyk <alex73mail@gmail.com>
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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alex73.osm.utils.Belarus;
import org.alex73.osm.utils.RehijonTypeSeparator;
import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.IOsmObjectID;
import org.alex73.osmemory.OsmObjectID;
import org.alex73.osmemory.OsmSimpleNode;
import org.alex73.osmemory.XMLDriver;
import org.alex73.osmemory.XMLReader.UPDATE_MODE;

import osm.xmldatatypes.Node;
import osm.xmldatatypes.Relation;
import osm.xmldatatypes.Way;

/**
 * Экспартуе аб'екты па тыпах.
 */
public class ExportObjectsByType extends RehijonTypeSeparator implements XMLDriver.IApplyChangeCallback {
    
    Map<String, ExportOutput> outputs;
    Set<IOsmObjectID> queueObjects = new HashSet<>();

    public ExportObjectsByType(Belarus osm, Borders borders, Set<String> unusedFiles) throws Exception {
        super(osm,borders);

        outputs = new HashMap<>();
        for (ExportOutput eo : ExportOutput.list(checkTypes, borders, unusedFiles)) {
            outputs.put(eo.key(), eo);
        }
    }

    /**
     * Апрацоўвае ўсе аб'екты Беларусі.
     */
    public void collectData() throws Exception {
        System.out.println(new Date() + " Collect data...");

        outputs.values().forEach(o -> o.clear());

        List<IOsmObject> queue = new ArrayList<>();
        osm.all(o -> queue.add(o));

        queue.parallelStream().filter(o -> o.getTags().length > 0 && osm.contains(o))
                .forEach(o -> process(o));
        outputs.values().parallelStream().forEach(o -> o.finishUpdate());
    }

    public void afterChangeset() {
        outputs.values().parallelStream()
                .forEach(out -> queueObjects.stream().forEach(obj -> out.forgetInQueue(obj)));

        List<IOsmObject> queue = new ArrayList<>();
        for (IOsmObjectID objID : queueObjects) {
            IOsmObject obj = osm.getObject(objID);
            if (obj == null || obj instanceof OsmSimpleNode) {
                continue;
            }
            queue.add(obj);
        }
        queueObjects.clear();

        queue.parallelStream().filter(o -> o.getTags().length > 0 && osm.contains(o))
                .forEach(o -> process(o));
        outputs.values().parallelStream().forEach(o -> o.finishUpdate());
    }

    public void saveExport(GitClient git) {
        System.out.println(new Date() + " Write...");
        outputs.values().parallelStream().forEach(o -> o.save(git, osm));
    }

    @Override
    public void beforeUpdateNode(UPDATE_MODE mode, Node node) {
    }

    @Override
    public void afterUpdateNode(UPDATE_MODE mode, Node node) {
        queueObjects.add(new OsmObjectID(IOsmObject.TYPE_NODE, node.getId()));
    }

    @Override
    public void beforeUpdateWay(UPDATE_MODE mode, Way way) {
    }

    @Override
    public void afterUpdateWay(UPDATE_MODE mode, Way way) {
        queueObjects.add(new OsmObjectID(IOsmObject.TYPE_WAY, way.getId()));
    }

    @Override
    public void beforeUpdateRelation(UPDATE_MODE mode, Relation relation) {
    }

    @Override
    public void afterUpdateRelation(UPDATE_MODE mode, Relation relation) {
        queueObjects.add(new OsmObjectID(IOsmObject.TYPE_RELATION, relation.getId()));
    }

    /**
     * Захоўвае ў адпаведны ExportOutput.
     */
    @Override
    synchronized protected void storeToOutput(IOsmObject obj, String typ, String rehijon) throws Exception {
        String key = ExportOutput.key(typ, rehijon);
        ExportOutput o = outputs.get(key);
        if (o == null) {
            // new file
            o = new ExportOutput(typ, rehijon);
            outputs.put(key, o);
        }
        o.out(obj);
    }
}
