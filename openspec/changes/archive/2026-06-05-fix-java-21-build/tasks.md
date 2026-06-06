## 1. Restore sbt bootstrap on Java 21

- [x] 1.1 Update `project/build.properties` to a Java 21 compatible sbt 1.x release and adjust blocking sbt plugins as needed for the new launcher.
- [x] 1.2 Rewrite the sbt 0.13-era build-definition syntax in `build.sbt` and `project/` so the build loads successfully under sbt 1.x.

## 2. Reconcile Scala and dependency compatibility

- [x] 2.1 Move the project to the smallest viable Scala version for Java 21 and update only the Scala-binary-versioned dependencies that fail under that baseline.
- [x] 2.2 Fix any build-definition or source-level compatibility errors introduced by the version changes while preserving the existing `submit`, `submitLocal`, and `styleCheck` workflow.

## 3. Validate and document the migration boundary

- [x] 3.1 Run `sbt compile` on the current system and confirm the project no longer requires an older JDK to build.
- [x] 3.2 Record the final toolchain choices and any intentionally deferred dependency modernization so the change remains scoped to the minimum necessary compatibility fix.