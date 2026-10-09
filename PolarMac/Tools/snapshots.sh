#!/bin/zsh
# Capturas fuera de pantalla de las pantallas de Polar con datos demo (sin abrir la app real).
# Uso: Tools/snapshots.sh [carpeta de salida] [filtro]   (por defecto docs/capturas/fase-3/sub3/mac)
set -eu
BASE_DIR="$(cd -- "$(dirname -- "$0")/.." && pwd)"
OUT="${1:-$BASE_DIR/../docs/capturas/fase-3/sub3/mac}"
WORK="$BASE_DIR/.build/snapshots-src"
rm -rf "$WORK"; mkdir -p "$WORK" "$BASE_DIR/.build"
for f in "$BASE_DIR"/Sources/*.swift; do sed 's/^@main //' "$f" > "$WORK/${f:t}"; done
cp "$BASE_DIR/Tools/SnapshotMain.swift" "$WORK/main.swift"
export POLAR_GEOMETRY_JSON="$BASE_DIR/../shared-fixtures/estilos-geometria.json"
export POLAR_FIXTURES_DIR="$BASE_DIR/../shared-fixtures"
export POLAR_FONT_DIRS="$BASE_DIR/../PolarAndroid/app/src/main/res/font:$BASE_DIR/../PolarAndroid/app/src/main/assets/fonts"
xcrun swiftc -swift-version 5 -module-cache-path /private/tmp/polar-module-cache "$WORK"/*.swift -o "$BASE_DIR/.build/polar-snapshots"
"$BASE_DIR/.build/polar-snapshots" "$OUT" "${2:-}"
