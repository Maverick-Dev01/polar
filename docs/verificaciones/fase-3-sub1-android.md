# Fase 3 · Subproyecto 1 · Verificación Android

Emulador AVD `mercatto_test`, usuario Demo (id 10), APK debug (`io.github.maverickdev01.polar.debug`), fotos libres de Pictures/Vacaciones. Diseño Polaroid, Carta, 9 fotos en una hoja.

## Tamaños de PDF (bytes)

| Caso | Ligero | Alta | Máxima |
|---|---|---|---|
| Fotos llenando (Llenar) | 281 859 | 894 151 | 5 537 424 |
| Todas en «Ajustar», versión nueva | 284 332 | 817 419 | 5 236 708 |
| Todas en «Ajustar», versión anterior (2.2.0, antes del cambio) | n/a | 5 262 602 («Guardar PDF») | 5 262 602 («sin compresión JPEG») |

Con «Ajustar» el PDF anterior no usaba JPEG (la capa por foto tenía transparencia). Ahora Alta pesa 817 KB frente a 5.26 MB (-84 %). Orden verificado: Ligero < Alta < Máxima.

## Otras verificaciones
- Enlace de QR de 3144 caracteres: aviso en línea «Enlace muy largo: no cabe en un QR. Usa uno más corto.» y el PDF exportado dibuja el marcador rojo «Enlace muy largo» en lugar del QR (rasterizado con pdftoppm).
- Papelera: se duplicó y se borraron dos diseños; Ajustes muestra «Vaciar papelera (1)» (el último borrado, aún deshacible, queda protegido); tras vaciar, la fila desaparece y la carpeta del último borrado sigue en `trash/`; ningún proyecto activo se tocó.
- No se ejecutaron las pruebas instrumentadas (`connectedDebugAndroidTest`) para no tocar el usuario 0 del emulador; se agregaron pruebas instrumentadas nuevas (tamaños por calidad y comparación visual) que sí compilan.
