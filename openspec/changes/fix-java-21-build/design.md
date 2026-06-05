## Context

The current project is pinned to `sbt 0.13.18` and `Scala 2.11.12`. On Java 21, the build fails before compilation because the legacy sbt launcher still calls `System.setSecurityManager`, which is no longer supported. The build definition also contains sbt 0.13-era constructs such as `extends Build`, `compile in Test`, and `packageSubmission in Compile`, so restoring compatibility requires both a launcher upgrade and a narrow sbt-definition migration.

## Goals / Non-Goals

**Goals:**
- Make the project bootstrap and compile on Java 21 in the current environment.
- Keep the migration as small as possible by changing only the build toolchain, build definition syntax, and the dependency versions that the new Scala binary version forces.
- Preserve existing project behavior and custom sbt tasks such as `submit`, `submitLocal`, and `styleCheck`.

**Non-Goals:**
- Broad modernization of application code, architecture, or library choices.
- Reworking the Coursera submission flow beyond compatibility fixes required by the sbt migration.
- Upgrading dependencies that are already compatible unless they block Java 21 builds.

## Decisions

### Upgrade the sbt launcher to a maintained 1.x release
The first blocking failure is in the launcher itself, so the build must move off sbt 0.13. A maintained sbt 1.x release is the minimum viable baseline for Java 21 support and current tooling.

Alternative considered: keep sbt 0.13 and require an older JDK. Rejected because the requested target is the current system running Java 21.

### Raise Scala only to the smallest Java 21 compatible baseline that keeps the project buildable
Scala 2.11 is too old to be a safe target on Java 21, so the build should move to the smallest realistic modern baseline, expected to be Scala 2.12.x unless verification proves a different version is required. This limits churn versus a full move to newer language generations while still unlocking current artifacts.

Alternative considered: keep Scala 2.11 under a newer sbt. Rejected because Java 21 compatibility and artifact availability are both likely to fail at that baseline.

### Migrate legacy sbt syntax and build definitions only where required by sbt 1.x
The custom build logic in `project/` should be retained, but outdated syntax must be translated to the modern scoped-key style so the build definition compiles under sbt 1.x.

Alternative considered: rewrite the entire build into a new structure. Rejected because it expands scope beyond the minimum necessary fix.

### Update plugins and Scala-binary-versioned dependencies selectively
Plugin versions and `%`/`%%` dependencies should only be updated when the new sbt or Scala baseline makes the old artifacts unavailable or incompatible. If a plugin is abandoned, it should be removed or replaced only when it blocks the build.

Alternative considered: full dependency refresh. Rejected because it adds avoidable behavior risk.

## Risks / Trade-offs

- Dependency availability drift for the new Scala binary version, especially around Spark, Akka, Monix, and test/build tooling. → Mitigation: choose the lowest viable Scala baseline first, then update only the dependencies that fail resolution or compilation.
- Custom sbt tasks may break during scoped-key migration. → Mitigation: validate task loading after each build-definition rewrite and keep task names unchanged.
- Some old plugins may not have sbt 1.x support. → Mitigation: remove or replace only blocking plugins, and treat nonessential IDE integrations as optional if they no longer work.

## Migration Plan

1. Upgrade `project/build.properties` to a Java 21 compatible sbt 1.x version.
2. Update build-definition code and syntax in `build.sbt` and `project/` so the build loads under sbt 1.x.
3. Move Scala to the smallest viable Java 21 compatible version and adjust only the affected plugin and dependency coordinates.
4. Verify `sbt compile`, then confirm the custom tasks still load and that the project no longer requires an older JDK.

## Open Questions

- Which of the existing Scala 2.11 dependencies can remain with only a version bump, and which require replacement or removal?
- Is Scala 2.12 sufficient for all current dependencies, or does one of them force a different minimal baseline?

## Implementation Notes

- Final toolchain: `sbt 1.10.0`, root Scala `2.12.19`, Scala.js plugin `1.13.2`, and the UI subproject kept on Scala `2.12.19` once the Scala.js toolchain moved off the 0.6 line.
- Build-helper dependency updates: `scalaj-http 2.4.2`, `scalastyle 1.0.0`, and `scalatest 3.0.9` for the sbt project classpath.
- Application dependency updates forced by Scala 2.12 publication: `scrimage-core 2.1.8`, `spark-sql 2.4.8`, `scalacheck 1.12.6`, and `scalatags 0.8.5` for the Scala.js UI.
- Deferred modernization: `akka-stream 2.4.12`, `monix 2.1.1`, `fs2-io 0.9.2`, and `junit 4.10` were left in place because they already resolved under the updated toolchain and were outside the minimum compatibility scope.
- Known residual warning: sbt now reports `courseId` as lint-unused during project load, but the custom submission workflow is still wired in and `submit`, `submitLocal`, and `styleCheck` remain available.