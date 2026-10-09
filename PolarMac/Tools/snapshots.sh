#!/bin/zsh
# Capturas fuera de pantalla de la guía (recorrido, modo «?», centro de ayuda, artículo) en claro y oscuro.
# No abre Polar.app: compila un ejecutable aparte que dibuja ventanas sin mostrarlas. Salida: docs/capturas/fase-3/sub4/mac/
set -eu
BASE_DIR="$(cd -- "$(dirname -- "$0")/.." && pwd)"
OUT="${1:-$BASE_DIR/../docs/capturas/fase-3/sub4/mac}"
WORK="$BASE_DIR/.build/snapshots"
mkdir -p "$WORK/src" "$OUT"
rm -f "$WORK/src"/*.swift
for f in "$BASE_DIR"/Sources/*.swift; do cp "$f" "$WORK/src/"; done
sed -i '' 's/^@main @MainActor struct PolarApp/@MainActor struct PolarAppUnused/' "$WORK/src/PolarApp.swift"
cp "$BASE_DIR/Tools/Snapshots.swift" "$WORK/src/main.swift"
xcrun swiftc -swift-version 5 -module-cache-path /private/tmp/polar-module-cache "$WORK"/src/*.swift -o "$WORK/snapshots"
export POLAR_HELP_JSON="$BASE_DIR/../shared-fixtures/help.json"
export POLAR_GEOMETRY_JSON="$BASE_DIR/../shared-fixtures/estilos-geometria.json"
export POLAR_FONT_DIRS="$BASE_DIR/../PolarAndroid/app/src/main/res/font:$BASE_DIR/../PolarAndroid/app/src/main/assets/fonts"
"$WORK/snapshots" "$OUT"
print "Capturas en $OUT"
