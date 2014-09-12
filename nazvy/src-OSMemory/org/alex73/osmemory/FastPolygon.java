/**************************************************************************
 OSMemory library for OSM data processing.

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

package org.alex73.osmemory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;

/**
 * Polygon.contains call performance optimization. It can be used for checks like "if node inside country",
 * "if way inside city", etc.
 * 
 * This class creates 400(by default) cells above of polygon as cache. Each cell can have 3 states: fully
 * included into polygon, not included in polygon, included partially. For the fully included and not included
 * cells checking is very fast operation. For partially included - it checks only subpolygon inside cell, that
 * is also a little bit faster process.
 * 
 * Keep in mind, that only nodes inside polygon will be checked. That means, if you have way from upper left
 * corner into bottom right corner, i.e. overlaps polygon, but there is no point of ways inside polygon, then
 * this way will be treated as 'not contains'.
 */
public class FastPolygon {
    private static final int PARTS_COUNT_BYXY = 20;
    final GeometryFactory GEOM = new GeometryFactory(new PrecisionModel(10000000));

    final Geometry GEO_EMPTY = GEOM.createPoint(new Coordinate(0, 0));
    final Geometry GEO_FULL = GEOM.createPoint(new Coordinate(0, 0));

    final int minx, maxx, miny, maxy, stepx, stepy;
    final MemoryStorage2 storage;
    final Polygon poly;
    final Geometry[][] cachedGeo;

    public FastPolygon(Polygon poly, MemoryStorage2 storage) throws Exception {
        if (poly == null || storage == null) {
            throw new IllegalArgumentException();
        }
        this.storage = storage;
        this.poly = poly;
        Envelope bo = poly.geom.getEnvelopeInternal();
        minx = (int) (bo.getMinX() / NodeObject2.DIVIDER) - 1;
        maxx = (int) (bo.getMaxX() / NodeObject2.DIVIDER) + 1;
        miny = (int) (bo.getMinY() / NodeObject2.DIVIDER) - 1;
        maxy = (int) (bo.getMaxY() / NodeObject2.DIVIDER) + 1;

        // can be more than 4-byte signed integer
        long dx = ((long) maxx) - ((long) minx);
        long dy = ((long) maxy) - ((long) miny);

        stepx = (int) (dx / PARTS_COUNT_BYXY + 1);
        stepy = (int) (dy / PARTS_COUNT_BYXY + 1);
        cachedGeo = new Geometry[PARTS_COUNT_BYXY][];
        for (int i = 0; i < cachedGeo.length; i++) {
            cachedGeo[i] = new Geometry[PARTS_COUNT_BYXY];
        }
    }

    Geometry calcCache(int ix, int iy) {
        int ulx = minx + ix * stepx;
        int uly = miny + iy * stepy;
        double mix = ulx * NodeObject2.DIVIDER;
        double max = (ulx + stepx - 1) * NodeObject2.DIVIDER;
        double miy = uly * NodeObject2.DIVIDER;
        double may = (uly + stepy - 1) * NodeObject2.DIVIDER;
        com.vividsolutions.jts.geom.Polygon p = GEOM.createPolygon(new Coordinate[] { coord(mix, miy),
                coord(mix, may), coord(max, may), coord(max, miy), coord(mix, miy), });
        Geometry intersection = poly.geom.intersection(p);
        if (intersection.isEmpty()) {
            return GEO_EMPTY;
        } else if (intersection.equalsExact(p)) {
            return GEO_FULL;
        } else {
            return intersection;// .union(intersection.buffer(0.001));
        }
    }

    public boolean contains(BaseObject2 obj) {
        if (obj instanceof NodeObject2) {
            return containsNode((NodeObject2) obj);
        } else if (obj instanceof WayObject2) {
            return containsWay((WayObject2) obj);
        } else if (obj instanceof RelationObject2) {
            return containsRelation((RelationObject2) obj);
        } else {
            throw new RuntimeException("Unknown object type: " + obj.getCode());
        }
    }

    public boolean containsNode(NodeObject2 node) {
        int x = node.lon;
        int y = node.lat;
        if (x < minx || x >= maxx || y < miny || y >= maxy) {
            return false;
        }
        int ix = (x - minx) / stepx;
        int iy = (y - miny) / stepy;
        Geometry cached = cachedGeo[ix][iy];
        if (cached == null) {
            cached = calcCache(ix, iy);
            cachedGeo[ix][iy] = cached;
        }
        if (cached == GEO_EMPTY) {
            return false;
        } else if (cached == GEO_FULL) {
            return true;
        } else {
            Point p = createPoint(node.getLongitude(), node.getLatitude());
            boolean result = cached.covers(p);
            return result;
        }
    }

    public boolean containsWay(WayObject2 way) {
        for (int i = 0; i < way.nodeIds.length; i++) {
            long nid = way.nodeIds[i];
            NodeObject2 n = storage.getNodeById(nid);
            if (n != null && containsNode(n)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsRelation(RelationObject2 rel) {
        for (int i = 0; i < rel.memberIDs.length; i++) {
            BaseObject2 o = rel.getMemberObject(storage, i);
            if (o != null && contains(o)) {
                return true;
            }
        }
        return false;
    }

    Point createPoint(double longitude, double latitude) {
        return GEOM.createPoint(coord(longitude, latitude));
    }

    Coordinate coord(double longitude, double latitude) {
        Coordinate result = new Coordinate(longitude, latitude);
        GEOM.getPrecisionModel().makePrecise(result);
        return result;
    }
}
