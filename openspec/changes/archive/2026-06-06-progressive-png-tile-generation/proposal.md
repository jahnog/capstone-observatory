## Why

The current PNG tile generation workflow is expensive to rerun on a live server because it can spend time recomputing work that is already on disk and it runs at normal process priority. This change is needed so long-running tile generation can resume progressively, skip existing outputs, and reduce its impact on the host while it is rebuilding the tile set.

## What Changes

- Add a resumable tile-generation workflow that treats existing PNG tiles as completed work and only renders missing tiles.
- Make tile generation progressive at the year and tile level so partial output on disk can be continued safely after interruption.
- Add a low-priority execution path that runs the generator through `nice` and `ionice` so CPU and disk contention are reduced on the current server.
- Preserve the generated tile layout and existing rendered image format so the browser-facing climate views keep working without path changes.

## Capabilities

### New Capabilities
- `progressive-tile-generation`: The observatory tile-generation workflow can resume from partial PNG output, skip tiles that already exist, and provide a low-priority execution mode suitable for running on a shared server.

### Modified Capabilities
- None.

## Impact

- Affected generation entry points such as `src/main/scala/observatory/Main.scala` and any helper code that determines which years and tiles need rendering.
- Affected operational scripts or documented commands used to launch the tile generator on the server.
- No intended changes to the tile file naming convention, the rendered PNG format, or the browser pages that consume `target/temperatures/...` and `target/deviations/...`.