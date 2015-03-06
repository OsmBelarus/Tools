package org.alex73.osm.validators.common;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;

public class JS {
    private final File file;

    public JS(String file) throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("dd MMM YYYY HH:mm:ss Z");
        format.setTimeZone(TimeZone.getTimeZone("Europe/Minsk"));

        String s = "data = new Array();\n";
        s += "data.currentTimestamp = '" + format.format(new Date()) + "';\n";
        this.file = new File(file);
        FileUtils.writeStringToFile(this.file, s, "UTF-8", false);
    }

    public void add(String var, Object obj) throws Exception {
        String s = "data." + var + " = " + new Gson().toJson(obj) + ";\n";
        FileUtils.writeStringToFile(this.file, s, "UTF-8", true);
    }
}
