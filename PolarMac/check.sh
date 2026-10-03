#!/bin/zsh
set -eu
BASE_DIR="$(cd -- "$(dirname -- "$0")" && pwd)"
cd "$BASE_DIR"
mkdir -p .build
sources=(Sources/Models.swift Sources/PhotoLook.swift Sources/TemplateImport.swift Sources/PhotoImporter.swift Sources/Renderer.swift Sources/FontCatalog.swift Sources/LibraryStore.swift Sources/Studio.swift)
for test in Tests/*Checks.swift; do
    executable=".build/check-${test:t:r}"
    xcrun swiftc -swift-version 5 -module-cache-path /private/tmp/polar-module-cache "${sources[@]}" "$test" -o "$executable"
    "$executable"
done
