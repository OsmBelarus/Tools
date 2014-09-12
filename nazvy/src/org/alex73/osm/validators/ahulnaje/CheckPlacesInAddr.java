package org.alex73.osm.validators.ahulnaje;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alex73.osm.utils.Env;
import org.alex73.osm.utils.TSV;
import org.alex73.osm.validators.vioski.PadzielOsmNas;
import org.alex73.osm.validators.vulicy2.OsmNamed;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

public class CheckPlacesInAddr {

    static SqlSession db;

    public static void main(String[] args) throws Exception {
        Env.load();

        initDB();

        Map<String, String> adminLevelsBelToRus = new HashMap<>();
        List<OsmNamed> list = db.selectList("osm.admin_levels");
        for (OsmNamed place : list) {
            if (place != null && place.name_be != null && place.name != null) {
                if (place.name_be.endsWith(" раён") || place.name_be.endsWith(" вобласць")) {
                    adminLevelsBelToRus.put(place.name_be, place.name);
                }
            }
        }
        List<PadzielOsmNas> ls = new TSV('\t').readCSV("vioski/padziel.csv", PadzielOsmNas.class);
        for (PadzielOsmNas p : ls) {
            p.osmNameVoblascRu = adminLevelsBelToRus.get(p.osmNameVoblasc);
            p.osmNameRajonRu = adminLevelsBelToRus.get(p.osmNameRajon);
        }
        new TSV('\t').saveCSV("vioski/padziel2.csv", PadzielOsmNas.class, ls);
    }

    static void initDB() throws Exception {
        System.out.println("Load database...");
        String resource = "osm.xml";
        SqlSessionFactory sqlSessionFactory;
        InputStream inputStream = Resources.getResourceAsStream(resource);
        try {
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream, Env.env);
        } finally {
            inputStream.close();
        }
        db = sqlSessionFactory.openSession();
    }
}
