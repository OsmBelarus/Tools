package org.alex73.osm.monitors.export;

import java.io.File;
import java.util.Date;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

public class GitClient {
    final File dir;
    final Repository repository;

    public GitClient(String dir) throws Exception {
        this.dir = new File(dir);
        repository = Git.open(this.dir).getRepository();
    }

    public void add(String path) throws Exception {
        new Git(repository).add().addFilepattern(path).call();
    }

    public void commit(String user, String uid, String commitMessage) throws Exception {
        System.out.println(new Date() + " Commit: " + commitMessage);
        new Git(repository).commit().setAuthor(user, uid + "@osm.org").setMessage(commitMessage)
                .call();
    }

    public boolean hasCommit(String messagePart) throws Exception {
        for (RevCommit commit : new Git(repository).log().call()) {
            if (commit.getShortMessage().startsWith(messagePart)) {
                return true;
            }
        }
        return false;
    }

    public void reset() throws Exception {
        new Git(repository).reset().setMode(ResetType.HARD).call();
    }

    private void removeFiles(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.getName().startsWith(".git")) {
                continue;
            }
            if (f.isDirectory()) {
                removeFiles(f);
                f.delete();
            } else {
                f.delete();
            }
        }
    }
}
