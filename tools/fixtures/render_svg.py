#!/usr/bin/env python3
"""Genera docs/disenos-fase-3/<id>.svg a partir de shared-fixtures/estilos-geometria.json.
Sólo biblioteca estándar. Uso: python3 tools/fixtures/render_svg.py"""
import json, os, random
from xml.sax.saxutils import escape

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SRC = os.path.join(ROOT, "shared-fixtures", "estilos-geometria.json")
OUT = os.path.join(ROOT, "docs", "disenos-fase-3")

SAMPLE = {"TITLE": "Nuestro verano", "SUBTITLE": "Tú y yo", "CAPTION": "Una historia para guardar",
          "SONG": "Nuestra canción", "ARTIST": "Artista", "DATE": "8 oct 2026"}
SAMPLE_BY_STYLE = {("photobooth", "TITLE"): "FOTOMATÓN", ("washi", "CAPTION"): "qué buen día",
                   ("instaxWide", "TITLE"): "Playa, 2026", ("cassette", "ARTIST"): "Artista",
                   ("collage", "TITLE"): "Fin de semana"}
FAMILY = {"Gelasio": "Gelasio, Georgia, serif", "Caveat": "Caveat, 'Bradley Hand', 'Segoe Script', cursive",
          ".System": "-apple-system, 'Helvetica Neue', Arial, sans-serif"}
GRADS = [("#F2B8A0", "#7A293B"), ("#A9C6D8", "#3C5A72"), ("#F4D58D", "#C46A3D"), ("#B9D4B0", "#4C7A5A")]


def fmt(v):
    return ("%.2f" % v).rstrip("0").rstrip(".")


def shape_path(s, X, Y, Wp, Hp):
    """Elemento SVG (sin relleno) con la forma del hueco de foto, en px."""
    k = s.get("shape", "rect")
    if k == "ellipse":
        return '<ellipse cx="%s" cy="%s" rx="%s" ry="%s"' % (fmt(X + Wp / 2), fmt(Y + Hp / 2), fmt(Wp / 2), fmt(Hp / 2))
    rx = s.get("radius", 0) * min(Wp, Hp) if k == "round" else 0
    return '<rect x="%s" y="%s" width="%s" height="%s" rx="%s"' % (fmt(X), fmt(Y), fmt(Wp), fmt(Hp), fmt(rx))


def render(sid, st, ref):
    W = ref
    H = W / st["cardAspect"]
    sc = min(3.0, 640.0 / H, 540.0 / W) if H > W else min(3.0, 540.0 / W)
    pad = 24
    cw, ch = W * sc, H * sc
    tw, th = cw + 2 * pad, ch + 2 * pad
    o = ['<svg xmlns="http://www.w3.org/2000/svg" width="%s" height="%s" viewBox="0 0 %s %s">' % (fmt(tw), fmt(th), fmt(tw), fmt(th))]
    o.append('<rect width="100%" height="100%" fill="#E4DED5"/>')
    o.append('<defs>')
    for i, (a, b) in enumerate(GRADS):
        o.append('<linearGradient id="g%d" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="%s"/><stop offset="1" stop-color="%s"/></linearGradient>' % (i, a, b))
    o.append('<clipPath id="card"><rect x="%s" y="%s" width="%s" height="%s" rx="%s"/></clipPath>' % (pad, pad, fmt(cw), fmt(ch), fmt(1.5 * sc)))
    for i, s in enumerate(st["photoSlots"]):
        X, Y, Wp, Hp = pad + s["x"] * cw, pad + s["y"] * ch, s["w"] * cw, s["h"] * ch
        o.append('<clipPath id="p%d">%s/></clipPath>' % (i, shape_path(s, X, Y, Wp, Hp)))
    o.append('<filter id="sh" x="-5%" y="-5%" width="110%" height="110%"><feDropShadow dx="0" dy="2" stdDeviation="3" flood-opacity="0.18"/></filter></defs>')
    o.append('<rect x="%s" y="%s" width="%s" height="%s" rx="%s" fill="%s" filter="url(#sh)"/>' % (pad, pad, fmt(cw), fmt(ch), fmt(1.5 * sc), st["defaultBackground"]))
    o.append('<g clip-path="url(#card)">')

    def decos(layer):
        for d in st["decorations"]:
            if d["layer"] != layer:
                continue
            X, Y, Wd, Hd = pad + d["x"] * cw, pad + d["y"] * ch, d["w"] * cw, d["h"] * ch
            tr = ' transform="rotate(%s %s %s)"' % (fmt(d["rotationDeg"]), fmt(X + Wd / 2), fmt(Y + Hd / 2)) if d["rotationDeg"] else ""
            op = ' opacity="%s"' % d["opacity"]
            t = d["type"]
            if t == "rect":
                o.append('<rect x="%s" y="%s" width="%s" height="%s" fill="%s"%s%s/>' % (fmt(X), fmt(Y), fmt(Wd), fmt(Hd), d["color"], op, tr))
            elif t == "roundRect":
                o.append('<rect x="%s" y="%s" width="%s" height="%s" rx="%s" fill="%s"%s%s/>' % (fmt(X), fmt(Y), fmt(Wd), fmt(Hd), fmt(d["radius"] * min(Wd, Hd)), d["color"], op, tr))
            elif t == "ellipse":
                o.append('<ellipse cx="%s" cy="%s" rx="%s" ry="%s" fill="%s"%s%s/>' % (fmt(X + Wd / 2), fmt(Y + Hd / 2), fmt(Wd / 2), fmt(Hd / 2), d["color"], op, tr))
            elif t == "ring":
                o.append('<ellipse cx="%s" cy="%s" rx="%s" ry="%s" fill="none" stroke="%s" stroke-width="%s"%s%s/>' % (fmt(X + Wd / 2), fmt(Y + Hd / 2), fmt(Wd / 2), fmt(Hd / 2), d["color"], fmt(d["strokeW"] * cw), op, tr))
            elif t == "stripes":
                n = d["count"]
                bw = Wd / (2 * n - 1)
                o.append('<g%s%s>' % (op, tr))
                for i in range(n):
                    o.append('<rect x="%s" y="%s" width="%s" height="%s" fill="%s"/>' % (fmt(X + 2 * i * bw), fmt(Y), fmt(bw), fmt(Hd), d["color"]))
                o.append('</g>')

    decos("below")
    for i, s in enumerate(st["photoSlots"]):
        X, Y, Wp, Hp = pad + s["x"] * cw, pad + s["y"] * ch, s["w"] * cw, s["h"] * ch
        o.append('<g clip-path="url(#p%d)">' % i)
        o.append('<rect x="%s" y="%s" width="%s" height="%s" fill="url(#g%d)"/>' % (fmt(X), fmt(Y), fmt(Wp), fmt(Hp), i % 4))
        m = min(Wp, Hp)
        o.append('<circle cx="%s" cy="%s" r="%s" fill="#FFFFFF" opacity="0.55"/>' % (fmt(X + Wp * 0.72), fmt(Y + Hp * 0.3), fmt(m * 0.11)))
        o.append('<path d="M%s %s L%s %s L%s %s Z" fill="#000" opacity="0.22"/>' % (fmt(X + Wp * 0.05), fmt(Y + Hp), fmt(X + Wp * 0.38), fmt(Y + Hp * 0.52), fmt(X + Wp * 0.72), fmt(Y + Hp)))
        o.append('<path d="M%s %s L%s %s L%s %s Z" fill="#000" opacity="0.32"/>' % (fmt(X + Wp * 0.45), fmt(Y + Hp), fmt(X + Wp * 0.72), fmt(Y + Hp * 0.62), fmt(X + Wp * 1.0), fmt(Y + Hp)))
        o.append('</g>')
    decos("above")

    for t in st["textSlots"]:
        X, Y, Wt, Ht = pad + t["x"] * cw, pad + t["y"] * ch, t["w"] * cw, t["h"] * ch
        txt = SAMPLE_BY_STYLE.get((sid, t["role"]), SAMPLE[t["role"]])
        anchor = {"left": "start", "center": "middle", "right": "end"}[t["align"]]
        tx = {"left": X, "center": X + Wt / 2, "right": X + Wt}[t["align"]]
        size = t["defaultSizePt"] * sc * (W / ref)
        o.append('<rect x="%s" y="%s" width="%s" height="%s" fill="none" stroke="#00A0FF" stroke-width="0.5" stroke-dasharray="3 3" opacity="0.35" class="guide"/>' % (fmt(X), fmt(Y), fmt(Wt), fmt(Ht)))
        o.append('<text x="%s" y="%s" text-anchor="%s" dominant-baseline="central" font-family="%s" font-size="%s" font-weight="%s" fill="%s">%s</text>'
                 % (fmt(tx), fmt(Y + Ht / 2), anchor, FAMILY[t["defaultFont"]], fmt(size), "700" if t["bold"] else "400", t["color"], escape(txt)))

    q = st.get("qrSlot")
    if q:
        side = q["size"] * cw
        X, Y = pad + q["x"] * cw, pad + q["y"] * ch
        n = 11
        c = side / n
        rnd = random.Random(7)
        o.append('<rect x="%s" y="%s" width="%s" height="%s" fill="#fff"/>' % (fmt(X), fmt(Y), fmt(side), fmt(side)))
        def finder(fx, fy):
            o.append('<rect x="%s" y="%s" width="%s" height="%s" fill="#111"/>' % (fmt(X + fx * c), fmt(Y + fy * c), fmt(3 * c), fmt(3 * c)))
            o.append('<rect x="%s" y="%s" width="%s" height="%s" fill="#fff"/>' % (fmt(X + (fx + .5) * c), fmt(Y + (fy + .5) * c), fmt(2 * c), fmt(2 * c)))
            o.append('<rect x="%s" y="%s" width="%s" height="%s" fill="#111"/>' % (fmt(X + (fx + 1) * c), fmt(Y + (fy + 1) * c), fmt(c), fmt(c)))
        finder(0, 0); finder(n - 3, 0); finder(0, n - 3)
        for i in range(n):
            for j in range(n):
                if (i < 4 and j < 4) or (i > n - 5 and j < 4) or (i < 4 and j > n - 5):
                    continue
                if rnd.random() < 0.5:
                    o.append('<rect x="%s" y="%s" width="%s" height="%s" fill="#111"/>' % (fmt(X + i * c), fmt(Y + j * c), fmt(c), fmt(c)))
    o.append('</g></svg>')
    return "\n".join(o)


def main():
    doc = json.load(open(SRC, encoding="utf-8"))
    os.makedirs(OUT, exist_ok=True)
    for sid, st in doc["styles"].items():
        p = os.path.join(OUT, sid + ".svg")
        with open(p, "w", encoding="utf-8") as f:
            f.write(render(sid, st, doc["referenceCardWidthPt"]))
        print("escrito", os.path.relpath(p, ROOT))


if __name__ == "__main__":
    main()
