#!/usr/bin/env python3
"""Comprueba que una copia incompleta de la firma no genere una llave nueva."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

source = Path(__file__).with_name("android-release.py")
for missing in ("key", "config"):
    with tempfile.TemporaryDirectory() as folder:
        root = Path(folder)
        (root / "tools").mkdir()
        script = root / "tools/android-release.py"
        shutil.copyfile(source, script)
        private = root / ".polar-signing"
        private.mkdir()
        if missing == "key":
            config = private / "signing.json"
            config.write_text(json.dumps({"alias": "polar", "password": "test-only"}))
        else:
            (private / "polar-release.jks").write_bytes(b"test-only")
        result = subprocess.run([sys.executable, str(script)], capture_output=True, text=True,
            env=dict(os.environ, JAVA_HOME=str(root / "unused-jdk")))
        assert result.returncode != 0
        assert ("Recupera" if missing == "key" else "no se reemplazará") in result.stderr
        assert len(list(private.iterdir())) == 1
print("OK: una firma incompleta se rechaza sin regenerar ni reemplazar la llave.")
