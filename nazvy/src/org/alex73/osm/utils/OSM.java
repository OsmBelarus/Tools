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

public class OSM {
    static String ICON_NODE = "<img src=\"http://wiki.openstreetmap.org/w/images/b/b5/Mf_node.png\"/>";
    static String ICON_WAY = "<img src=\"http://wiki.openstreetmap.org/w/images/6/6a/Mf_way.png\"/>";
    static String ICON_REL = "<img src=\"http://wiki.openstreetmap.org/w/images/5/59/Relation.png\"/>";
    static String ICON_JOSM = "<img src=\"http://wiki.openstreetmap.org/w/images/6/6a/JOSM-Icon_Daten_download.jpg\"/>";

    public static String browse(String code) {
        return "<a href='http://www.openstreetmap.org/"
                + code.replace("n", "node/").replace("w", "way/").replace("r", "relation/") + "'>" + code
                + "</a>";
    }

    public static String histText(String code) {
        return "<a target='_blank' href='http://www.openstreetmap.org/"
                + code.replace("n", "node/").replace("w", "way/").replace("r", "relation/") + "/history'>"
                + code + "</a>";
    }

    public static String josmIcon(String code) {
        return "<a href='#' onclick='javascript: send(\"load_object?objects=" + code + "\")'>" + ICON_JOSM
                + "</a>";
    }

    public static String histIcon(String code) {
        String icon;
        if (code.startsWith("n")) {
            icon = ICON_NODE;
        } else if (code.startsWith("w")) {
            icon = ICON_WAY;
        } else if (code.startsWith("r")) {
            icon = ICON_REL;
        } else {
            icon = code;
        }
        String r = "<a target='_blank' href='http://www.openstreetmap.org/"
                + code.replace("n", "node/").replace("w", "way/").replace("r", "relation/") + "/history'>";
        r += icon + "</a>";
        return r;
    }
}
