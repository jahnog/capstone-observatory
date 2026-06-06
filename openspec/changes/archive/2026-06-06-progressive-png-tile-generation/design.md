## Context

The current tile generator in `observatory.Main` already avoids overwriting an individual PNG once it reaches the inner render loop, but it still decides whether a year needs work by checking a single sentinel tile at the maximum zoom. That means a year can look complete even when another tile is missing, and the code duplicates similar traversal logic for temperatures and deviations. The operational path is also manual: the generator is run through `sbt "runMain observatory.Main"` with no built-in `nice` or `ionice` wrapper, which is risky on the shared server.

## Goals / Non-Goals

**Goals:**
- Make tile generation resumable from partial output by determining missing work from the full expected tile set instead of a single sentinel file.
- Preserve existing PNG paths and browser consumption patterns while avoiding regeneration of tiles that already exist.
- Reduce wasted CPU and disk contention on the shared server by adding a low-priority launcher that uses `nice` and `ionice`.
- Keep the implementation local to the tile-generation path rather than redesigning the broader observatory pipeline.

**Non-Goals:**
- Changing the zoom levels, tile naming convention, or image format.
- Reworking the browser pages or the visualization algorithms used to render each tile.
- Introducing new external runtime dependencies beyond standard Linux process-priority tools already expected on the server.

## Decisions

### Build a missing-tile work plan per layer and year
Before loading yearly data or rendering tiles, the generator will enumerate the expected `(zoom, x, y)` coordinates for the supported zoom range and compare them against the corresponding output directory. The result will be a list of missing tiles for each layer/year pair.

This replaces the current max-zoom sentinel heuristic and makes partial years resumable even when the sentinel tile exists.

Alternative considered: keep the sentinel check and only improve logging. Rejected because it does not solve the core correctness issue for interrupted or partial runs.

### Only perform expensive per-year computation when the work plan is non-empty
The generator will skip loading temperature records, computing yearly averages, and building deviation inputs for any layer/year pair whose work plan is empty. For deviations, the code will also use the same completeness logic when deciding which historical temperature years are available as baseline inputs.

Alternative considered: continue loading yearly data and rely on the inner file-exists guard. Rejected because it still burns CPU and I/O on no-op years.

### Funnel tile writes through a shared progressive renderer with stable output paths
Temperatures and deviations will use a shared helper that accepts the planned missing tile coordinates, ensures parent directories exist, renders only those tiles, and writes them to the existing `target/<layer>/<year>/<zoom>/<x>-<y>.png` layout. The helper will emit progress information at the year/layer level so operators can see forward movement during long runs.

Alternative considered: keep two copy-pasted nested loops. Rejected because the skip/progress behavior would drift and become harder to maintain.

### Treat a tile as complete only after a successful final-file write
The renderer will write each tile through a temporary file in the destination directory and move it into place only after the PNG write succeeds. This prevents interrupted writes from leaving behind a final-path file that would be mistaken for completed work on the next run.

Alternative considered: trust direct writes to the final path. Rejected because a killed process can leave a misleading partial artifact.

### Add a server-safe launcher that wraps sbt with `ionice` and `nice`
The repository will add a documented launcher script or command wrapper that executes the generator via `ionice` and `nice`, with parameters chosen for background, low-contention execution on Linux. This wrapper becomes the recommended operational entry point for server runs.

Alternative considered: document a plain `sbt` command and leave prioritization to the operator. Rejected because the user requirement is to make the safe path the default, not an implicit convention.

## Risks / Trade-offs

- [Risk: Full completeness scans add extra filesystem checks] → Mitigation: the zoom range is small, so enumerating expected paths is far cheaper than rerendering tiles or loading yearly datasets unnecessarily.
- [Risk: Low-priority execution increases wall-clock time] → Mitigation: this is acceptable for background rebuilds because protecting the shared server is the higher priority.
- [Risk: Atomic file replacement behavior can differ across filesystems] → Mitigation: write temporary files into the destination directory and use the standard same-directory move path.
- [Risk: Progress logging can become noisy] → Mitigation: log at the year/layer and tile-count level rather than per-pixel or overly chatty inner-loop messages.

## Migration Plan

1. Introduce a helper that enumerates expected tiles and returns the missing coordinates for a given layer/year.
2. Refactor temperature generation to load data only when missing coordinates exist and to render through the shared helper.
3. Refactor deviation generation to use the same completeness rules for both missing output detection and baseline-year selection.
4. Add the low-priority launcher and document it as the preferred server command.
5. Validate by running a partial generation twice and confirming that the second run skips completed files while filling gaps.

## Open Questions

- Should the launcher hard-code a conservative priority profile, or should it accept optional overrides for operators who need a different niceness level?
- Is progress output best kept in stdout, or should the launcher also encourage redirecting logs to a file for long server runs?