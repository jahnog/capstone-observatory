## 1. Build missing-tile planning helpers

- [x] 1.1 Add a helper in the tile-generation path that enumerates the expected `(zoom, x, y)` coordinates for the supported zoom range and returns the missing output tiles for a given layer and year.
- [x] 1.2 Replace the current sentinel-tile completeness checks with the new missing-tile planner for both temperature and deviation outputs.
- [x] 1.3 Reuse the same completeness logic when deciding which historical temperature years are available as baseline inputs for deviation generation.

## 2. Refactor progressive rendering behavior

- [x] 2.1 Refactor temperature generation so yearly records are loaded only when the missing-tile planner reports work to do.
- [x] 2.2 Refactor deviation generation to render only planned missing tiles and avoid no-op work for complete years.
- [x] 2.3 Route tile writes through a temporary-file-then-move flow so a tile is only considered complete after a successful final-path write.
- [x] 2.4 Add concise progress logging that reports year, layer, and missing-tile counts during long-running generation.

## 3. Add the low-priority server launch path

- [x] 3.1 Add a Linux-friendly launcher script or documented wrapper command that runs `observatory.Main` through both `ionice` and `nice`.
- [x] 3.2 Document the recommended server-safe invocation and any assumptions or fallback behavior for environments where these tools are unavailable.

## 4. Validate resumable generation end to end

- [x] 4.1 Exercise a partial-output case where the sentinel tile exists but another expected tile is missing, and confirm the generator still schedules the missing tile.
- [x] 4.2 Rerun generation against an already complete output set and confirm existing PNG tiles are not overwritten.
- [x] 4.3 Simulate or observe an interrupted tile write and confirm the next run regenerates the unfinished tile instead of treating it as complete.
- [x] 4.4 Launch the generator through the new low-priority path on Linux and confirm it still writes tiles to the existing output layout.