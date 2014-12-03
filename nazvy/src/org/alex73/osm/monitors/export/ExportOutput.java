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

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.alex73.osm.utils.Env;
import org.alex73.osm.utils.Lat;
import org.alex73.osm.validators.objects.CheckType;
import org.alex73.osmemory.IOsmNode;
import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.IOsmRelation;
import org.alex73.osmemory.IOsmWay;
import org.alex73.osmemory.MemoryStorage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Запісвае зьвесткі ў файл калі яны зьмяніліся.
 */
public class ExportOutput {
    public static final String CHECKSUM_ALGORITHM = "MD5";

    final String typ, rehijon;
    final File file1, file2;
    String path1, path2;
    String oldchecksum;

    List<IOsmObject> queue = new ArrayList<>();

    /**
     * Стварае ExportOutput'ы для ўсіх магчымых камбінацый border+type.
     */
    public static List<ExportOutput> list(List<CheckType> types, Borders borders, Set<String> unusedFiles)
            throws Exception {
        Collection<File> files = FileUtils.listFiles(new File(Env.readProperty("monitoring.gitdir")),
                new String[] { "txt" }, true);

        List<ExportOutput> result = new ArrayList<>();
        for (CheckType t : types) {
            for (Borders.Border b : borders.kraina) {
                ExportOutput o = new ExportOutput(t.getType().getFile(), b.name);
                if (o.file1.exists() || o.file2.exists()) {
                    result.add(o);
                    files.remove(o.file1);
                    files.remove(o.file2);
                }
            }
            for (Borders.Border b : borders.voblasci) {
                ExportOutput o = new ExportOutput(t.getType().getFile(), b.name);
                if (o.file1.exists() || o.file2.exists()) {
                    result.add(o);
                    files.remove(o.file1);
                    files.remove(o.file2);
                }
            }
            for (Borders.Border b : borders.rajony) {
                ExportOutput o = new ExportOutput(t.getType().getFile(), b.name);
                if (o.file1.exists() || o.file2.exists()) {
                    result.add(o);
                    files.remove(o.file1);
                    files.remove(o.file2);
                }
            }
            for (Borders.Border b : borders.miesty) {
                ExportOutput o = new ExportOutput(t.getType().getFile(), b.name);
                if (o.file1.exists() || o.file2.exists()) {
                    result.add(o);
                    files.remove(o.file1);
                    files.remove(o.file2);
                }
            }
        }
        String prefix = new File(Env.readProperty("monitoring.gitdir")).getAbsolutePath().replace('\\', '/') + '/';
        for (File f : files) {
            String fn = f.getAbsolutePath().replace('\\', '/');
            if (!fn.startsWith(prefix)) {
                throw new RuntimeException("Wrong file: " + f);
            }
            unusedFiles.add(fn.substring(prefix.length()));
        }

        return result;
    }

    public ExportOutput(String typ, String rehijon) throws Exception {
        if (rehijon.startsWith("/")) {
            rehijon = rehijon.substring(1);
        }
        if (rehijon.endsWith("/")) {
            rehijon = rehijon.substring(0, rehijon.length() - 1);
        }
        this.typ = typ;
        this.rehijon = rehijon;
        path1 = rehijon.isEmpty() ? "What/" + typ + ".txt" : "What/" + typ + "/" + rehijon + ".txt";
        path2 = rehijon.isEmpty() ? "Where/" + typ + ".txt" : "Where/" + rehijon + "/" + typ + ".txt";
        path1 = Lat.unhac(Lat.lat(path1, false)).replace(' ', '_');
        path2 = Lat.unhac(Lat.lat(path2, false)).replace(' ', '_');
        file1 = new File(Env.readProperty("monitoring.gitdir") + "/" + path1);
        file1.getParentFile().mkdirs();
        file2 = new File(Env.readProperty("monitoring.gitdir") + "/" + path2);
        file2.getParentFile().mkdirs();

        String ck1 = readChecksum(file1);
        String ck2 = readChecksum(file2);
        if (StringUtils.equals(ck1, ck2)) {
            oldchecksum = ck1;
        }
    }

    public String key() {
        return typ + "|" + rehijon;
    }

    public static String key(String typ, String rehijon) {
        if (rehijon.startsWith("/")) {
            rehijon = rehijon.substring(1);
        }
        if (rehijon.endsWith("/")) {
            rehijon = rehijon.substring(0, rehijon.length() - 1);
        }
        return typ + "|" + rehijon;
    }

    /**
     * Дадае 1 аб'ект.
     */
    public void out(IOsmObject o) {
        queue.add(o);
    }

    public void clear() {
        queue.clear();
    }

    /**
     * Выдаляе аб'ект. Для абнаўленьня па changeset.
     */
    void forgetInQueue(int type, long id) {
        int low = 0;
        int high = queue.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midvaluetype = queue.get(mid).getType();
            long midvalueid = queue.get(mid).getId();

            if (midvaluetype < type) {
                low = mid + 1;
            } else if (midvaluetype > type) {
                high = mid - 1;
            } else if (midvalueid < id) {
                low = mid + 1;
            } else if (midvalueid > id) {
                high = mid - 1;
            } else {
                queue.remove(mid);
                break;
            }
        }
    }

    public void forgetNode(long id) {
        forgetInQueue(IOsmObject.TYPE_NODE, id);
    }

    public void forgetWay(long id) {
        forgetInQueue(IOsmObject.TYPE_WAY, id);
    }

    public void forgetRelation(long id) {
        forgetInQueue(IOsmObject.TYPE_RELATION, id);
    }

    /**
     * Захоўвае ў файл толькі калі нешта зьмянілася.
     */
    void save(GitClient git, MemoryStorage osm) {
        try {
            if (queue.isEmpty()) {
                if (file1.exists()) {
                    // файл выдалены
                    file1.delete();
                    git.remove(path1);
                }
                if (file2.exists()) {
                    // файл выдалены
                    file2.delete();
                    git.remove(path2);
                }
                oldchecksum = null;
            } else {
                byte[] data = export(osm);
                String ck = calcChecksum(data);
                // параўноўваем
                if (!ck.equals(oldchecksum)) {
                    FileUtils.writeByteArrayToFile(file1, data);
                    git.add(path1);
                    FileUtils.writeByteArrayToFile(file2, data);
                    git.add(path2);
                    oldchecksum = ck;
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException();
        }
    }

    public void finishUpdate() {
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
        // выдаляем аднолькавыя якія могуць быць пасьля changeset
        for (int i = 1; i < queue.size(); i++) {
            IOsmObject prev = queue.get(i - 1);
            IOsmObject curr = queue.get(i);
            if (prev.getType() == curr.getType() && prev.getId() == curr.getId()) {
                queue.remove(i);
                i--;
            }
        }
    }

    private static final Charset OUT_CHARSET = Charset.forName("UTF-8");

    /**
     * Экспартуем аб'екты
     */
    byte[] export(MemoryStorage osm) throws Exception {
        OutputFormatter formatter = new OutputFormatter(osm);

        for (IOsmObject o : queue) {
            formatter.objectName(o);
            formatter.newLine();
            formatter.otherNames(o);
            formatter.otherTags(o);
            switch (o.getType()) {
            case IOsmObject.TYPE_NODE:
                formatter.getGeometry((IOsmNode) o);
                break;
            case IOsmObject.TYPE_WAY:
                formatter.getGeometry((IOsmWay) o);
                break;
            case IOsmObject.TYPE_RELATION:
                formatter.getGeometry((IOsmRelation) o);
                break;
            default:
                throw new RuntimeException();
            }
        }
        return formatter.getOutput().getBytes(OUT_CHARSET);
    }

    public static String readChecksum(File file) throws Exception {
        if (!file.exists()) {
            return null;
        }
        MessageDigest md = MessageDigest.getInstance(CHECKSUM_ALGORITHM);
        try (RandomAccessFile ra = new RandomAccessFile(file, "r")) {
            try (FileChannel fc = ra.getChannel()) {
                MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
                md.update(buffer);
            }
        }
        return str(md);
    }

    public static String calcChecksum(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance(CHECKSUM_ALGORITHM);
        md.update(data);
        return str(md);
    }

    private static String str(MessageDigest md) {
        byte[] digest = md.digest();
        StringBuilder o = new StringBuilder(40);
        for (int i = 0; i < digest.length; i++) {
            o.append(Integer.toHexString((0xFF & digest[i])));
        }
        return o.toString();
    }
}
