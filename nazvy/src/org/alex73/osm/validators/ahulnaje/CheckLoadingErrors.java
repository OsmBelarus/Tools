package org.alex73.osm.validators.ahulnaje;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.alex73.osm.utils.Env;
import org.alex73.osm.utils.OSM;
import org.alex73.osm.utils.VelocityOutput;
import org.alex73.osm.validators.vulicy2.VialikiDom;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

public class CheckLoadingErrors {
    static SqlSession db;
    public static Map<String, List<String>> pamylki = new TreeMap<>();
    public static List<Zauvaha> vialikija_damy = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        Env.load();

        String out = Env.readProperty("out.dir") + "/pamylki.html";
        int count = read();
        VelocityOutput.output("org/alex73/osm/validators/ahulnaje/pamylki.velocity", out, "pamylki", pamylki,
                "vialikija_damy", vialikija_damy, "count", count);
    }

    static int read() throws Exception {
        String resource = "osm.xml";
        SqlSessionFactory sqlSessionFactory;
        InputStream inputStream = Resources.getResourceAsStream(resource);
        try {
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream, Env.env);
        } finally {
            inputStream.close();
        }
        db = sqlSessionFactory.openSession();

        List<Error> errors = db.selectList("getAllErrors");
        Collections.sort(errors, new Comparator<Error>() {
            public int compare(Error e1, Error e2) {
                int c = t(e1.type) - t(e2.type);
                if (c == 0) {
                    c = Long.compare(e1.id, e2.id);
                }
                return c;
            }

            int t(String type) {
                switch (type) {
                case "n":
                    return 1;
                case "w":
                    return 2;
                case "r":
                    return 3;
                }
                return 0;
            }
        });

        for (Error err : errors) {
            List<String> p = pamylki.get(err.error);
            if (p == null) {
                p = new ArrayList<>();
                pamylki.put(err.error, p);
            }
            p.add(OSM.histIcon(err.getCode()));
        }

        List<VialikiDom> vdb = db.selectList("big_buildings");
        for (VialikiDom d : vdb) {
            Zauvaha z = new Zauvaha();
            z.text = "Будынак працягласьцю больш за " + ((int) d.maxsize) + " мэтраў";
            z.osmLink = OSM.histIcon("w" + d.id);
            vialikija_damy.add(z);
        }

        return errors.size();
    }

    public static class Zauvaha {
        public String text;
        public String osmLink;
    }
}
