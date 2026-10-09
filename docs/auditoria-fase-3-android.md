# Auditoría de Android · Fase 3, subproyecto 2

Criterios: la skill Impeccable (modo auditoría nativa, Material 3) y «redesign-existing-projects», aplicados a Compose.
Reglas de simetría: tarjetas de igual altura, filas de botones del mismo ancho, 48 dp mínimos (56 en acciones principales), sin alturas fijas que recorten, tokens de espaciado y radios.

Configuraciones recorridas en el emulador `mercatto_test` (usuario Demo, fotos de stock): 411 dp claro y oscuro, 360 dp con letra al 130 %, tablet 1280 × 800 dp claro.
Capturas en `docs/capturas/fase-3/sub2/android/` (`antes-*` y `despues-*`; las PNG no se versionan).
Pantallas: Inicio, Catálogo, Editor (Fotos, Filtros, Diseño, Texto, Papel), Encuadrar, Terminar, Ajustes y bienvenida.

## Hallazgos corregidos

| # | Pantalla | Hallazgo | Corrección |
|---|---|---|---|
| 1 | Editor · barra | A 411 dp el título se partía en «Nuevo / diseño» y el estado quedaba como «Gua…» | Por debajo de 480 dp el subtítulo muestra solo el estado, en `labelSmall` |
| 2 | Editor · barra | Rehacer e Imprimir debían caber a 360 dp con letra al 130 % | Rehacer pasa al menú (< 400 dp o letra > 1.15); prueba de límites sin encimes |
| 3 | Editor · bandeja | El botón de la bandeja decía «Más opciones», igual que el ⋮ de la barra | Ahora «Ampliar» / «Reducir» |
| 4 | Editor · pista | La fila «Toca una tarjeta…» tenía 48 dp fijos y recortaba con letra grande | Altura mínima 48 dp con relleno vertical |
| 5 | Editor · menú de tarjeta | Con seis acciones la fila desplazable ocultaba Texto y Quitar a 411 dp | Si caben (≥ 60 dp cada una) se reparten por igual; si no, desplazamiento con 88 dp fijos |
| 6 | Editor · bandeja reducida | Texto «Seleccionar» literal en el código y etiquetas cortadas con «…» en una línea | Texto en `strings.xml`; etiquetas en dos líneas |
| 7 | Editor · Fotos | «Agregar» y «Agregar carpeta» quedaban de distinta altura si una envolvía | `fillMaxRowHeight` en la fila |
| 8 | Editor · hoja | Radio de 8 dp escrito a mano en la insignia | Token `shapes.small` |
| 9 | Encuadrar | Fila desplazable con «Centrar» y «Restablecer» cortados | Cuadrícula de tres columnas con el mismo ancho y 56 dp |
| 10 | Encuadrar | El valor del zoom no quedaba pegado al borde derecho | Alineado al final |
| 11 | Encuadrar | En tablet todo se estiraba a 1280 dp | Columna centrada de 640 dp como máximo |
| 12 | Terminar | Las hojas quedaban pegadas a la izquierda de su recuadro | Centradas |
| 13 | Catálogo | El buscador nuevo tocaba la barra superior y tenía otro radio que el de Inicio | Relleno superior y `shapes.extraLarge` en ambos |
| 14 | Inicio | En biblioteca vacía había dos botones «Nuevo diseño» (botón flotante y el del estado vacío) | El botón flotante se oculta cuando no hay diseños |
| 15 | Inicio | Buscador sin altura mínima y estirado en tablet | 56 dp mínimo y ancho máximo de 640 dp |
| 16 | Papel y Ajustes | Margen y «Borde de las tarjetas» escondidos; papel predeterminado con 3 de 8 tamaños | A la vista, margen en mm o pulgadas, 8 tamaños (tarea 3) |
| 17 | Texto | «Mostrar este texto» y «Tamaño» escondidos; «Canción» no iba primero | Reorganizado (tarea 5) |

## Hallazgos pendientes

| # | Pantalla | Hallazgo | Prioridad | Nota |
|---|---|---|---|---|
| P1 | Catálogo | En las miniaturas el texto se corta («Nuestra canció», «Tú y yo» sobre el boleto) | P2 | Lo dibuja `Thumbnailer`/`PolarRenderer`, no el diseño de la pantalla; requiere ajustar las zonas de texto de esas dos tarjetas |
| P2 | Editor · menú de tarjeta | A menos de ~370 dp o con letra grande la fila sigue siendo desplazable | P3 | Decisión: los tamaños de toque se respetan; se ve el borde de la siguiente acción como pista |
| P3 | Tablet | Los botones de navegación del emulador se dibujan sobre la esquina inferior izquierda | — | Efecto del emulador al forzar `wm size`; no es de la app |
| P4 | Bienvenida | Solo revisada a 411 dp claro | P3 | Sin hallazgos; falta repetirla en 360 dp y oscuro |
| P5 | Catálogo y Ajustes en 360 dp / tablet | No se capturaron en todas las configuraciones | P3 | Sus filas usan `weight(1f)` y `heightIn(min = 48.dp)`; revisión de código sin hallazgos |

## Verificación

Compilación: `./gradlew testDebugUnitTest lintDebug assembleDebug` en verde.
Las configuraciones del emulador se restablecieron a 1080 × 2400, 420 dpi y letra 1.0.
