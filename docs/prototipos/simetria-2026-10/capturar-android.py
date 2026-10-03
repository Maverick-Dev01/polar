"""Capturas nativas; --size captura un AVD de tamaño fijo sin modificar el display.
Copiar temporalmente el host de antes/después como SymmetryAuditCaptureTest.kt,
añadir auditoria.jpg en androidTest/assets y compilar/instalar ambos APK.
Retirar host y foto al terminar. El host usa biblioteca aislada.
"""
from pathlib import Path
import argparse
import subprocess

ROOT = Path(__file__).resolve().parents[3]
ADB = str(Path.home() / "Library/Android/sdk/platform-tools/adb")
PACKAGE = "io.github.maverickdev01.polar.debug"
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--serial", default="emulator-5554")
parser.add_argument("--phase", choices=["antes", "despues"], default="despues")
parser.add_argument("--size", choices=["360x640", "411x891", "1280x800"])
args = parser.parse_args()
DEST = ROOT / "docs/capturas/simetria" / args.phase
DEST.mkdir(parents=True, exist_ok=True)

def adb(*items, binary=False):
    return subprocess.check_output([ADB, "-s", args.serial, *items], text=not binary)

def current(kind):
    lines = adb("shell", "wm", kind).strip().splitlines()
    return lines[-1].split(":", 1)[1].strip() if len(lines) > 1 else "reset"

saved = None if args.size else (current("size"), current("density"))
try:
    for size in [args.size] if args.size else ["360x640", "411x891", "1280x800"]:
        if not args.size:
            adb("shell", "wm", "density", "160")
            adb("shell", "wm", "size", size)
        result = adb("shell", "am", "instrument", "-w", "-e", "class", "com.polar.app.SymmetryAuditCaptureTest", "-e", "auditSize", size, "-e", "auditPhase", args.phase, PACKAGE + ".test/androidx.test.runner.AndroidJUnitRunner")
        (DEST / f"android-{size}.txt").write_text(result.rstrip() + "\n")
        print(result, flush=True)
        if "OK (1 test)" not in result or "FAILURES" in result or "INSTRUMENTATION_FAILED" in result:
            raise RuntimeError("La captura Android no terminó correctamente")
        prefix = f"{args.phase}-android-{size}-"
        names = [n for n in adb("shell", "run-as", PACKAGE, "ls", "cache/symmetry-audit").splitlines() if n.startswith(prefix) and n.endswith(".png")]
        expected = 44 if args.phase == "antes" else 72
        if len(names) != expected:
            raise RuntimeError(f"{size}: {len(names)} capturas; se esperaban {expected}")
        for name in names:
            (DEST / name).write_bytes(adb("exec-out", "run-as", PACKAGE, "cat", "cache/symmetry-audit/" + name, binary=True))
        print(f"Guardadas {len(names)} capturas de {size}", flush=True)
finally:
    if saved:
        adb("shell", "wm", "size", saved[0])
        adb("shell", "wm", "density", saved[1])
        print("Tamaño y densidad restaurados", flush=True)
