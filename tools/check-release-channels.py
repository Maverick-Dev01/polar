#!/usr/bin/env python3
"""Prueba la selección de canales sin leer llaves reales ni compilar Android."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

source = Path(__file__).with_name("android-release.py")
for channel, expected in ((None, ["prepareGithubRelease", "preparePlayRelease"]),
                          ("github", ["prepareGithubRelease"]),
                          ("play", ["preparePlayRelease"])):
    with tempfile.TemporaryDirectory() as folder:
        root = Path(folder)
        (root / "tools").mkdir()
        shutil.copyfile(source, root / "tools/android-release.py")
        private = root / ".polar-signing"
        private.mkdir()
        (private / "signing.json").write_text(json.dumps({"alias": "polar", "password": "test-only"}))
        (private / "polar-release.jks").write_bytes(b"existing-test-key")
        android = root / "PolarAndroid"
        android.mkdir()
        wrapper = android / "gradlew"
        wrapper.write_text(f'#!{sys.executable}\nimport json, pathlib, sys\npathlib.Path("tasks.json").write_text(json.dumps(sys.argv[1:]))\n')
        wrapper.chmod(0o700)
        args = [sys.executable, str(root / "tools/android-release.py")]
        if channel:
            args += ["--channel", channel]
        result = subprocess.run(args + ["-PPOLAR_VERSION_NAME=2.3.0"], capture_output=True, text=True,
                                env=dict(os.environ, JAVA_HOME=str(root / "unused-jdk")))
        assert result.returncode == 0, result.stderr
        assert json.loads((android / "tasks.json").read_text()) == expected + ["-PPOLAR_VERSION_NAME=2.3.0"]
        assert (private / "polar-release.jks").read_bytes() == b"existing-test-key"
        assert "test-only" not in result.stdout + result.stderr

for option, code in (("--help", 0), ("--channel=invalid", 2)):
    with tempfile.TemporaryDirectory() as folder:
        root = Path(folder)
        (root / "tools").mkdir()
        shutil.copyfile(source, root / "tools/android-release.py")
        result = subprocess.run([sys.executable, str(root / "tools/android-release.py"), option],
                                capture_output=True, text=True)
        assert result.returncode == code, result.stderr
        assert not (root / ".polar-signing").exists()
print("OK: ambos canales, selección individual, ayuda y errores sin cambiar llaves.")
