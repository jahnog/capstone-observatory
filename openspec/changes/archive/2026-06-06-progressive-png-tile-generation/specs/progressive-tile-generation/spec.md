## ADDED Requirements

### Requirement: Tile-set completeness uses the full expected output set
The tile generation workflow MUST treat a year or layer as complete only when every expected PNG tile for the configured zoom range exists in the output tree.

#### Scenario: sentinel tile exists but another tile is missing
- **WHEN** the highest-zoom sentinel tile exists for a year or layer but another expected tile path is missing
- **THEN** the next generation run still treats that output set as incomplete and schedules the missing tile

#### Scenario: fully complete output set is skipped
- **WHEN** every expected PNG tile for a year or layer already exists
- **THEN** the generation run skips rendering new tiles for that output set

### Requirement: Existing completed tiles are preserved
The tile generation workflow MUST skip rendering and overwriting any tile whose final PNG path already exists.

#### Scenario: rerun encounters an existing tile
- **WHEN** a generation run reaches a tile whose target PNG file is already present
- **THEN** the system leaves that file unchanged and proceeds to the remaining missing tiles

### Requirement: Tile completion is interruption-safe
The tile generation workflow MUST only treat a tile as complete after the final PNG file has been written successfully to its destination path.

#### Scenario: interrupted write does not count as completed output
- **WHEN** a generation run is interrupted before a tile write reaches its final destination path
- **THEN** a later generation run treats that tile as missing and renders it again

### Requirement: Low-priority server launch path is available
The repository MUST provide a documented low-priority launch path that invokes the tile generator through both `ionice` and `nice`.

#### Scenario: operator uses the recommended server command
- **WHEN** an operator starts tile generation using the documented low-priority command or script
- **THEN** the process executes `ionice` and `nice` before running the Scala tile-generation entry point