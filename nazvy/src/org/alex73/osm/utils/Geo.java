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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.alex73.osm.data.BaseObject;
import org.alex73.osm.data.MemoryStorage;
import org.alex73.osm.data.NodeObject;
import org.alex73.osm.data.RelationObject;
import org.alex73.osm.data.WayObject;

public class Geo {

    static final DecimalFormat N2 = new DecimalFormat("00");
    static final StringBuilder o = new StringBuilder();

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
        NodeObject node = storage.getNodeById(nodeId);
        if (node == null) {
            throw new IndexOutOfBoundsException("There is no node #" + nodeId);
        }
        return new Point2D.Double(node.lon, node.lat);
    }

    public static Path2D way2path(MemoryStorage storage, long wayId) {
        WayObject way = storage.getWayById(wayId);
        if (way == null) {
            throw new IndexOutOfBoundsException("There is no way #" + wayId);
        }
        Path2D.Double result = new Path2D.Double(Path2D.WIND_NON_ZERO, way.nodeIds.length);
        for (int i = 0; i < way.nodeIds.length; i++) {
            long nodeId = way.nodeIds[i];
            NodeObject node = storage.getNodeById(nodeId);
            if (node == null) {
                throw new IndexOutOfBoundsException("There is no node #" + nodeId);
            }
            if (i == 0) {
                result.moveTo(node.lon, node.lat);
            } else {
                result.lineTo(node.lon, node.lat);
            }
        }
        return result;
    }

    public static Area way2area(MemoryStorage storage, long wayId) {
        return new Area(way2path(storage, wayId));
    }

    public static Area rel2area(MemoryStorage storage, long relId) {
        RelationObject rel = storage.getRelationById(relId);
        if (rel == null) {
            throw new IndexOutOfBoundsException("There is no relation #" + relId);
        }

        List<Path2D> outerWays = new ArrayList<>();
        List<Path2D> innerWays = new ArrayList<>();
        Point2D center = null;
        Area borderArea = null;
        for (int i = 0; i < rel.members.length; i++) {
            RelationObject.Member m = rel.members[i];
            String role = m.role;
            switch (role) {
            case "outer":
            case "":// TODO: remove
                if (m.type == BaseObject.TYPE.WAY) {
                    try {
                        outerWays.add(way2path(storage, m.id));
                    } catch (IndexOutOfBoundsException ex) {
                    }
                }
                break;
            case "inner":
                // TODO
                if (m.type != BaseObject.TYPE.WAY) {
                    throw new RuntimeException();
                }
                try {
                    innerWays.add(way2path(storage, m.id));
                } catch (IndexOutOfBoundsException ex) {
                }
                break;
            case "label":
            case "admin_centre":
                if (m.type == BaseObject.TYPE.NODE) {
                    try {
                        center = node2point(storage, m.id);
                    } catch (IndexOutOfBoundsException ex) {
                    }
                }
                break;
            case "is_in":
            case "subarea":
                break;
            case "border":
                if (m.type == BaseObject.TYPE.WAY) {
                    // TODO change: should be outer
                    outerWays.add(way2path(storage, m.id));
                } else if (m.type == BaseObject.TYPE.RELATION) {
                    if (borderArea != null) {
                        throw new RuntimeException("Second border for rel#" + relId);
                    }
                    try {
                        borderArea = rel2area(storage, m.id);
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
