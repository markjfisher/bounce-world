# Changelog

## [Unreleased]

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