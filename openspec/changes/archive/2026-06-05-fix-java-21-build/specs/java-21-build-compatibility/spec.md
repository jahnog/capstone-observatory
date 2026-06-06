## ADDED Requirements

### Requirement: Build bootstrap on Java 21
The project SHALL load its sbt build on a Java 21 runtime without failing because of launcher or build-definition incompatibilities.

#### Scenario: sbt starts on Java 21
- **WHEN** a developer runs `sbt compile` on the current system using Java 21
- **THEN** sbt starts successfully and does not terminate with the legacy `System.setSecurityManager` failure or an equivalent build-definition bootstrap error

### Requirement: Project compiles on the current system
The project SHALL compile its existing sources on the current system after the minimum necessary build and dependency updates are applied for Java 21 compatibility.

#### Scenario: compile succeeds after compatibility updates
- **WHEN** a developer runs `sbt compile` in a clean workspace on the current system
- **THEN** the build completes successfully without requiring an older JDK installation

### Requirement: Existing build workflow remains available
The migrated build MUST preserve the current project-facing sbt workflow, including the custom submission and style-check tasks, unless a task is explicitly removed in a later approved change.

#### Scenario: custom tasks remain defined
- **WHEN** a developer loads the sbt shell after the compatibility migration
- **THEN** the build still exposes the existing custom tasks needed by the project, including `submit`, `submitLocal`, and `styleCheck`