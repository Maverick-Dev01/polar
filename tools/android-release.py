#!/usr/bin/env python3
"""Conserva la firma privada y prepara APK GitHub y AAB Play. Sin dependencias."""
import argparse
import json
import os
from pathlib import Path
import secrets
import shutil
import subprocess
import sys

parser = argparse.ArgumentParser(description="Preparar las distribuciones Android firmadas de Polar.")
parser.add_argument("--channel", choices=("both", "github", "play"), default="both",
                    help="Canal de distribución (por defecto: ambos). Los demás argumentos pasan a Gradle.")
options, gradle_args = parser.parse_known_args()

root = Path(__file__).resolve().parent.parent
private = root / ".polar-signing"
config = private / "signing.json"
keystore = private / "polar-release.jks"
jdk = Path(os.environ["JAVA_HOME"]) if os.environ.get("JAVA_HOME") else None
suffix = ".exe" if os.name == "nt" else ""
keytool = str(jdk / "bin" / ("keytool" + suffix)) if jdk else shutil.which("keytool")
if not keytool:
    sys.exit("Configura JAVA_HOME con JDK 21 antes de preparar el release.")
private.mkdir(mode=0o700, exist_ok=True)
if config.exists() and not keystore.exists():
    sys.exit("Falta la llave privada existente. Recupera toda la carpeta .polar-signing; no se generará otra firma.")
if not config.exists():
    if keystore.exists():
        sys.exit("Hay una llave sin signing.json. Recupera su configuración; no se reemplazará.")
    password = secrets.token_urlsafe(48)
    configuration = {"alias": "polar", "password": password}
    # Guardar primero permite recuperar la contraseña si se interrumpe keytool.
    with config.open("x", encoding="utf-8") as target:
        json.dump(configuration, target)
    config.chmod(0o600)
else:
    configuration = json.loads(config.read_text(encoding="utf-8"))
environment = dict(os.environ, POLAR_SIGNING_PASSWORD=configuration["password"])
if not keystore.exists():
    subprocess.run([keytool, "-genkeypair", "-keystore", str(keystore), "-storetype", "JKS",
        "-alias", configuration["alias"], "-keyalg", "RSA", "-keysize", "4096", "-validity", "10000",
        "-dname", "CN=Polar, OU=Android, O=Maverick-Dev01, C=MX",
        "-storepass:env", "POLAR_SIGNING_PASSWORD", "-keypass:env", "POLAR_SIGNING_PASSWORD"],
        env=environment, check=True)
    keystore.chmod(0o600)
environment.update(POLAR_STORE_FILE=str(keystore), POLAR_STORE_PASSWORD=configuration["password"],
    POLAR_KEY_ALIAS=configuration["alias"], POLAR_KEY_PASSWORD=configuration["password"])
wrapper = root / "PolarAndroid" / ("gradlew.bat" if os.name == "nt" else "gradlew")
tasks = {"both": ["prepareGithubRelease", "preparePlayRelease"],
         "github": ["prepareGithubRelease"], "play": ["preparePlayRelease"]}[options.channel]
subprocess.run([str(wrapper), *tasks, *gradle_args], cwd=wrapper.parent, env=environment, check=True)
print("Assets listos en release-assets/. La firma privada permanece en .polar-signing/; conserva ambos archivos de firma para otros equipos.")
