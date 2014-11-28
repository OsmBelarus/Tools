/**************************************************************************
 
Some tools for OSM.

 Copyright (C) 2014 Aleś Bułojčyk <alex73mail@gmail.com>
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

package org.alex73.osm.monitors.export;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.alex73.osm.utils.Env;
import org.alex73.osm.utils.Lat;
import org.alex73.osmemory.IOsmNode;
import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.IOsmRelation;
import org.alex73.osmemory.IOsmWay;
import org.alex73.osmemory.MemoryStorage;
import org.apache.commons.io.FileUtils;

public class ExportOutput {
    final String path;
    final File file;
    final OutputFormatter formatter;

    List<IOsmObject> queue = new ArrayList<>();

    static List<ExportOutput> listExist(MemoryStorage osm) {
        List<ExportOutput> result = new ArrayList<>();
        Collection<File> files = FileUtils.listFiles(new File(Env.readProperty("monitoring.gitdir")),
                new String[] { "txt" }, true);
        String prefix = new File(Env.readProperty("monitoring.gitdir")).getAbsolutePath().replace('\\', '/') + '/';
        for (File f : files) {
            String fn = f.getAbsolutePath().replace('\\', '/');
            if (!fn.startsWith(prefix)) {
                throw new RuntimeException();
            }
            result.add(new ExportOutput(fn.substring(prefix.length()), osm));
        }
        return result;
    }

    public ExportOutput(String path, MemoryStorage osm) {
        this.path = path;
        file = new File(Env.readProperty("monitoring.gitdir") + "/"
                + Lat.unhac(Lat.lat(path, false)).replace(' ', '_'));
        file.getParentFile().mkdirs();
        formatter = new OutputFormatter(osm);
    }

    void out(IOsmObject o) {
        queue.add(o);
    }

    void save(GitClient git) {
        try {
            if (queue.isEmpty()) {
                if (file.exists()) {
                    // файл выдалены
                    file.delete();
                    git.remove(path);
                }
            } else {
                byte[] data = export();
                if (!file.exists() || file.length() != data.length) {
                    // файл не існаваў альбо адрозьніваецца памерам
                    FileUtils.writeByteArrayToFile(file, data);
                    git.add(path);
                } else {
                    // параўноўваем
                    try (FileChannel fc = new RandomAccessFile(file, "r").getChannel()) {
                        MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
                        if (buffer.compareTo(ByteBuffer.wrap(data)) != 0) {
                            FileUtils.writeByteArrayToFile(file, data);
                            git.add(path);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException();
        }
    }

    /**
     * Экспартуем аб'екты
     */
    byte[] export() throws Exception {
        Collections.sort(queue, new Comparator<IOsmObject>() {
            @Override
            public int compare(IOsmObject o1, IOsmObject o2) {
                int r = o1.getType() - o2.getType();
                if (r == 0) {
                    r = Long.compare(o1.getId(), o2.getId());
                }
                return r;
            }
        });
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024 * 1024);
        PrintStream wr = new PrintStream(out, false, "UTF-8");
        for (IOsmObject o : queue) {
            wr.println(formatter.objectName(o));
            String otherNames = formatter.otherNames(o);
            if (otherNames != null) {
                wr.println("  other names: " + otherNames);
            }
            wr.println("  other tags : " + formatter.otherTags(o));
            switch (o.getType()) {
            case IOsmObject.TYPE_NODE:
                IOsmNode n = (IOsmNode) o;
                wr.println("    geometry : " + formatter.getGeometry(n));
                break;
            case IOsmObject.TYPE_WAY:
                IOsmWay w = (IOsmWay) o;
                wr.println("    geometry :" + formatter.getGeometry(w));
                break;
            case IOsmObject.TYPE_RELATION:
                IOsmRelation r = (IOsmRelation) o;
                for (String g : formatter.getGeometry(r)) {
                    wr.println("    geometry : " + g);
                }
                break;
            default:
                throw new RuntimeException();
            }
        }
        return out.toByteArray();
    }
}
