#!/usr/bin/env python3
"""Genera shared-fixtures/moldes/*.png y moldes.json (sólo biblioteca estándar).
Uso: python3 tools/fixtures/make_moldes.py"""
import json, os, struct, zlib

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "shared-fixtures", "moldes")
W, H = 1200, 1600


def write_png(path, w, h, px):  # px: bytearray RGBA
    raw = b"".join(b"\x00" + bytes(px[y * w * 4:(y + 1) * w * 4]) for y in range(h))
    def chunk(t, d):
        c = struct.pack(">I", len(d)) + t + d
        return c + struct.pack(">I", zlib.crc32(t + d) & 0xffffffff)
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
                + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def inside(shape, x, y, r):  # r = (x0,y0,x1,y1) px
    x0, y0, x1, y1 = r
    if not (x0 <= x < x1 and y0 <= y < y1):
        return False
    if shape == "rect":
        return True
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    px, py = x + .5, y + .5
    if shape == "ellipse":
        a, b = (x1 - x0) / 2, (y1 - y0) / 2
        return ((px - cx) / a) ** 2 + ((py - cy) / b) ** 2 <= 1
    rad = 0.2 * min(x1 - x0, y1 - y0)  # round
    dx = max(x0 + rad - px, 0, px - (x1 - rad))
    dy = max(y0 + rad - py, 0, py - (y1 - rad))
    return dx * dx + dy * dy <= rad * rad


def paint(bg, bars, holes, shape):
    px = bytearray(W * H * 4)
    for y in range(H):
        for x in range(W):
            i = (y * W + x) * 4
            c = bg
            for (bx0, by0, bx1, by1, bc) in bars:
                if bx0 <= x < bx1 and by0 <= y < by1:
                    c = bc
            for h in holes:
                if inside(shape, x, y, h):
                    px[i:i + 4] = b"\x00\x00\x00\x00"
                    break
            else:
                px[i:i + 4] = bytes(c + (255,))
    return px


def half(px, w, h):
    o = bytearray((w // 2) * (h // 2) * 4)
    for y in range(h // 2):
        for x in range(w // 2):
            acc = [0, 0, 0, 0]
            for dy in (0, 1):
                for dx in (0, 1):
                    j = ((2 * y + dy) * w + 2 * x + dx) * 4
                    for k in range(4):
                        acc[k] += px[j + k]
            i = (y * (w // 2) + x) * 4
            o[i:i + 4] = bytes(v // 4 for v in acc)
    return o


def dhash(px, w, h):
    """dHash 64 bits: compone sobre blanco, gris, caja 9x8 por promedio de área, compara vecinos."""
    gray = []
    for i in range(0, len(px), 4):
        a = px[i + 3] / 255
        r, g, b = (px[i + k] * a + 255 * (1 - a) for k in range(3))
        gray.append(0.299 * r + 0.587 * g + 0.114 * b)
    def weights(n, parts):
        """Para cada pixel 0..n-1, lista (celda, peso) con límites fraccionarios (promedio de área exacto)."""
        out = []
        for i in range(n):
            lo, hi = i * parts / n, (i + 1) * parts / n
            c = int(lo)
            ws = []
            while c < parts and c < hi:
                ws.append((c, (min(hi, c + 1) - max(lo, c)) / (hi - lo)))
                c += 1
            out.append(ws)
        return out
    wx, wy = weights(w, 9), weights(h, 8)
    sums = [0.0] * 72
    for y in range(h):
        row = y * w
        colsum = [0.0] * 9
        for x in range(w):
            v = gray[row + x]
            for c, wt in wx[x]:
                colsum[c] += v * wt
        for gy, wty in wy[y]:
            for gx in range(9):
                sums[gy * 9 + gx] += colsum[gx] * wty
    cells = [v / ((w / 9) * (h / 8)) for v in sums]
    bits = 0
    for gy in range(8):
        for gx in range(8):
            bits = (bits << 1) | (1 if cells[gy * 9 + gx] > cells[gy * 9 + gx + 1] + 1.0 else 0)
    return bits


def frac(r):
    return [round(r[0] / W, 4), round(r[1] / H, 4), round((r[2] - r[0]) / W, 4), round((r[3] - r[1]) / H, 4)]


def main():
    os.makedirs(OUT, exist_ok=True)
    wine, cream = (122, 41, 59), (247, 242, 235)
    bars = [(0, 1380, W, 1500, (242, 221, 225)), (100, 1410, 700, 1470, (43, 34, 33))]  # franja con "texto" opaco
    # a) rectángulos: grande arriba + 2 pequeños
    rects = [(96, 96, 1104, 696), (96, 760, 576, 1260), (624, 760, 1104, 1260)]
    # b) mismos huecos redondeados (radio 20 % del lado menor)
    # c) círculos inscritos en cuadrados
    circles = [(300, 90, 900, 690), (90, 760, 590, 1260), (610, 760, 1110, 1260)]
    meta = {"imageSize": [W, H], "notes": [
        "Fondo opaco de color; los huecos son transparentes (alfa 0). La franja inferior rosa y la barra oscura son opacas y NO son huecos.",
        "regions = [x, y, w, h] como fracción del ancho/alto de la imagen; radius = fracción del lado menor del hueco en px (20 % en 'round').",
        "Con tolerancia 0.01 en las fracciones se espera que la detección devuelva estas regiones, en orden de lectura (arriba-abajo, izquierda-derecha) y con la forma indicada."],
        "templates": {}, "dhash": {}}
    specs = [("rects", "rect", rects, wine), ("rounded", "round", rects, wine), ("circles", "ellipse", circles, cream)]
    hashes = {}
    for name, shape, holes, bg in specs:
        px = paint(bg, bars if bg == wine else [(0, 1380, W, 1500, (242, 221, 225)), (100, 1410, 700, 1470, (122, 41, 59))], holes, shape)
        fn = "molde-%s.png" % name
        write_png(os.path.join(OUT, fn), W, H, px)
        meta["templates"][fn] = {"shape": shape, "radius": 0.2 if shape == "round" else 0.0,
                                 "regions": [{"rect": frac(r), "shape": shape, "radius": 0.2 if shape == "round" else 0.0} for r in holes]}
        hashes[name] = (px, dhash(px, W, H))
    # Casi duplicado: la plantilla de rects reescalada al 50 %
    px = hashes["rects"][0]
    small = half(px, W, H)
    write_png(os.path.join(OUT, "molde-rects-50.png"), W // 2, H // 2, small)
    h_small = dhash(small, W // 2, H // 2)
    # Distinto: plantilla de tira vertical de 4 huecos con otra distribución y fondo
    strip = [(300, 60 + i * 370, 900, 60 + i * 370 + 340) for i in range(4)]
    px_b = paint((43, 34, 33), [(0, 1540, W, 1600, (247, 242, 235))], strip, "rect")
    write_png(os.path.join(OUT, "molde-distinto-tira.png"), W, H, px_b)
    h_b = dhash(px_b, W, H)
    meta["templates"]["molde-rects-50.png"] = {"sameAs": "molde-rects.png", "imageSize": [W // 2, H // 2]}
    meta["templates"]["molde-distinto-tira.png"] = {"shape": "rect", "radius": 0.0,
        "regions": [{"rect": frac(r), "shape": "rect", "radius": 0.0} for r in strip]}
    hr = hashes["rects"][1]
    d = lambda a, b: bin(a ^ b).count("1")
    meta["dhash"] = {
        "algorithm": "dHash 64 bits: componer sobre blanco, gris (0.299R+0.587G+0.114B), reducir a 9x8 por promedio de área exacto (límites fraccionarios), bit = celda[x] > celda[x+1] + 1.0 (tolerancia de 1 nivel de gris contra empates); primer bit = MSB, filas de arriba a abajo.",
        "values": {"molde-rects.png": "%016x" % hr, "molde-rects-50.png": "%016x" % h_small,
                   "molde-rounded.png": "%016x" % hashes["rounded"][1], "molde-circles.png": "%016x" % hashes["circles"][1],
                   "molde-distinto-tira.png": "%016x" % h_b},
        "expected": {
            "molde-rects.png vs molde-rects-50.png": {"hamming": d(hr, h_small), "rule": "<= 6 -> duplicado"},
            "molde-rects.png vs molde-distinto-tira.png": {"hamming": d(hr, h_b), "rule": "> 6 -> distinto"},
            "molde-rects.png vs molde-rounded.png": {"hamming": d(hr, hashes["rounded"][1]), "note": "misma plantilla con huecos redondeados: puede ser parecido (<= 6); el usuario decide"}},
        "note": "Los valores son de referencia del script; una implementación con otro remuestreo (bilineal) puede diferir unos bits, por eso la regla es el umbral (<= 6), no igualdad exacta."}
    assert d(hr, h_small) <= 6, d(hr, h_small)
    assert d(hr, h_b) > 6, d(hr, h_b)
    with open(os.path.join(OUT, "moldes.json"), "w", encoding="utf-8") as f:
        json.dump(meta, f, ensure_ascii=False, indent=2)
    print(json.dumps(meta["dhash"]["expected"], indent=1))


if __name__ == "__main__":
    main()
