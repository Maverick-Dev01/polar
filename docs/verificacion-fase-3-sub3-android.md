# Verificación Android · fase 3 · subproyecto 3

Build: `./gradlew testDebugUnitTest lintDebug assembleDebug` en verde (336 pruebas, 0 errores de lint).

## Decisiones de implementación
- La geometría se lee en tiempo de ejecución de `shared-fixtures/estilos-geometria.json`: una tarea Gradle (`copySharedGeometry`) lo copia a recursos del APK. `GeometryDrawing` dibuja las primitivas de forma genérica.
- Cuadrículas por omisión: fotomatón 3×1, instantánea ancha 2×3, vinilo 3×3, casete 2×4, collage 2×3, cinta washi 2×3.
- Fuente del diseño: mientras la tarjeta tenga la fuente «Sistema» (valor por omisión) manda la `defaultFont` de la ranura de texto.
- QR de los diseños existentes (la geometría compartida sólo trae vinilo y casete): `spotify` sin cambios; `playerRed` x 0.62, y 0.40, lado 0.26 del ancho; `playerGray` x 0.355, y 0.60, lado 0.14 del ancho (etiqueta en la esquina inferior derecha de la foto). Todos los QR nuevos llevan respaldo blanco con zona de silencio de 8 % del lado.
- Detección de forma: un hueco se toma como rectángulo si llena ≥ 99.5 % de su caja (el 90 % del plan haría rectángulo un radio del 20 %, que sólo quita ~3 %); si no, se elige entre elipse y rectángulo redondeado por IoU (≥ 0.85, y mejora ≥ 0.8 % sobre el rectángulo). Los huecos con relleno ≥ 70 % entran a la detección.
- `meta.json` de «Mis moldes»: `id`, `nombre`, `dhash`, `sha256`, `regiones`, `creado` (segundos tipo Swift) y además `archivo`, `ancho`, `alto`.

## Recorrido en emulador (AVD mercatto_test, usuario 10)
Capturas en `docs/capturas/fase-3/sub3/android/` (no se versionan): los seis diseños con fotos, catálogo con miniaturas, asistente (pasos 1 a 3, círculos detectados como óvalos, arrastre), «Mis moldes», aviso de duplicado al importar `molde-rects-50.png` tras guardar `molde-rects.png`.
Pendiente de revisión manual: recorte por forma en el PDF exportado (usa el mismo `clipPath` que la vista previa).
