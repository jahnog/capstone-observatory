#!/bin/bash

set -euo pipefail

script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
cd "$script_dir"

nice_level=${TILE_GENERATION_NICE_LEVEL:-15}
ionice_class=${TILE_GENERATION_IONICE_CLASS:-3}
ionice_level=${TILE_GENERATION_IONICE_LEVEL:-7}
max_threads=${TILE_GENERATION_MAX_THREADS:-2}

if ! command -v nice >/dev/null 2>&1; then
  echo "nice is required to launch the tile generator safely." >&2
  exit 1
fi

if ! command -v ionice >/dev/null 2>&1; then
  echo "ionice is required to launch the tile generator safely." >&2
  exit 1
fi

if ! command -v renice >/dev/null 2>&1; then
  echo "renice is required to launch the tile generator safely." >&2
  exit 1
fi

command=(sbt)

if [[ $# -gt 0 ]]; then
  command+=("$@")
else
  command+=("runMain observatory.Main")
fi

export TILE_GENERATION_LOW_PRIORITY_APPLIED=1
export TILE_GENERATION_IONICE_LEVEL="$ionice_level"
export TILE_GENERATION_MAX_THREADS="$max_threads"

java_tool_options="-XX:ActiveProcessorCount=${max_threads}"
if [[ -n "${JAVA_TOOL_OPTIONS:-}" ]]; then
  export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS} ${java_tool_options}"
else
  export JAVA_TOOL_OPTIONS="${java_tool_options}"
fi

if [[ "$ionice_class" == "3" ]]; then
  exec ionice -c "$ionice_class" nice -n "$nice_level" "${command[@]}"
else
  exec ionice -c "$ionice_class" -n "$ionice_level" nice -n "$nice_level" "${command[@]}"
fi