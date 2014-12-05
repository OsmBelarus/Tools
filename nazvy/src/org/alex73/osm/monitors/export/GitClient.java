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
import java.util.Date;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Доступ да git каманд.
 */
public class GitClient {
    final File dir;
    final Repository repository;

    public GitClient(String dir) throws Exception {
        this.dir = new File(dir);
        repository = Git.open(this.dir).getRepository();
    }

    public synchronized void add(String path) {
        try {
            new Git(repository).add().addFilepattern(path).call();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public synchronized void remove(String path) {
        try {
            new Git(repository).rm().addFilepattern(path).call();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public synchronized void commit(String user, String email, String commitMessage, boolean amend)
            throws Exception {
        System.out.println(new Date() + " Commit: " + commitMessage);
        new Git(repository).commit().setAuthor(user, email).setMessage(commitMessage).setAmend(amend).call();
    }

    public synchronized boolean hasCommit(String messagePart) throws Exception {
        for (RevCommit commit : new Git(repository).log().call()) {
            if (commit.getShortMessage().contains(messagePart)) {
                return true;
            }
        }
        return false;
    }

    public synchronized RevCommit getPrevCommit() throws Exception {
        for (RevCommit commit : new Git(repository).log().call()) {
            return commit;
        }
        return null;
    }

    public synchronized void reset() throws Exception {
        new Git(repository).reset().setMode(ResetType.HARD).call();
    }
}
