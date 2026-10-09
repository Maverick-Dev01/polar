#!/bin/zsh
# Capturas fuera de pantalla de las pantallas de Polar con datos demo (sin abrir la app real).
# Uso: Tools/snapshots.sh [carpeta de salida] [filtro]   (por defecto docs/capturas/fase-3/sub2/mac)
set -eu
BASE_DIR="$(cd -- "$(dirname -- "$0")/.." && pwd)"
OUT="${1:-$BASE_DIR/../docs/capturas/fase-3/sub2/mac}"
WORK="$BASE_DIR/.build/snapshots-src"
rm -rf "$WORK"; mkdir -p "$WORK" "$BASE_DIR/.build"
for f in "$BASE_DIR"/Sources/*.swift; do sed 's/^@main //' "$f" > "$WORK/${f:t}"; done
cp "$BASE_DIR/Tools/SnapshotMain.swift" "$WORK/main.swift"
export POLAR_FONT_DIRS="$BASE_DIR/../PolarAndroid/app/src/main/res/font:$BASE_DIR/../PolarAndroid/app/src/main/assets/fonts"
xcrun swiftc -swift-version 5 -module-cache-path /private/tmp/polar-module-cache "$WORK"/*.swift -o "$BASE_DIR/.build/polar-snapshots"
"$BASE_DIR/.build/polar-snapshots" "$OUT" "${2:-}"
