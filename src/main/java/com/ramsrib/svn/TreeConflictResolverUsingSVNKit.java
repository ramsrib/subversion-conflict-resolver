package com.ramsrib.svn;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNClientManager;

import java.io.File;

/**
 * Tree Conflict Resolver using SVNKit library
 * @author sriram
 */
public class TreeConflictResolverUsingSVNKit {

    private static SVNClientManager svnClientManager = SVNClientManager.newInstance();

    private static void showStatus(File wcPath, boolean isRecursive, boolean isRemote, boolean isReportAll,
                                   boolean isIncludeIgnored, boolean isCollectParentExternals) throws SVNException {

        svnClientManager.getStatusClient().doStatus(wcPath, isRecursive, isRemote, isReportAll,
                isIncludeIgnored, isCollectParentExternals,
                new StatusHandler(isRemote));
    }

    public static void main(String[] args) throws Exception {

        File wcPath = new File("/tmp/repo1-wc");

        showStatus(wcPath, true, false, true, false, false);
    }

}
