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

package org.alex73.osm.monitors.export;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.alex73.osm.utils.Belarus;
import org.alex73.osm.utils.Env;
import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.O5MDriver;
import org.alex73.osmemory.O5MReader;
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

    public static void main(String[] args) throws Exception {
        Locale.setDefault(new Locale("en", "US"));
        git = new GitClient(Env.readProperty("monitoring.gitdir"));

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

    static void justDump(String file) throws Exception {
        System.out.println("Захоўваем " + file + " у git");
        // чытаем timestamp першага файлаў
        DataFileStatus f1status = getFileStatus(file);
        f1status.dump();

        // чытаем першы файл
        Belarus country = new Belarus(file);
        borders = new Borders(country);
        borders.update(country);
        borders.save();

        System.out.println(new Date() + " Export ...");
        git.reset();
        new ExportObjectsByType().export(country, borders, git);
        git.commit("OSM Robot", "robot", "OSM dump : " + new Date(f1status.initialDate).toGMTString());
    }

    static void dumpAfter(String file) throws Exception {
        System.out.println("Захоўваем усе changeset'ы пасьля " + file + " у git");

        // чытаем timestamp першага файлаў
        DataFileStatus f1status = getFileStatus(file);
        f1status.dump();

        // чытаем першы файл
        Belarus country = new Belarus(file);
        borders = new Borders(country);
        borders.update(country);
        borders.save();

        // атрымліваем сьпіс changesets паміж файламі
        List<Changeset> changesets = ReadChangesets.retrieve(f1status.knownChangesets, null);
        dumpChangesets(country, changesets);
    }

    static void dumpBetween(String file1, String file2) throws Exception {
        if (file2 != null) {
            System.out.println("Захоўваем усе changeset'ы пасьля " + file1 + " да " + file2 + " у git");
        } else {
            System.out.println("Захоўваем усе changeset'ы пасьля " + file1 + " у git");
        }

        // чытаем timestamp першага файлаў
        DataFileStatus f1status = getFileStatus(file1);
        f1status.dump();
        DataFileStatus f2status;
        if (file2 != null) {
            f2status = getFileStatus(file2);
            f2status.dump();
        } else {
            f2status = null;
        }

        // чытаем першы файл
        Belarus country = new Belarus(file1);
        borders = new Borders(country);
        borders.update(country);
        borders.save();

        // атрымліваем сьпіс changesets паміж файламі
        List<Changeset> changesets = ReadChangesets.retrieve(f1status.knownChangesets,
                f2status != null ? f2status.knownChangesets : null);
        dumpChangesets(country, changesets);
    }

    static void dumpChangesets(Belarus country, List<Changeset> changesets) throws Exception {
        git.reset();
        ExportObjectsByType export = new ExportObjectsByType();

        changesets.forEach(ch -> System.out.println(ch.getId() + " " + ch.getUser() + " "
                + ch.getNumChanges()));
        for (Changeset ch : changesets) {
            // дадаем changeset і экспартуем у git
            boolean needExport = apply(country, ReadChangesets.download(ch));
            if (!needExport) {
                System.out.println("Skip #" + ch.getId() + " because it outside Belarus");
                continue;
            }
            String chMark = "#" + ch.getId() + " ";
            boolean gitContains = git.hasCommit(chMark);
            if (gitContains) {
                System.out.println("Skip #" + ch.getId() + " because it already committed");
                continue;
            }

            System.out.println(new Date() + " Export #" + ch.getId());
            export.export(country, borders, git);
            git.commit(ch.getUser(), Long.toString(ch.getUid()), changesetDescriptionForCommit(ch));
        }
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

    static boolean apply(Belarus country, byte[] changes) throws Exception {
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
                    }

                    @Override
                    public void beforeUpdateWay(long id) {
                        if (!inside) {
                            IOsmObject obj = country.getWayById(id);
                            if (obj != null) {
                                inside = country.contains(obj);
                            }
                        }
                    }

                    @Override
                    public void beforeUpdateRelation(long id) {
                        if (!inside) {
                            IOsmObject obj = country.getRelationById(id);
                            if (obj != null) {
                                inside = country.contains(obj);
                            }
                        }
                    }

                    @Override
                    public void afterUpdateNode(long id) {
                        if (!inside) {
                            IOsmObject obj = country.getNodeById(id);
                            if (obj != null) {
                                inside = country.contains(obj);
                            }
                        }
                    }

                    @Override
                    public void afterUpdateWay(long id) {
                        if (!inside) {
                            IOsmObject obj = country.getWayById(id);
                            if (obj != null) {
                                inside = country.contains(obj);
                            }
                        }
                    }

                    @Override
                    public void afterUpdateRelation(long id) {
                        if (!inside) {
                            IOsmObject obj = country.getRelationById(id);
                            if (obj != null) {
                                inside = country.contains(obj);
                            }
                        }
                    }
                });
        return inside;
    }

    static DataFileStatus getFileStatus(String file) throws Exception {

        final DataFileStatus status = new DataFileStatus();

        // collect changesets
        new O5MDriver(new O5MReader() {
            @Override
            protected void fileTimestamp(long timestamp) {
                status.initialDate = timestamp;
            }

            @Override
            protected void createNode(O5MDriver driver, long id, int lat, int lon, String user) {
                status.knownChangesets.add(driver.getCurrentChangeset());
            }

            @Override
            protected void createWay(O5MDriver driver, long id, long[] nodes, String user) {
                status.knownChangesets.add(driver.getCurrentChangeset());
            }

            @Override
            protected void createRelation(O5MDriver driver, long id, long[] memberIds, byte[] memberTypes,
                    String user) {
                status.knownChangesets.add(driver.getCurrentChangeset());
            }
        }).read(new File(file));

        return status;
    }
}
