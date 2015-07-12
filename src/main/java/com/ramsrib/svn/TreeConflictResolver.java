package com.ramsrib.svn;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Subversion Tree Conflict Resolver
 *
 * @author sriram
 */
public class TreeConflictResolver {

    /*
     1. Run svn st in the given directory
     2. Get the tree conflicts for specific scenario -
            - "Local missing, incoming edit upon update" - just find the local working copy and get the trunk copy and copy it and mark it as resolved.
            - "Local missing, incoming delete upon update" - just find the local copy and delete it and mark it as resolved
            - "Local file missing, incoming file replace upon merge"
     3. Extract the file level conflicts as a list
     4. For each item in the list:
            - Get the file name from file path and get only .java files
            - Find the file in the working copy
            - Find the file in the trunk
            - Copy the file from trunk path to working copy path
            - Run svn resolved for the file path (working copy conflict url)
     */

    private final static String trunkPath = "/tmp/my-trunk-wc/";
    private final static String branchPath = "/tmp/my-branch1-wc/";
    private final static String[] SUPPORTED_FILE_TYPES = {".java", ".jsp", ".jspx", ".xml", ".css", ".js", ".tagx", ".wsdd"};

    private final static Map<String, File> trunkFileMap = new HashMap<>();
    private final static Map<String, File> branchFileMap = new HashMap<>();
    private final static Map<String, File> branchFileConflictMap = new HashMap<>();
    private final static List<String> skippedFileList = new ArrayList<>();


    public static void main(String[] args) throws IOException {

        log("Starting Subversion Tree Conflict Resolver.");

        // walk the trunk and branch directories and build the file name and path map in memory
        doFileWalk(trunkPath, trunkFileMap);
        doFileWalk(branchPath, branchFileMap);

        // get the conflict list from file path
        List<String> conflictList = prepareConflictList(new File(branchPath).getAbsolutePath());
        populateBranchFileConflictMap(conflictList);

        // resolve the conflict
        doResolve();

        // print summary
        log("");
        log("Summary : ");
        log("Total Conflicts : " + conflictList.size());
        log("Total Supported Conflicts : " + branchFileConflictMap.size());
        log("Total Files skipped (from supported) : " + skippedFileList.size());
        log("Skipped Files List : " + skippedFileList);

    }

    private static void doFileWalk(String directory, Map<String, File> fileMap) throws IOException {

        Instant start = Instant.now();
        Path startingDir = Paths.get(directory);
        TreeConflictFileVisitor fileVisitor = new TreeConflictFileVisitor(fileMap);
        Files.walkFileTree(startingDir, fileVisitor);
        Duration dur = Duration.between(start, Instant.now());
        System.out.format("Tree Walk Completed for %s in : %s ms.%n", directory, dur.toMillis());
    }

    private static void doResolve() throws IOException {
        Instant start = Instant.now();
        for (String filename : branchFileConflictMap.keySet()) {
            String[] copyCommand = {"cp", trunkFileMap.get(filename).getAbsolutePath(), branchFileMap.get(filename).getAbsolutePath()};
            String[] svnResolvedCommand = {"svn", "resolved", branchFileConflictMap.get(filename).getAbsolutePath()};
            InputStream copyCommandResultStream = runCommand(copyCommand);
            printInputStream(copyCommandResultStream);
            InputStream svnResolvedCommandResultStream = runCommand(svnResolvedCommand);
            printInputStream(svnResolvedCommandResultStream);

        }
        Duration dur = Duration.between(start, Instant.now());
        System.out.format("Tree Conflicts Resolved in : %s seconds.%n", dur.getSeconds());
    }

    private static void printInputStream(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log(line);
            }
        }
    }

    private static void populateBranchFileConflictMap(List<String> conflictList) {

        for (String conflict : conflictList) {
            // path points to branch
            File conflictEntry = new File(conflict);
            // can be file or directory name
            String filename = conflictEntry.getName();

            if (isFilenameEndsWithGivenSuffix(filename, SUPPORTED_FILE_TYPES)) {
                if (branchFileConflictMap.containsKey(filename)) {
                    log("Duplicate file found and skipping the file conflict resolution for the file with this name : " + filename);
                    branchFileConflictMap.remove(filename);
                    skippedFileList.add(filename);
                } else {
                    branchFileConflictMap.put(filename, conflictEntry);
                }
            }
        }

    }

    private static boolean isFilenameEndsWithGivenSuffix(String filename, String[] supportedTypes) {
        if (supportedTypes.length == 0) {
            return true;
        }
        boolean fileSuffixMatches = false;
        for (String suffix : supportedTypes) {
            fileSuffixMatches = (fileSuffixMatches || filename.endsWith(suffix));
        }
        return fileSuffixMatches;
    }

    private static InputStream runCommand(String[] command) throws IOException {
        log("Running command : " + Arrays.asList(command));
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        return process.getInputStream();
    }

    private static List<String> prepareConflictList(String dirPath) throws IOException {

        List<String> conflictList = new ArrayList<>();
        String[] command = {"svn", "status", dirPath};

        try (
                InputStream inputStream = runCommand(command);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))
        ) {

            String line;
            while ((line = reader.readLine()) != null) {

                String[] words = line.split("\\s");

                boolean missingFlag = false;
                boolean conflictFlag = false;
                for (String word : words) {

                    if (word.equals("!")) {
                        missingFlag = true;
                    }

                    if (missingFlag) {
                        // local missing, incoming edit/replace
                        // NOTE: Don't run with local missing, incoming delete conflict, becoz it doesn't care about it.
                        if (word.equals("C")) {
                            // conflict detected
                            conflictFlag = true;
                        } else {
                            if (conflictFlag) {
                                conflictList.add(word);
                            }
                        }
                    }

                }
            }
        }

        return conflictList;
    }

    private static void log(String message) {
        System.out.println(message);
    }

    static class TreeConflictFileVisitor extends SimpleFileVisitor<Path> {

        Map<String, File> treeFileMap;

        TreeConflictFileVisitor(Map<String, File> fileMap) {
            this.treeFileMap = fileMap;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {

            if (attr.isRegularFile()) {
                treeFileMap.put(file.toFile().getName(), file.toFile());
            }

            return FileVisitResult.CONTINUE;
        }
    }

}
