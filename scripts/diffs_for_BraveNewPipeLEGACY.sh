#!/bin/bash
#
# Script Summary:
#
# Compares Java source code changes between two Git tags in the NewPipe upstream repository,
# mapping them from the 'main' flavor to the 'braveLegacy' flavor used in this fork.
#
# It assumes the existence of two directory structures:
# - app/src/main/java/         (original upstream code)
# - app/src/braveLegacy/java/ (forked or legacy version)
#
# For each file in the legacy source directory (excluding ones prefixed with 'Brave'),
# it locates the corresponding file in the main flavor and prints the `git diff` output
# between the specified tags. The diff paths are rewritten to show legacy paths for easier comparison.
#
# Requirements:
# - The upstream remote must be set (e.g., via `git remote add upstream https://github.com/TeamNewPipe/NewPipe`)
# - You must run `git fetch upstream --tags` before running this script (if you want to work with tags
#
# Example usage:
# - Set the oldVersionTag and newVersionTag to define the range of changes. (commit hashes also work
#
# License: GPL_v3
# Author: evermind
# Version: 1.0.0
#
# Changelog:
# - v1.0.0 (20250808):
#   * Initial version
#


mainFlavor=main
legacyFlavor=braveLegacy
legacyBase=app/src/$legacyFlavor/java/

# call first: git fetch upstream  --tags if
# -> upstream has to be defined as https://github.com/TeamNewPipe/NewPipe
oldVersionTag="v0.28.1-2.6.0" # could also be hashes
newVersionTag="v0.28.2-2.6.0"

for x in `find $legacyBase -type f ! -iname 'Brave*' | grep -v orig$ | grep -v rej$ | grep -v '\.*.sw.$'` ; do
    mainPath="$(echo "$x" | sed "s@src/$legacyFlavor/java@src/$mainFlavor/java@")"
    git diff $oldVersionTag..$newVersionTag $mainPath | sed "s@src/$mainFlavor/java@src/$legacyFlavor/java@"
    # could also be compared to the current HEAD
    # git diff $oldVersionTag $mainPath | sed "s@src/$mainFlavor/java@src/$legacyFlavor/java@"
done
