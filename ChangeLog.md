# Changelog

## [Unreleased]

## [2.2.0]

- Major version bump of all libraries and fixes to codebase to support, including:
  - java 25 (from 17)
  - kotlin 2.3.21 (not 2.4 because of ksp only at 2.3.9)
  - ktor 3.5.0 (from 3.0.3)
  - kvision (version 8 to 9)
  - kvision-server-rpc -> kilua-rpc-ktor + kvision-common-remote
- This fixes the standard "./gradlew build" which had metadata issues in kvision 8
- css/html moved to resources folder
- default packaging from jar -> jarWithJs

## [2.1.1]

- Fixed an edge bouncing issue where body would be close enough to it that it
  was showing as reflected in the client data, but should never happen in bounded space.
  There is now a small delta to ensure it never gets that close to the edge, and so
  should not cause rounding issues.

## [2.1.0]

- Fixed concurrency issue with the bodies list. Rather large refactor to achieve this.

## [2.0.1]

- Started changelog
- Updated concurrency to fix MCE error on the bodies list. Now bodies are added safely
  and the step loop locks the list with a mutex.

## [2.0.0]

The version that's been around for a while.