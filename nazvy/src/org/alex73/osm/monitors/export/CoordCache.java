package org.alex73.osm.monitors.export;

import java.io.File;

import org.apache.commons.io.FileUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.WKTReader;

/**
 * Правярае ці знаходзіцца кропка ў межах Беларусі. Мае кэш для значнага
 * паскарэньня.
 */
public class CoordCache {
    static final double DIVIDER = 4;
    final GeometryFactory GEOM = new GeometryFactory(new PrecisionModel(1000000000));

    final Geometry GEO_EMPTY = GEOM.createPoint(new Coordinate(0, 0));
    final Geometry GEO_FULL = GEOM.createPoint(new Coordinate(1, 1));

    final int minx, maxx, miny, maxy;
    final Geometry border;
    final Geometry[][] cachedGeo;

    public CoordCache() throws Exception {
        String borderWKT = FileUtils.readFileToString(new File("Belarus-79842.wkt"));
        border = new WKTReader(GEOM).read(borderWKT);
        Envelope bo = border.getEnvelopeInternal();
        minx = (int) (bo.getMinX() * DIVIDER);
        maxx = (int) (bo.getMaxX() * DIVIDER) + 1;
        miny = (int) (bo.getMinY() * DIVIDER);
        maxy = (int) (bo.getMaxY() * DIVIDER) + 1;
        cachedGeo = new Geometry[maxx - minx][];
        for (int i = 0; i < cachedGeo.length; i++) {
            cachedGeo[i] = new Geometry[maxy - miny];
        }
        for (int x = minx; x < maxx; x++) {
            for (int y = miny; y < maxy; y++) {
                double mix = x / DIVIDER;
                double max = (x + 1) / DIVIDER;
                double miy = y / DIVIDER;
                double may = (y + 1) / DIVIDER;
                Polygon p = GEOM.createPolygon(new Coordinate[] { new Coordinate(mix, miy), new Coordinate(mix, may),
                        new Coordinate(max, may), new Coordinate(max, miy), new Coordinate(mix, miy), });
                Geometry intersection = border.intersection(p);
                if (intersection.isEmpty()) {
                    cachedGeo[x - minx][y - miny] = GEO_EMPTY;
                } else if (intersection.equalsExact(p)) {
                    cachedGeo[x - minx][y - miny] = GEO_FULL;
                } else {
                    cachedGeo[x - minx][y - miny] = intersection;// .union(intersection.buffer(0.001));
                }
            }
        }
    }

    boolean isInside(double longitude, double latitude) {
        int x = (int) (longitude * DIVIDER);
        int y = (int) (latitude * DIVIDER);
        if (x < minx || x >= maxx || y < miny || y >= maxy) {
            return false;
        }
        Geometry cached = cachedGeo[x - minx][y - miny];
        if (cached == GEO_EMPTY) {
            return false;
        } else if (cached == GEO_FULL) {
            return true;
        } else {
            Point p = createPoint(longitude, latitude);
            return cached.covers(p);
        }
    }

    Point createPoint(double longitude, double latitude) {
        Coordinate coord = new Coordinate(longitude, latitude);
        GEOM.getPrecisionModel().makePrecise(coord);
        return GEOM.createPoint(coord);
    }
}
