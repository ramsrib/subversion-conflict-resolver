Subversion Tree Conflict Resolver

- A tree conflict occurs when a developer moved/renamed/deleted a file or folder, which another developer either also has moved/renamed/deleted or just modified. There are many different situations that can result in a tree conflict, and all of them require different steps to resolve the conflict.

This project has two conflict resolvers:

TreeConflictResolver

- This resolver assumes that you're just moving the files from one directory to another. So, it takes easy approach by search for that file in trunk working copy of the merge revision and updates the branch copy of the moved file. And marks the conflict  as resolved.

TreeConflictResolverUsingSvnKit

- It's not complete and work in progress!.
- It does the same thing as TreeConflictResolver, but it ll be more intelligent and uses SVNKit library.


To run,

- It needs the trunk working copy and branch work copy and its hard coded (will be fixed soon).
- So edit TreeConflictResolver.java file and package it using 'mvn clean package'
- then run, java -jar target/subversion-merge-conflict-resolver.jar
