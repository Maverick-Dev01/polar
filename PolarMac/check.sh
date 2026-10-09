#!/bin/zsh
set -eu
BASE_DIR="$(cd -- "$(dirname -- "$0")" && pwd)"
cd "$BASE_DIR"
mkdir -p .build
export POLAR_GEOMETRY_JSON="$BASE_DIR/../shared-fixtures/estilos-geometria.json"
export POLAR_FIXTURES_DIR="$BASE_DIR/../shared-fixtures"
export POLAR_FONT_DIRS="$BASE_DIR/../PolarAndroid/app/src/main/res/font:$BASE_DIR/../PolarAndroid/app/src/main/assets/fonts"
sources=(Sources/Models.swift Sources/StyleGeometry.swift Sources/MoldLibrary.swift Sources/MoldWizard.swift Sources/PhotoLook.swift Sources/PhotoStudio.swift Sources/TemplateImport.swift Sources/PhotoImporter.swift Sources/Renderer.swift Sources/FontCatalog.swift Sources/LibraryStore.swift Sources/Studio.swift)
for test in Tests/*Checks.swift; do
    executable=".build/check-${test:t:r}"
    xcrun swiftc -swift-version 5 -module-cache-path /private/tmp/polar-module-cache "${sources[@]}" "$test" -o "$executable"
    "$executable"
done
