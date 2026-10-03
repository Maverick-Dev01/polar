# Filtros de Polar · contrato implementado

2 de octubre de 2026. **Implementado en Android y Mac 2.1.0**, tras aprobar el prototipo. Estas matrices son el contrato de los motores nativos y sus pruebas compartidas. El HTML conserva filtros CSS ilustrativos; las apps usan los renderers de producción.

## Representación y presets

Vector columna `[R, G, B, A, 1]`, canales no premultiplicados de 0 a 255, en el mismo espacio sRGB de trabajo en ambos motores. Cada fila produce un canal. Las constantes de la última columna están en unidades de canal de 8 bits. Core Image usa biases divididos entre 255 y vectores RGB normalizados; hay que controlar su espacio de trabajo para que no cambie esta matemática. El alfa se conserva. No se modifica la fuente ni se filtran marcos/textos.

Los pesos Rec.709 indicados por el encargo son 0.2126, 0.7152 y 0.0722. Para igualar ColorMatrix de Android se aplican sobre los canales sRGB codificados, sin una conversión gamma adicional entre pasos.

### `original` · Original

```text
   1.000000    0.000000    0.000000    0.000000    0.000000
   0.000000    1.000000    0.000000    0.000000    0.000000
   0.000000    0.000000    1.000000    0.000000    0.000000
   0.000000    0.000000    0.000000    1.000000    0.000000
```

### `bw` · Blanco y negro

```text
   0.212600    0.715200    0.072200    0.000000    0.000000
   0.212600    0.715200    0.072200    0.000000    0.000000
   0.212600    0.715200    0.072200    0.000000    0.000000
   0.000000    0.000000    0.000000    1.000000    0.000000
```

### `film` · Película

```text
   0.244490    0.822480    0.083030    0.000000  -19.125000
   0.244490    0.822480    0.083030    0.000000  -19.125000
   0.244490    0.822480    0.083030    0.000000  -19.125000
   0.000000    0.000000    0.000000    1.000000    0.000000
```

### `sepia` · Sepia

```text
   0.393000    0.769000    0.189000    0.000000    0.000000
   0.349000    0.686000    0.168000    0.000000    0.000000
   0.272000    0.534000    0.131000    0.000000    0.000000
   0.000000    0.000000    0.000000    1.000000    0.000000
```

### `warm` · Cálido

```text
   1.039370   -0.035760   -0.003610    0.000000   12.000000
  -0.010630    1.014240   -0.003610    0.000000    0.000000
  -0.010630   -0.035760    1.046390    0.000000  -12.000000
   0.000000    0.000000    0.000000    1.000000    0.000000
```

### `cool` · Frío

```text
   1.000000    0.000000    0.000000    0.000000  -12.000000
   0.000000    1.000000    0.000000    0.000000    0.000000
   0.000000    0.000000    1.000000    0.000000   12.000000
   0.000000    0.000000    0.000000    1.000000    0.000000
```

### `faded` · Desvanecido

```text
   0.687402    0.193104    0.019494    0.000000   16.000000
   0.057402    0.823104    0.019494    0.000000   16.000000
   0.057402    0.193104    0.649494    0.000000   16.000000
   0.000000    0.000000    0.000000    1.000000    0.000000
```

### `vivid` · Vivo

```text
   1.316535   -0.196680   -0.019855    0.000000  -12.750000
  -0.058465    1.178320   -0.019855    0.000000  -12.750000
  -0.058465   -0.196680    1.355145    0.000000  -12.750000
   0.000000    0.000000    0.000000    1.000000    0.000000
```

Original = identidad. Blanco y negro = pesos indicados. Película = blanco y negro, contraste1.15 alrededor de127.5 y grano base0.25. Sepia = matriz clásica. Cálido = saturación1.05, rojo+12 y azul−12. Frío = rojo−12 y azul+12. Desvanecido = saturación0.70, escala0.90 y negros+16. Vivo = saturación1.25 y contraste1.10 alrededor de127.5. Las magnitudes que no especifica el encargo son decisiones de implementación; cambiarlas después afecta el resultado de proyectos que ya usen esos IDs, por eso se conserva este contrato al publicar.

## Intensidad y ajustes

`M_preset = (1 − intensidad) × I + intensidad × P`, intensidad entre0 y1. Con intensidad0 y ajustes neutros, incluso Película es identidad y no aporta grano.

Componer **sin redondeos ni clamp intermedios**, en este orden: preset → luz → contraste → calidez. Se aplica la matriz compuesta a la foto y luego se limita el resultado a0–255.

- Luz `t` de−1…1: identidad con bias `64 × t` en R/G/B.
- Contraste `c` de−1…1: diagonal RGB `f = 1 + 0.5 × c`, bias RGB `127.5 × (1 − f)`.
- Calidez `w` de−1…1: identidad, bias R `20 × w`, bias B `−20 × w`.
- Alfa: siempre `[0,0,0,1,0]`.

Matriz final `W × C × L × M_preset`. Esto permite un solo ColorMatrixColorFilter en Android y una transformación equivalente en Mac. La composición se comprueba con píxeles reales en ambos motores; el orden no se deduce de una cadena de filtros CSS.

## Grano idéntico a todas las resoluciones

Contrato usado en ambos motores, sin generador aleatorio dependiente del dispositivo:

1. Semilla FNV-1a de32 bits sobre UTF-8 de `assetID.uppercase() + ":" + índiceDecimalDeTarjeta`; offset2166136261, multiplicador16777619; XOR byte y multiplicación con desbordamiento UInt32. Los UUID ASCII se normalizan con la misma regla.
2. Cuadrícula de1pt en coordenadas de la tarjeta, origen arriba a la izquierda. Celda `x=floor(x_pt)`, `y=floor(y_pt)`. No usar el píxel de pantalla, tamaño del bitmap ni zoom como semilla.
3. `h = seed XOR (UInt32(x) × 0x9E3779B9) XOR (UInt32(y) × 0x85EBCA6B)`. Después `h ^= h >> 16; h *= 0x7FEB352D; h ^= h >> 15; h *= 0x846CA68B; h ^= h >> 16`, con desbordamiento de32 bits en cada operación.
4. `u = (h >> 24) / 255.0`. Fuerza `g = clamp(grain + (preset == film ? 0.25 × intensidad : 0), 0, 1)`. Delta `floor((2 × u − 1) × 8 × g + 0.5)`. Sumar el mismo delta a R/G/B; conservar alfa y limitar a0–255. Las mismas celdas se dibujan dentro del clip de la foto al exportar.

El grano de una tira comparte índice de tarjeta pero utiliza el ID de cada foto. Este contrato asegura determinismo espacial; la rasterización y compresión se deben contrastar, sin prometer que dos JPEG distintos tengan bytes idénticos.

## Modelo y resolución

`PhotoLook(preset="original", intensity=1, light=0, contrast=0, warmth=0, grain=0)`, inmutable y neutral. Campos opcionales en `settings.photoLook`, `cardOverrides[índice].photoLook` y `placements[índice].photoLook`. Resolver puro: **colocación > tarjeta > general > neutral**. Una foto individual de una tira tiene su propio look sin afectar las otras cuatro. Preset desconocido→original; números finitos con los rangos anteriores, grano0…1. Los campos ausentes de proyectos anteriores toman esos defaults. No se agregan roles a `textStyles`.

Alcances: proyecto completo; página actual; selección de una o varias páginas; foto individual. Página(s) materializa el look en todas sus tarjetas, incluso vacías, y limpia los looks de colocaciones sólo en esas páginas. Aplicar a todas guarda el look general y elimina los looks de tarjetas y colocaciones, conservando texto, fechas y encuadres; ofrece Deshacer. Quitar filtro escribe un objeto neutral en el alcance elegido, en lugar de heredar un filtro general anterior. Al cambiar la cantidad de fotos por tarjeta se materializan los looks efectivos en las colocaciones antes del remapeo.

`PhotoPlacement` conserva zoom/offsets/quarterTurns y añade únicamente el campo opcional de look. Zoom válido: finito, `>0` y `<=4`; Ajustar calcula su mínimo por proporción en PhotoFit, que puede ser menor de1. Las apps anteriores que exigían1…4 rechazan esos encuadres; se deben actualizar juntas. El formato conserva versión1, segundos Swift desde2001 y UUID en mayúsculas.

El raster de grano usa el centro de cada píxel de la capa: `originX + (x+0.5) × rect.width / layer.width` y equivalente Y. Las dimensiones se redondean hacia arriba, por eso se usa un factor por eje. Primero se aplica la matriz completa, se redondea/limita a8 bits y luego se suma el ruido; en PDF la capa filtrada usa300ppp. Marcos y texto se dibujan aparte. El sobrante atenuado de Encuadre es sólo una guía visual; no se exporta.

## Comprobaciones ejecutadas

- Resolver, defaults, desconocidos, rangos, ida/vuelta, fechas Swift/UUID y fixtures `.polar` cruzados.
- Intensidad0 neutra, composición y grano determinista en dos renders; muestrear la misma celda de tarjeta a distintas escalas.
- Una imagen sRGB de cinco colores sin perfil adicional para ambos renderers; tolerancia±1 respecto a la matriz y±2 entre plataformas.
- BW en área de fotos PDF/PNG: R=G=B±1; marcos y textos conservan color. Vacíos siguen sin imprimirse.
- Alcance, aplicar a todas/Deshacer, comparación mantenida, gesto como un paso y últimas ediciones guardadas.

Valores esperados de la **matriz del preset a intensidad1, sin ajustes/grano**, redondeando con `floor(v+0.5)` y clamp final:

| RGB fuente | Original | BW | Película | Sepia | Cálido | Frío | Desvanecido | Vivo |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 12,36,90 | 12,36,90 | 35,35,35 | 21,21,21 | 49,44,34 | 23,36,81 | 0,36,102 | 33,48,82 | 0,27,101 |
| 200,70,40 | 200,70,40 | 95,95,95 | 91,91,91 | 140,125,97 | 217,69,25 | 188,70,52 | 168,86,67 | 236,57,16 |
| 20,190,120 | 20,190,120 | 149,149,149 | 152,152,152 | 177,157,123 | 26,192,107 | 8,190,132 | 69,176,132 | 0,208,111 |
| 240,235,220 | 240,235,220 | 235,235,235 | 251,251,251 | 255,255,220 | 252,235,207 | 228,235,232 | 231,227,218 | 253,246,225 |
| 128,128,128 | 128,128,128 | 128,128,128 | 128,128,128 | 173,154,120 | 140,128,116 | 116,128,140 | 131,131,131 | 128,128,128 |
