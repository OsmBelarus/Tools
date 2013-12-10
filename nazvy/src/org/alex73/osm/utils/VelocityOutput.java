package org.alex73.osm.utils;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Properties;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

public class VelocityOutput {
    public static void output(String template, Object data, String file) throws Exception {
        VelocityContext context = new VelocityContext();
        context.put("data", data);
        Properties props = new Properties();
        InputStream in = VelocityOutput.class.getResourceAsStream("velocity.properties");
        try {
            props.load(in);
        } finally {
            in.close();
        }
        Velocity.init(props);
        Template t = Velocity.getTemplate(template);
        Writer wr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
        t.merge(context, wr);
        wr.close();
    }
}
