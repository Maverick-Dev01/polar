"""Compila hosts temporales de las vistas actuales, con letra al 100/130 %.
El 130 % escala las fuentes de las copias de prueba, nunca las fuentes del
renderer ni los contenedores. No toca Polar.app ni la biblioteca del usuario.
"""
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[3]
HERE = Path(__file__).resolve().parent
OUTPUT = ROOT / "docs/capturas/simetria/antes"
OUTPUT.mkdir(parents=True, exist_ok=True)
SOURCES = ROOT / "PolarMac/Sources"
common = [SOURCES / (name + ".swift") for name in ["Models", "PhotoImporter", "TemplateImport", "Renderer", "Studio", "LibraryStore", "FontCatalog"]]
named = {"largeTitle": 26, "title": 22, "title2": 17, "title3": 15, "headline": 13, "body": 13, "callout": 12, "subheadline": 11, "footnote": 10, "caption": 10, "caption2": 10}
for percent in [100, 130]:
    temp = Path(f"/private/tmp/polar-mac-audit-source-{percent}")
    temp.mkdir(exist_ok=True)
    views = []
    for filename in ["PolarApp.swift", "LibraryViews.swift"]:
        text = (SOURCES / filename).read_text()
        if filename == "PolarApp.swift":
            text = text[:text.index("@main @MainActor struct PolarApp")] + text[text.index("@MainActor struct StudioView"):]
        if percent == 130:
            # Sólo las copias del host: escala tamaños de UI explícitos y estilos semánticos.
            text = re.sub(r"(size:\s*)([0-9]+(?:\.[0-9]+)?)", lambda m: m[1] + str(float(m[2]) * 1.3), text)
            text = re.sub(r"\.font\(\.(largeTitle|title2|title3|title|headline|body|callout|subheadline|footnote|caption2|caption)(\.bold\(\))?\)", lambda m: ".font(.system(size: " + str(named[m[1]] * 1.3) + (", weight: .bold" if m[2] or m[1] == "headline" else "") + "))", text)
        dest = temp / filename
        dest.write_text(text)
        views.append(dest)
    executable = temp / "capture"
    subprocess.run(["xcrun", "swiftc", "-swift-version", "5", "-module-cache-path", "/private/tmp/polar-module-cache", *map(str, common + views), str(HERE / "MacAuditSnapshots.swift"), "-o", str(executable)], check=True)
    subprocess.run([str(executable), str(ROOT), str(OUTPUT), str(percent)], check=True)
