# Auditoría visual Mac · Fase 3, subproyecto 2

Método: capturas fuera de pantalla con `PolarMac/Tools/snapshots.sh` (NSHostingView + datos demo generados en código; no se abre la app real ni se toca ninguna biblioteca). Tamaños 1100×720 y 1600×1000, claro y oscuro. Las PNG viven en `docs/capturas/fase-3/sub2/mac/` (después) y `mac-antes/` (antes); no se versionan.

Criterios: skills `impeccable` (audit → polish) y `redesign-existing-projects`, aplicados como principios a SwiftUI: simetría de filas, alturas mínimas de 48 pt (56 en acciones principales), tokens `Spacing` y `PolarRadius`, sin texto cortado a media palabra.

Nota: 1100×720 queda por debajo del mínimo real de la ventana (1120×740); se captura igual como peor caso.

| # | Pantalla | Hallazgo | Estado |
|---|----------|----------|--------|
| 1 | Editor · riel de diseños | Nombres de diseño cortados a media palabra («Foto + ca…», «Reprodu…») por ancho de 216 pt y `lineLimit(1)` | Corregido: riel de 248 pt, nombre a 2 líneas, descripción a 3, relleno `Spacing.s` |
| 2 | Editor · riel de diseños | «Aplicar diseño a» truncaba el menú a «Colec…» | Corregido: etiqueta arriba y `EqualChoice` de ancho completo (Colección / Hoja / Tarjeta) |
| 3 | Editor · Texto | Los selectores segmentados nativos (Texto, Aplicar a, Tamaño) no ocupaban el ancho y los menús tenían anchos distintos; Negrita/Cursiva eran casillas desalineadas | Corregido: `EqualChoice` (segmentos iguales) y `ToggleChip` (48 pt) en todo el panel |
| 4 | Editor · Texto | Posición, desplazamiento y alineación mezclados con lo básico; «Mostrar este texto» y «Tamaño» a la vista | Corregido: «Más opciones» (Alineación, Horizontal, Vertical, Restablecer); a la vista Mostrar este texto, Tamaño (Auto/S/M/L + exacto), fuente, color |
| 5 | Editor · Texto (música) | El enlace del QR estaba al final del panel | Corregido: sección «Canción» primero (título, artista, Enlace para el QR) |
| 6 | Editor · Fotos | Rejilla de 4 acciones; Quitar fondo solo dentro de Encuadrar | Corregido: rejilla de 6 (añade Quitar fondo y Girar 90°); vista previa centrada |
| 7 | Terminar | Fila de acciones asimétrica: Imprimir y Compartir como mosaicos de 64 pt y Guardar PDF/imagen como píldoras pequeñas | Corregido: los cuatro con `ActionTileLabel`, mismo ancho y alto |
| 8 | Terminar | Selector de Calidad con etiqueta desalineada | Corregido: ancho máximo y alineación a la izquierda |
| 9 | Editor · Papel | Margen y «Imprimir borde de las tarjetas» ya estaban a la vista (sin disclosure) | Sin cambio necesario |
| 10 | Biblioteca | Datos demo con nombres repetidos (artefacto del arnés, no de la app); hueco vertical amplio entre miniatura y título por `reservesSpace` | Aceptado: es el espacio reservado para que todas las tarjetas midan lo mismo |
| 11 | Editor · galería | Filas de `minHeight 48` dejan aire sobre «Seleccionar varias» | Pendiente (bajo impacto) |
| 12 | Editor · Papel | Hueco bajo el control de margen (slider de 48 pt) | Aceptado: objetivo táctil mínimo |
| 13 | Editor · Diseño | «Fotos por hoja» se partía en 5 + 3 botones (rejilla adaptable) y «Estilos de texto» no ocupaba el ancho | Corregido: 4 columnas × 2 filas flexibles; botón de ancho completo y 48 pt |
| 14 | Editor · Texto | La vista previa de la fuente es un recuadro blanco también en oscuro | Aceptado: representa el papel blanco de impresión |

## Navegación entre hojas (tarea 6)

`SheetNavigator` (Studio.swift) decide con umbral de 80 pt de desplazamiento horizontal acumulado, un solo cambio por gesto (hasta que termina la fase) y 0,4 s de pausa; las flechas respetan media pausa. `SheetNavigationKeys` (PolarApp.swift) lo conecta al lienzo: dos dedos sobre la hoja y ← → si el foco no está en un campo ni en un control; desactivado al editar huecos, con una importación en curso, soltando archivos, en selección múltiple o con un diálogo abierto. En Mac las fotos no se arrastran dentro de la hoja; lo único arrastrable es un hueco de plantilla, cubierto por «editar huecos». Prueba de la lógica en `StudioChecks`.
