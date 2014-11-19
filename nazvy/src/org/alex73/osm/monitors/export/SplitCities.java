package org.alex73.osm.monitors.export;

import java.util.ArrayList;
import java.util.List;

import org.alex73.osm.monitors.export.ExportObjectsByType.Rehijon;
import org.alex73.osm.utils.Belarus;
import org.alex73.osmemory.IOsmNode;
import org.alex73.osmemory.geometry.GeometryHelper;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

public class SplitCities {
    static final int NODE_MATRIX_SIZE = 100;

    Belarus osm;
    Object[][] split;
    private int minx, maxx, miny, maxy;
    private int stepx, stepy;

    public SplitCities(Belarus osm) {
        this.osm = osm;
    }

    public void split(List<Rehijon> rehijony) {
        // рыхтуем матрыцу 100x100 для пошуку node

        split = new Object[NODE_MATRIX_SIZE][];
        for (int i = 0; i < NODE_MATRIX_SIZE; i++) {
            split[i] = new Object[NODE_MATRIX_SIZE];
        }

        Envelope bo = osm.getGeometry().getEnvelopeInternal();
        minx = (int) (bo.getMinX() / IOsmNode.DIVIDER) - 1;
        maxx = (int) (bo.getMaxX() / IOsmNode.DIVIDER) + 1;
        miny = (int) (bo.getMinY() / IOsmNode.DIVIDER) - 1;
        maxy = (int) (bo.getMaxY() / IOsmNode.DIVIDER) + 1;

        // can be more than 4-byte signed integer
        long dx = ((long) maxx) - ((long) minx);
        long dy = ((long) maxy) - ((long) miny);
        stepx = (int) (dx / NODE_MATRIX_SIZE + 1);
        stepy = (int) (dy / NODE_MATRIX_SIZE + 1);

        for (int i = 0; i < NODE_MATRIX_SIZE; i++) {
            for (int j = 0; j < NODE_MATRIX_SIZE; j++) {
                List<Rehijon> rm = new ArrayList<>();
                for (Rehijon r : rehijony) {
                    if (mayCover(r, i, j)) {
                        rm.add(r);
                    }
                }
                split[i][j] = rm;
            }
        }
    }

    public List<Rehijon> getList(int lat, int lon) {
        int x = lon;
        int y = lat;

        int ix = (x - minx) / stepx;
        int iy = (y - miny) / stepy;
        return (List<Rehijon>) split[ix][iy];
    }

    boolean mayCover(Rehijon r, int ix, int iy) {
        int ulx = minx + ix * stepx;
        int uly = miny + iy * stepy;
        double mix = ulx * IOsmNode.DIVIDER;
        double max = (ulx + stepx - 1) * IOsmNode.DIVIDER;
        double miy = uly * IOsmNode.DIVIDER;
        double may = (uly + stepy - 1) * IOsmNode.DIVIDER;
        Polygon p = GeometryHelper.createBoxPolygon(mix, max, miy, may);
        Geometry intersection = r.area.getGeometry().intersection(p);
        return !intersection.isEmpty();
    }
}
