package org.alex73.osm.monitors.export;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class DataFileStatus  {

    public long initialDate;
    public Set<Long> knownChangesets = new HashSet<>();

    public void dump() {
        System.out.println("Initial date: " + new Date(initialDate).toGMTString());
        System.out.println("Known changesets count: " + knownChangesets.size());
    }
}
