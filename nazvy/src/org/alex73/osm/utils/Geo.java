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

package org.alex73.osm.utils;

import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.alex73.osmemory.IOsmNode;
import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.IOsmRelation;
import org.alex73.osmemory.IOsmWay;
import org.alex73.osmemory.MemoryStorage;

/**
 * Некаторыя геаграфічныя мэтады.
 * 
 * @deprecated
 */
@Deprecated
public class Geo {
    static final DecimalFormat N2 = new DecimalFormat("00");
    static final StringBuilder o = new StringBuilder();
    public static final Area BELARUS = box2area(22.55, 50.716667, 32.708056, 56.066667);
    public static double LATITUDE_SIZE = 111.321;
    public static double LONGTITUDE_BELARUS_SIZE = 67.138;

    public synchronized static String coord2str(double lat, double lon) {
        o.setLength(0);
        coord(lat);
        o.append(' ');
        coord(lon);
        return o.toString();
    }

    static private void coord(double c) {
        long ci = Math.round(c * 3600);
        o.append(N2.format(ci / 3600)).append('°');
        ci = ci % 3600;
        o.append(N2.format(ci / 60)).append('′');
        o.append(N2.format(ci % 60)).append('″');
    }

    public static Point2D node2point(MemoryStorage storage, long nodeId) {
        IOsmNode node = storage.getNodeById(nodeId);
        if (node == null) {
            throw new IndexOutOfBoundsException("There is no node #" + nodeId);
        }
        return new Point2D.Double(node.getLongitude(), node.getLatitude());
    }

    public static Area box2area(double min_lon, double min_lat, double max_lon, double max_lat) {
        return new Area(new Rectangle2D.Double(min_lon, min_lat, max_lon - min_lon, max_lat - min_lat));
    }

    public static Path2D way2path(MemoryStorage storage, long wayId) {
        IOsmWay way = storage.getWayById(wayId);
        if (way == null) {
            throw new IndexOutOfBoundsException("There is no way #" + wayId);
        }
        Path2D.Double result = new Path2D.Double(Path2D.WIND_NON_ZERO, way.getNodeIds().length);
        for (int i = 0; i < way.getNodeIds().length; i++) {
            long nodeId = way.getNodeIds()[i];
            IOsmNode node = storage.getNodeById(nodeId);
            if (node == null) {
                throw new IndexOutOfBoundsException("There is no node #" + nodeId);
            }
            if (i == 0) {
                result.moveTo(node.getLongitude(), node.getLatitude());
            } else {
                result.lineTo(node.getLongitude(), node.getLatitude());
            }
        }
        return result;
    }

    public static Area way2area(MemoryStorage storage, long wayId) {
        return new Area(way2path(storage, wayId));
    }

    public static Area rel2area(MemoryStorage storage, long relId) {
        IOsmRelation rel = storage.getRelationById(relId);
        if (rel == null) {
            throw new IndexOutOfBoundsException("There is no relation #" + relId);
        }

        List<Path2D> outerWays = new ArrayList<>();
        List<Path2D> innerWays = new ArrayList<>();
        Point2D center = null;
        Area borderArea = null;
        for (int i = 0; i < rel.getMembersCount(); i++) {
            IOsmObject m = rel.getMemberObject(storage, i);
            String role = rel.getMemberRole(storage, i);
            switch (role) {
            case "outer":
            case "":// TODO: remove
                if (rel.getMemberType(i) == IOsmObject.TYPE_WAY) {
                    try {
                        outerWays.add(way2path(storage, m.getId()));
                    } catch (IndexOutOfBoundsException ex) {
                    }
                }
                break;
            case "inner":
                // TODO
                if (rel.getMemberType(i) != IOsmObject.TYPE_WAY) {
                    throw new RuntimeException();
                }
                try {
                    innerWays.add(way2path(storage, m.getId()));
                } catch (IndexOutOfBoundsException ex) {
                }
                break;
            case "label":
            case "admin_centre":
                if (rel.getMemberType(i) == IOsmObject.TYPE_NODE) {
                    try {
                        center = node2point(storage, m.getId());
                    } catch (IndexOutOfBoundsException ex) {
                    }
                }
                break;
            case "is_in":
            case "subarea":
                break;
            case "border":
                if (rel.getMemberType(i) == IOsmObject.TYPE_WAY) {
                    // TODO change: should be outer
                    outerWays.add(way2path(storage, m.getId()));
                } else if (rel.getMemberType(i) == IOsmObject.TYPE_RELATION) {
                    if (borderArea != null) {
                        throw new RuntimeException("Second border for rel#" + relId);
                    }
                    try {
                        borderArea = rel2area(storage, m.getId());
                    } catch (IndexOutOfBoundsException ex) {
                    }
                } else {
                    throw new RuntimeException("Wrong border for rel#" + relId);
                }
                break;
            default:
                throw new RuntimeException("Unknown role: " + role + " in rel#" + relId);
            }
        }
        if (borderArea != null) {
            if (!outerWays.isEmpty() || !innerWays.isEmpty()) {
                throw new RuntimeException("Second border for rel#" + relId);
            }
            return borderArea;
        } else if (!outerWays.isEmpty()) {
            if (borderArea != null) {
                throw new RuntimeException("Second border for rel#" + relId);
            }

            Area result = new Area();
            while (!outerWays.isEmpty()) {
                result.add(new Area(extractClosedPath(outerWays, relId)));
            }
            while (!innerWays.isEmpty()) {
                result.add(new Area(extractClosedPath(innerWays, relId)));
            }
            return result;
        } else if (center != null) {
            Path2D.Double result = new Path2D.Double(Path2D.WIND_NON_ZERO);
            result.moveTo(center.getX(), center.getY());
            return new Area(result);
        } else {
            return null;
        }
    }

    static Path2D extractClosedPath(List<Path2D> pathes, long relId) {
        Path2D result = new Path2D.Double(pathes.remove(0));
        while (true) {
            Path2D next = extractConnectedToEnd(result, pathes);
            if (next == null) {
                break;
            }
            result.append(next, true);
        }
        Point2D.Double firstPoint = pathFirst(result); //
        Point2D.Double endPoint = pathLast(result);
        if (!firstPoint.equals(endPoint)) {
            // System.out.println("Non-closed for r#" + relId + ": " +
            // firstPoint + "/" + endPoint);
        }
        return result;
    }

    static Path2D extractConnectedToEnd(Path2D path, List<Path2D> pathes) {
        Point2D.Double endPoint = pathLast(path);

        for (int i = 0; i < pathes.size(); i++) {
            Point2D.Double pf = pathFirst(pathes.get(i));
            if (endPoint.equals(pf)) {
                return pathes.remove(i);
            }
            Point2D.Double pe = pathLast(pathes.get(i));
            if (endPoint.equals(pe)) {
                return reversePath(pathes.remove(i));
            }
        }
        return null;
    }

    static Point2D.Double pathFirst(Path2D path) {
        PathIterator it = path.getPathIterator(null);
        double[] c = new double[2];
        it.currentSegment(c);
        return new Point2D.Double(c[0], c[1]);
    }

    static Point2D.Double pathLast(Path2D path) {
        PathIterator it = path.getPathIterator(null);
        double[] c = new double[2];
        while (!it.isDone()) {
            it.currentSegment(c);
            it.next();
        }
        return new Point2D.Double(c[0], c[1]);
    }

    static Path2D reversePath(Path2D path) {
        PathIterator it = path.getPathIterator(null);
        List<Point2D> points = new ArrayList<>();
        double[] c = new double[2];
        while (!it.isDone()) {
            it.currentSegment(c);
            points.add(new Point2D.Double(c[0], c[1]));
            it.next();
        }
        Collections.reverse(points);

        Path2D.Double result = new Path2D.Double(Path2D.WIND_NON_ZERO);
        for (int i = 0; i < points.size(); i++) {
            Point2D p = points.get(i);
            if (i == 0) {
                result.moveTo(p.getX(), p.getY());
            } else {
                result.lineTo(p.getX(), p.getY());
            }
        }
        return result;
    }

    public static boolean isInside(Area area, Point2D p) {
        if (p == null) {
            return false;
        }
        if (!area.getBounds2D().contains(p)) {
            return false;
        }
        return area.contains(p);
    }

    public static boolean isInside(Area area, Path2D path) {
        if (path == null) {
            return false;
        }
        if (!area.intersects(path.getBounds2D())) {
            return false;
        }
        PathIterator it = path.getPathIterator(null);
        double[] c = new double[2];
        while (!it.isDone()) {
            it.currentSegment(c);
            if (area.contains(c[0], c[1])) {
                return true;
            }
            it.next();
        }
        return false;
    }

    public static boolean isInside(Area area, Area a) {
        if (a == null) {
            return false;
        }
        if (!area.intersects(a.getBounds2D())) {
            return false;
        }
        a.intersect(area);
        return !a.isEmpty();
    }
}
