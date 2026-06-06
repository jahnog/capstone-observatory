## Why

The project currently cannot start its build on the current system because `sbt 0.13.18` fails under Java 21 before compilation begins, stopping local development and validation. This change is needed now to restore a working build with the minimum necessary toolchain updates, while preserving the existing project behavior and structure.

## What Changes

- Update the build toolchain to a Java 21 compatible `sbt` and Scala combination.
- Adjust build settings, plugins, and dependency coordinates only where required to keep the project compiling on the current system.
- Document the compatibility target and the minimal migration boundaries so future maintenance does not drift into a broader modernization effort.

## Capabilities

### New Capabilities
- `java-21-build-compatibility`: The project can bootstrap and compile on Java 21 using the minimum viable build, Scala, and dependency updates required by the current environment.

### Modified Capabilities
- None.

## Impact

- Affected build files such as `project/build.properties`, `build.sbt`, and any plugin definitions or build helpers that are incompatible with Java 21.
- Affected dependency and Scala binary versions where old artifacts are no longer compatible with the updated toolchain.
- Affected developer workflow because local compile and test commands must run successfully on the current system again.