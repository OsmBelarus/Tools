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

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.alex73.osm.utils.Belarus;
import org.alex73.osm.utils.Env;
import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.XMLDriver;
import org.alex73.osmemory.XMLReader;

import osm.xmldatatypes.Changeset;
import osm.xmldatatypes.Tag;

/**
 * Экспартуе changesets паміж дзьвюма файламі o5m і потым апошні файл o5m цалкам.
 */
public class Export2Initial {
    static Borders borders;
    static GitClient git;
    static String borderFile;

    public static void main(String[] args) throws Exception {
        Locale.setDefault(new Locale("en", "US"));
        git = new GitClient(Env.readProperty("monitoring.gitdir"));
        borderFile = Env.readProperty("monitoring.gitdir") + "/miezy.properties";

        if (args.length == 1) {
            justDump(args[0]);
        } else if (args.length == 2 && args[1].equals("-")) {
            dumpBetween(args[0], null);
        } else if (args.length == 2) {
            dumpBetween(args[0], args[1]);
        } else {
            System.err.println("Export <fromfile.o5m> <tofile.o5m>");
            System.exit(1);
        }
    }

    /**
     * Экспартуе толькі o5m файл.
     */
    static void justDump(String file) throws Exception {
        System.out.println("Захоўваем " + file + " у git");
        // чытаем timestamp першага файлаў
        DataFileStatus f1status = new DataFileStatus(file);
        f1status.dump();

        git.reset();

        // чытаем першы файл
        Belarus country = new Belarus(file);
        borders = new Borders(borderFile, country);
        borders.update(country);
        borders.save(borderFile);
        git.add("miezy.properties");

        Set<String> unusedFiles = new HashSet<>();
        ExportObjectsByType export = new ExportObjectsByType(country, borders, unusedFiles);
        removeFiles(unusedFiles);
        System.out.println(new Date() + " Export ...");
        export.collectData();
        export.saveExport(git);
        git.commit("OSM Robot", "robot", "OSM dump : " + new Date(f1status.initialDate).toGMTString());
    }

    /**
     * Экспартуе changeset'ы паміж двума o5m файламі.
     */
    static void dumpBetween(String file1, String file2) throws Exception {
        if (file2 != null) {
            System.out.println("Захоўваем усе changeset'ы пасьля " + file1 + " да " + file2 + " у git");
        } else {
            System.out.println("Захоўваем усе changeset'ы пасьля " + file1 + " у git");
        }

        // чытаем timestamp першага файлаў
        DataFileStatus f1status = new DataFileStatus(file1);
        f1status.dump();
        DataFileStatus f2status;
        if (file2 != null) {
            f2status = new DataFileStatus(file2);
            f2status.dump();
        } else {
            f2status = null;
        }

        // чытаем першы файл
        Belarus country = new Belarus(file1);
        borders = new Borders(borderFile, country);

        git.reset();
        // атрымліваем сьпіс changesets паміж файламі
        List<Changeset> changesets = ReadChangesets.retrieve(f1status.knownChangesets,
                f2status != null ? f2status.knownChangesets : null);
        Set<String> unusedFiles = new HashSet<>();
        ExportObjectsByType export = new ExportObjectsByType(country, borders, unusedFiles);
        removeFiles(unusedFiles);
        export.collectData();

        changesets.forEach(ch -> System.out.println(ch.getId() + " " + ch.getUser() + " "
                + ch.getNumChanges()));
        int c = 0;
        for (Changeset ch : changesets) {
            c++;
            // дадаем changeset і экспартуем у git
            boolean needExport = apply(country, ReadChangesets.download(ch), export);
            export.fixOutput();
            export.processQueue();
            if (!needExport) {
                System.out.println("Skip #" + ch.getId() + " because it outside Belarus");
                continue;
            }
            boolean gitContains = git.hasCommit("#" + ch.getId() + " ");
            if (gitContains) {
                System.out.println("Skip #" + ch.getId() + " because it already committed");
                continue;
            }

            System.out.println(new Date() + " Export #" + ch.getId() + " [" + c + "/" + changesets.size()
                    + "]");
            export.saveExport(git);
            git.commit(ch.getUser(), Long.toString(ch.getUid()), changesetDescriptionForCommit(ch));
        }
    }

    /**
     * Выдаляе непатрэбныя файлы з git. Такое можа быць калі зьмяніліся назвы файлаў у тыпах.
     */
    static void removeFiles(Set<String> unusedFiles) throws Exception {
        unusedFiles.forEach(f -> git.remove(f));
    }

    /**
     * Стварае апісаньне каміта для changeset.
     */
    static String changesetDescriptionForCommit(Changeset ch) {
        String o = "#" + ch.getId() + " [" + ch.getNumChanges() + "]";
        for (Tag t : ch.getTag()) {
            if ("comment".equals(t.getK())) {
                o += ": " + t.getV();
            }
        }
        o += '\n';
        for (Tag t : ch.getTag()) {
            if (!"comment".equals(t.getK())) {
                o += "  " + t.getK() + ": " + t.getV() + "\n";
            }
        }
        o += "at " + ch.getClosedAt().toXMLFormat();
        return o;
    }

    static boolean inside;

    /**
     * Чытае changeset, высьвятляе ці ён па-за межамі Беларусі, і перадае зьвесткі ў ExportObjectByType.
     */
    static boolean apply(Belarus country, byte[] changes, XMLDriver.IApplyChangeCallback cb) throws Exception {
        inside = false;
        XMLReader reader = new XMLReader(country, Belarus.MIN_LAT, Belarus.MAX_LAT, Belarus.MIN_LON,
                Belarus.MAX_LON);
        new XMLDriver(reader).applyOsmChange(new ByteArrayInputStream(changes),
                new XMLDriver.IApplyChangeCallback() {

                    @Override
                    public void beforeUpdateNode(long id) {
                        if (!inside) {
                            IOsmObject obj = country.getNodeById(id);
                            if (obj != null) {
                                inside = country.contains(obj);
                            }
                        }
                        cb.beforeUpdateNode(id);
                    }

                    @Override
                    public void beforeUpdateWay(long id) {
                        if (!inside) {
                            IOsmObject obj = country.getWayById(id);
                            if (obj != null) {
                                inside = country.contains(obj);
                            }
                        }
                        cb.beforeUpdateWay(id);
                    }

                    @Override
                    public void beforeUpdateRelation(long id) {
                        if (!inside) {
                            IOsmObject obj = country.getRelationById(id);
                            if (obj != null) {
                                inside = country.contains(obj);
                            }
                        }
                        cb.beforeUpdateRelation(id);
                    }

                    @Override
                    public void afterUpdateNode(long id) {
                        if (!inside) {
                            IOsmObject obj = country.getNodeById(id);
                            if (obj != null) {
                                inside = country.contains(obj);
                            }
                        }
                        cb.afterUpdateNode(id);
                    }

                    @Override
                    public void afterUpdateWay(long id) {
                        if (!inside) {
                            IOsmObject obj = country.getWayById(id);
                            if (obj != null) {
                                inside = country.contains(obj);
                            }
                        }
                        cb.afterUpdateWay(id);
                    }

                    @Override
                    public void afterUpdateRelation(long id) {
                        if (!inside) {
                            IOsmObject obj = country.getRelationById(id);
                            if (obj != null) {
                                inside = country.contains(obj);
                            }
                        }
                        cb.afterUpdateRelation(id);
                    }
                });
        return inside;
    }
}
