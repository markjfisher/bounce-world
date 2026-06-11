# AGENTS.md

## Project overview

Kotlin Multiplatform service that coordinates a shared "bouncy world" physics simulation. Display clients register over REST/TCP and receive body positions; a KVision 9 JS web UI provides admin/monitoring. The runnable artifact is a JVM fat JAR (`server/build/libs/server-*.jar`) started via `java -jar`, default ports REST 8080 and TCP 9002.

Requires **Java 25** to run Gradle (KVision 9.6 / Kilua RPC Gradle plugins). JVM toolchain is also 25.

## Repository layout

* `server/` — active KMP module (JVM backend + JS frontend + shared code)
  * `src/jvmMain/kotlin/` — Ktor app (`bw.AppKt`), routing, command processors, domain model, physics simulators, TCP server
  * `src/jsMain/kotlin/bw/` — KVision 9 web UI
  * `src/jsMain/resources/` — `index.html`, `modules/css/bw.css` (KVision 9 ES-module layout)
  * `src/commonMain/kotlin/bw/` — shared Kilua RPC `@RpcService` interfaces and models
  * `src/jvmTest/kotlin/` — JVM unit tests (Kotest, JUnit 5, MockK)
  * `src/jvmMain/resources/` — `application.conf`, `logback.xml`, `shapes/*.dat`
* `visualizations/` — Micronaut/TornadoFX desktop app; **excluded** from `settings.gradle.kts`
* `gradle/libs.versions.toml` — dependency and plugin versions
* `gradle.properties` — Kotlin/KSP/Kilua RPC versions for plugins (`systemProp.*`)

## Build and test commands

Run from the repository root. Requires JDK 25 and Gradle 9.1 (wrapper included).

```bash
./gradlew                          # default: clean + jarWithJs (fat JAR with embedded frontend)
./gradlew check                    # runs JVM tests
./gradlew :server:jvmTest          # JVM tests only
./gradlew :server:jvmRun           # dev backend on :8080
./gradlew :server:jsBrowserDevelopmentRun   # webpack dev server on :3000
./gradlew build                    # full assemble + check (including metadata compile)
./gradlew versionCatalogUpdate --interactive   # check dependency updates (pinned versions excluded)
```

Dependency update pins (in root `build.gradle.kts`): `kotlin-version`, `ksp-version`, `kilua-rpc-version`, `kvision-version` — bump these together, not via blind catalog update.

No lint/format Gradle tasks are configured (`ktlintCheck`, `detekt`, `spotless` not present).

## Coding conventions

* Prefer idiomatic Kotlin; keep functions small and focused.
* Preserve existing package layout (`domain`, `simulator`, `routing`, `command`, `geometry`, `bw`, etc.).
* Stack: Kotlin 2.3.21, KSP 2.3.9, Kilua RPC 0.0.45, KVision 9.6, Ktor 3.5, JVM toolchain 25.
* RPC: Kilua RPC `@RpcService` interfaces in `commonMain`; JVM implementations are plain classes registered via `initRpc { registerService<...> { ... } }` in `App.kt` — not `actual` classes.
* State via Ktor `Application.attributes` and command processors. Config from `application.conf` with env-var overrides (`WORLD_*`, `HOST`, `PORT`, `TCP_HOST`, `TCP_PORT`).
* JS resources: use `@JsModule` + `useModule()` (not `require()`). CSS lives under `src/jsMain/resources/modules/`.
* Do not add new libraries or Gradle plugins unless the task requires it.

## Testing expectations

* Add or update tests in `server/src/jvmTest/kotlin/` for behavior changes.
* Framework: Kotest assertions + JUnit 5 runner; MockK for mocks.
* Run `./gradlew :server:jvmTest` (or `check`) before considering work complete.
* `server/src/testx/` is outside the build; do not use it for new tests.

## Agent workflow

1. Read this file and skim nearby code in the relevant `server/src/*` tree.
2. Make the smallest safe change.
3. Run `check` (and `jarWithJs` if packaging, RPC, or frontend code changed).
4. Summarize what changed, what was tested, and any remaining risks.

## Do not edit without explicit reason

* Generated files (e.g. `server/build/generated/ksp/`).
* Build outputs (`build/`), Gradle wrapper, or secrets/env-specific configs.
* `visualizations/` unless the task explicitly targets it.
* Large unrelated refactors while fixing a focused issue.

## Project-specific notes

* **Architecture:** `World` + `WorldSimulator` drives physics; `WorldFactory` wires config. REST in `routing/`; `TcpServer` handles persistent client protocol on a separate coroutine.
* **Entry point:** `bw.AppKt` (`server/src/jvmMain/kotlin/bw/App.kt`).
* **Frontend RPC:** `IBouncyService` / `IBouncyWsService` (Kilua RPC). Call `registerRemoteTypes()` in both JVM and JS `main()`.
* **Concurrency:** `runBlocking` in `main`; TCP server and simulation use coroutines (`Dispatchers.IO`).
* **Persistence:** In-memory only; shapes loaded from `shapes/*.dat` resources.
* **CI:** No in-repo CI config. Releases published manually to GitHub releases.
