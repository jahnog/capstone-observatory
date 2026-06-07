## Low-priority tile generation

Use `./generate-tiles-low-priority.sh` for server rebuilds. The script wraps `sbt "runMain observatory.Main"` with both `ionice` and `nice` so the generator yields CPU and disk priority to the rest of the host.

Direct Linux launches of `sbt "runMain observatory.Main"` also re-exec the tile-generation JVM under the same low-priority settings, so the entrypoint does not continue at normal priority if the wrapper is bypassed.

The generator still writes the same default layout under `target/temperatures/<year>/<zoom>/<x>-<y>.png` and `target/deviations/<year>/<zoom>/<x>-<y>.png`.

### Recommended server invocation

```bash
./generate-tiles-low-priority.sh
```

### Safe smoke run

Use environment variables to restrict work to a small slice when validating the launcher or rebuilding incrementally:

```bash
TILE_GENERATION_TARGET_ROOT=target/tile-generation-smoke \
TILE_GENERATION_LAYERS=temperatures \
TILE_GENERATION_TEMPERATURE_YEARS=1975 \
TILE_GENERATION_DEVIATION_YEARS= \
TILE_GENERATION_BASELINE_YEARS= \
TILE_GENERATION_MAX_ZOOM=0 \
./generate-tiles-low-priority.sh
```

### Supported environment overrides

- `TILE_GENERATION_TARGET_ROOT`: alternate output root for validation or staged rebuilds. Defaults to `target`.
- `TILE_GENERATION_LAYERS`: comma-separated layer list such as `temperatures` or `temperatures,deviations`.
- `TILE_GENERATION_TEMPERATURE_YEARS`: comma-separated years for temperature tiles.
- `TILE_GENERATION_DEVIATION_YEARS`: comma-separated years for deviation tiles. Set to an empty string to skip them.
- `TILE_GENERATION_BASELINE_YEARS`: comma-separated baseline years used to compute normals for deviations.
- `TILE_GENERATION_MAX_ZOOM`: highest zoom level to generate.
- `TILE_GENERATION_NICE_LEVEL`: niceness value passed to `nice`. Defaults to `15`.
- `TILE_GENERATION_IONICE_CLASS`: `ionice` class passed to the launcher. Defaults to `3` for idle I/O scheduling.

If either `nice` or `ionice` is unavailable, the launcher fails fast because it cannot guarantee the required low-priority execution profile.