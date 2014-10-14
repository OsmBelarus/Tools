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

import java.io.FileInputStream;
import java.util.Properties;

/**
 * Чытае налады для валідатараў.
 */
public class Env {
    public static Properties env;

    public static void load() {
        load("env.properties");
    }

    public static void load(String file) {
        env = new Properties();
        try (FileInputStream e = new FileInputStream(file)) {
            env.load(e);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String readProperty(String name) {
        if (env == null) {
            load();
        }
        String v = env.getProperty(name);
        if (v == null) {
            throw new RuntimeException("Property '" + name + "' not defined");
        }
        return v.replace("$HOME", System.getProperty("user.home"));
    }
}
