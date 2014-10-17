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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

/**
 * Запісвае зьвесткі па шаблёну velocity.
 */
public class VelocityOutput {
    public static void output(String template, String file, Object... args) throws Exception {
        VelocityContext context = new VelocityContext();
        for (int i = 0; i < args.length; i += 2) {
            context.put((String) args[i], args[i + 1]);
        }

        SimpleDateFormat format = new SimpleDateFormat("dd MMM YYYY HH:mm:ss Z");
        format.setTimeZone(TimeZone.getTimeZone("Europe/Minsk"));
        context.put("currentDateTime", format.format(new Date()));

        GregorianCalendar c = (GregorianCalendar) Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        XMLGregorianCalendar xmlc = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
        context.put("currentDateTimeXML", xmlc.toXMLFormat());

        context.put("OSM", OSM.class);

        Properties props = new Properties();
        InputStream in = VelocityOutput.class.getResourceAsStream("velocity.properties");
        try {
            props.load(in);
        } finally {
            in.close();
        }
        Velocity.init(props);
        Template t = Velocity.getTemplate(template);
        new File(file).getParentFile().mkdirs();
        Writer wr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
        t.merge(context, wr);
        wr.close();
    }
}
