#!/bin/bash

set -euo pipefail

script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
cd "$script_dir"

nice_level=${TILE_GENERATION_NICE_LEVEL:-15}
ionice_class=${TILE_GENERATION_IONICE_CLASS:-3}

if ! command -v nice >/dev/null 2>&1; then
  echo "nice is required to launch the tile generator safely." >&2
  exit 1
fi

command=(sbt)

if [[ $# -gt 0 ]]; then
  command+=("$@")
else
  command+=("runMain observatory.Main")
fi

if command -v ionice >/dev/null 2>&1; then
  exec ionice -c "$ionice_class" nice -n "$nice_level" "${command[@]}"
else
  echo "ionice is not available; continuing with nice only." >&2
  exec nice -n "$nice_level" "${command[@]}"
fi