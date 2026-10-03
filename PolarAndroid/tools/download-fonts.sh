#!/usr/bin/env bash
# Descarga las fuentes de Polar desde github.com/google/fonts con su licencia.
set -euo pipefail
cd "$(dirname "$0")/.."
RES=app/src/main/res/font
LIC=app/src/main/assets/licenses
AST=app/src/main/assets/fonts
mkdir -p "$RES" "$LIC" "$AST"

FONTS=(
  "ofl/caveat|caveat" "ofl/kalam|kalam" "apache/homemadeapple|homemade_apple" "ofl/sacramento|sacramento"
  "ofl/dancingscript|dancing_script" "ofl/patrickhand|patrick_hand" "ofl/shadowsintolight|shadows_into_light"
  "ofl/amaticsc|amatic_sc" "ofl/gelasio|gelasio" "ofl/librebaskerville|libre_baskerville" "ofl/ebgaramond|eb_garamond"
  "ofl/josefinsans|josefin_sans" "ofl/nunito|nunito" "ofl/quicksand|quicksand" "ofl/montserrat|montserrat"
  "apache/specialelite|special_elite" "ofl/courierprime|courier_prime" "ofl/abrilfatface|abril_fatface"
  "ofl/pacifico|pacifico" "ofl/lobster|lobster"
)

pick_ttf='import json,sys
files=[f for f in json.load(sys.stdin) if f["name"].endswith(".ttf")]
upright=[f for f in files if "Italic" not in f["name"]] or files
upright.sort(key=lambda f:(0 if ("Regular" in f["name"] or "[" in f["name"]) else 1, len(f["name"])))
print(upright[0]["download_url"])'
pick_lic='import json,sys
print([f["download_url"] for f in json.load(sys.stdin) if f["name"] in ("OFL.txt","LICENSE.txt")][0])'

for entry in "${FONTS[@]}"; do
  dir="${entry%%|*}"; res="${entry##*|}"
  listing=$(curl -fsSL "https://api.github.com/repos/google/fonts/contents/$dir")
  curl -fsSL "$(printf '%s' "$listing" | python3 -c "$pick_ttf")" -o "$RES/$res.ttf"
  curl -fsSL "$(printf '%s' "$listing" | python3 -c "$pick_lic")" -o "$LIC/$res.txt"
  echo "ok  $res  $(du -h "$RES/$res.ttf" | cut -f1)"
done

# Fuentes variables cuyo eje de grosor arranca en delgado: se cargan desde assets
# con Typeface.Builder para fijar el grosor (400 / 700) en vez de usar el valor por defecto.
for name in josefin_sans montserrat nunito quicksand; do
  mv "$RES/$name.ttf" "$AST/$name.ttf"
done
