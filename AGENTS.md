# AGENTS.md

## Project overview

Kotlin Multiplatform service that coordinates a shared "bouncy world" physics simulation. Display clients register over REST/TCP and receive body positions; a KVision JS web UI provides admin/monitoring. The runnable artifact is a JVM fat JAR (`server/build/libs/server-*.jar`) started via `java -jar`, default ports REST 8080 and TCP 9002.

## Repository layout

* `server/` — active KMP module (JVM backend + JS frontend + shared code)
  * `src/jvmMain/kotlin/` — Ktor app (`bw.AppKt`), routing, command processors, domain model, physics simulators, TCP server
  * `src/jsMain/kotlin/bw/` — KVision web UI; `src/jsMain/web/` entry HTML
  * `src/commonMain/kotlin/bw/` — shared KVision `@KVService` interfaces and models
  * `src/jvmTest/kotlin/` — JVM unit tests (Kotest, JUnit 5, MockK)
  * `src/jvmMain/resources/` — `application.conf`, `logback.xml`, `shapes/*.dat`
* `visualizations/` — Micronaut/TornadoFX desktop app; **excluded** from `settings.gradle.kts` (not built by default)
* `gradle/libs.versions.toml` — dependency and plugin versions

## Build and test commands

Run from the repository root. Requires JDK 17 and network for first-time dependency download.

```bash
./gradlew jar                    # default task; fat JAR with embedded frontend
./gradlew check                  # runs JVM tests (JS tests have no sources)
./gradlew :server:jvmTest        # JVM tests only
./gradlew :server:jvmRun         # dev backend on :8080
./gradlew :server:jsBrowserDevelopmentRun   # webpack dev server on :3000
```

**Verified status (2026-06):** `jar`, `check`, and `:server:jvmTest` succeed. `./gradlew build` currently fails on `compileCommonMainKotlinMetadata` (KSP-generated `BouncyService` missing `getWorldData` implementation). Prefer `jar` + `check` for release and CI until that is fixed.

No lint/format Gradle tasks are configured (`ktlintCheck`, `detekt`, `spotless` not present). Kotlin official code style is set in `gradle.properties`.

## Coding conventions

* Prefer idiomatic Kotlin; keep functions small and focused.
* Preserve existing package layout (`domain`, `simulator`, `routing`, `command`, `geometry`, `bw`, etc.).
* JVM toolchain 17; Kotlin 2.1.0; Ktor 3.x; KVision 8.x.
* State is passed via Ktor `Application.attributes`, not a DI framework. Command processors (`WorldCommandProcessor`, `ClientCommandProcessor`, `ShapesCommandProcessor`) mediate API and TCP actions.
* Config comes from `application.conf` with env-var overrides (`WORLD_*`, `HOST`, `PORT`, `TCP_HOST`, `TCP_PORT`). See `README.md` for defaults.
* Do not add new libraries or Gradle plugins unless the task requires it.

## Testing expectations

* Add or update tests in `server/src/jvmTest/kotlin/` for behavior changes.
* Framework: Kotest assertions + JUnit 5 runner; MockK for mocks.
* Run `./gradlew :server:jvmTest` (or `check`) before considering work complete.
* `server/src/testx/` is outside the build; do not use it for new tests.

## Agent workflow

1. Read this file and skim nearby code in the relevant `server/src/*` tree.
2. Make the smallest safe change.
3. Run `check` (and `jar` if packaging or KSP/common code changed).
4. Summarize what changed, what was tested, and any remaining risks.

## Do not edit without explicit reason

* Generated files (e.g. `server/build/generated/ksp/`).
* Build outputs (`build/`), Gradle wrapper, or secrets/env-specific configs.
* `visualizations/` unless the task explicitly targets it.
* Large unrelated refactors while fixing a focused issue.

## Project-specific notes

* **Architecture:** `World` domain object + `WorldSimulator` (bounded or wrapping) drives physics; `WorldFactory` wires config. REST routes in `routing/`; persistent client protocol also served by `TcpServer` on a separate coroutine.
* **Entry point:** `bw.AppKt` (`server/src/jvmMain/kotlin/bw/App.kt`).
* **Frontend:** KVision remote services (`IBouncyService`, `IBouncyWsService` in `commonMain`); KSP generates service stubs — keep JVM `actual` implementations in sync with the interface.
* **Concurrency:** `runBlocking` in `main`; TCP server and simulation use coroutines (`Dispatchers.IO`).
* **Persistence:** In-memory only; shape definitions loaded from `shapes/*.dat` resources.
* **CI:** No in-repo CI config (no `.github/workflows`, GitLab CI, or Jenkinsfile). Releases are published manually to GitHub releases.
