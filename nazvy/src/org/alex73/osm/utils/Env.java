package org.alex73.osm.utils;

import java.io.FileInputStream;
import java.util.Properties;

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
            System.err.println("Property '" + name + "' not defined");
            System.exit(1);
        }
        return v.replace("$HOME", System.getProperty("user.home"));
    }
}
