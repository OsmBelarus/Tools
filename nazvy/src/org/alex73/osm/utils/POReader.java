/**************************************************************************
 PO file reader

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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class POReader extends HashMap<String, String> {
    static final Pattern RE_RU = Pattern.compile("msgid \"(.*)\"");
    static final Pattern RE_BE = Pattern.compile("msgstr \"(.*)\"");

    public POReader(String filename) throws Exception {
        BufferedReader rd = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
        String s;
        String ru = null;
        while ((s = rd.readLine()) != null) {
            Matcher m;
            if ((m = RE_RU.matcher(s)).matches()) {
                ru = m.group(1);
            } else if ((m = RE_BE.matcher(s)).matches()) {
                put(ru, m.group(1));
                ru = null;
            }
        }
    }
}
