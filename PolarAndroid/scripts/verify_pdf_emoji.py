"""Verifica el PDF del escenario sintético DeviceSmokeTest, con fotos azules/amarillas y ♥."""
import subprocess
import sys
import tempfile
from pathlib import Path
from PIL import Image


def verify(pdf):
    with tempfile.TemporaryDirectory() as folder:
        target = Path(folder) / "page"
        subprocess.run(["pdftoppm", "-f", "1", "-l", "1", "-r", "144", "-singlefile", "-png", str(pdf), str(target)], check=True)
        with Image.open(target.with_suffix(".png")) as image:
            rgb = image.convert("RGB").tobytes()
            red = sum(rgb[i] > 220 and rgb[i + 1] < 100 and rgb[i + 2] < 100 for i in range(0, len(rgb), 3))
        assert red > 50, f"El corazón desapareció en el visor PDF externo: {red} píxeles rojos"
        print(f"PDF conserva el corazón en el visor externo: {red} píxeles rojos")


if __name__ == "__main__":
    verify(Path(sys.argv[1]))
