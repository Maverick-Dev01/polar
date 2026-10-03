# Verificación de Polar para Mac — 2 de octubre de 2026

Actualizada **Polar.app**, versión **2.0.0**, compilación **3**, para Apple Silicon y macOS 14 o posterior. Si estaba abierta durante la actualización, hay que cerrarla y volverla a abrir. La sesión anterior del usuario no se cerró durante las pruebas.

## Funciones incorporadas desde Android

- Biblioteca «Tus diseños»: búsqueda, orden, nombres, duplicados, eliminación recuperable y apertura de proyectos.
- Autoguardado con estado visible, escritura atómica y guardado antes de salir o cambiar de proyecto. Los guardados se realizan de forma serial para evitar que uno antiguo sustituya la última edición. Si falla la escritura, el editor conserva los cambios pendientes.
- Copias locales de fotos y plantillas para conservar los diseños aunque se muevan los originales.
- Texto y apariencia por tarjeta, opción de volver al texto general, fechas de captura o elegidas y las 20 fuentes ya incluidas en Android, con sus licencias.
- Historial con agrupación de edición, categorías de diseños, tema claro/oscuro/sistema, unidades y papel predeterminado.
- Pantalla «Terminar» con hojas, fotos, espacios vacíos y avisos de resolución; impresión nativa de macOS, exportación PDF/PNG y compartir PDF.

Se conservaron las distribuciones, papeles, edición de fotos, importación de moldes y omisión de marcos/textos vacíos. La implementación usa las funciones nativas de Mac para impresión, compartir, fuentes y emojis.

## Resultados

**Las siete suites nativas pasan** mediante `cd PolarMac && ./check.sh`:

| Suite | Cobertura principal |
| --- | --- |
| AutosaveChecks | 100 ediciones rápidas, última edición, salida/reapertura, historial agrupado, textos por tarjeta, preferencias, biblioteca, copias de fotos y fallo de escritura |
| ImporterChecks | Orden, duplicados, orientación EXIF, archivos ilegibles, originales intactos y límite de 2.000 fotos |
| ModelChecks | Capacidad, páginas, persistencia y validación |
| ParityChecks | Proyectos antiguos y JSON de Android, textos por tarjeta, fechas/EXIF, 20 fuentes, PDF, tarjetas vacías y emojis |
| RendererChecks | 20 estilos incluyendo importación, ocho papeles en ambas orientaciones, tipografía, PDF/PNG a 300 ppp, QR, calendario y escritura atómica |
| StudioChecks | Deshacer, selección, huecos multipágina, 44 fotos/11 hojas, importación y archivos mayores de 5 MB |
| TemplateChecks | Huecos blancos/transparentes, orientación, detección, ajuste manual y persistencia |

La prueba de autoguardado falló con el código original y pasó con la actualización. `./build.sh` terminó correctamente; la firma local pasó `codesign --verify --deep --strict Polar.app`. El paquete contiene las 20 fuentes y declara versión 2.0.0/3.

En una aplicación de prueba aislada, con las mismas vistas y fotografías sintéticas, se verificaron apertura/reapertura, texto exclusivo de la segunda tarjeta, estado «Guardado», tema oscuro, biblioteca, resumen de dos hojas/cinco fotos y diálogo nativo de impresión. La barra final se revisó visualmente y sus etiquetas quedan completas. Se canceló el diálogo sin enviar trabajos: la impresora EPSON mostraba estar desconectada.

## Límites comprobados

No se realizó impresión física ni validación en un Mac Intel. Esta compilación está firmada localmente, sin notarización para distribución pública. El cierre forzado del proceso antes de que termine el intervalo de autoguardado puede perder la edición aún pendiente. Los guardados son seriales y pueden pausar la interfaz con proyectos grandes; el archivo del proyecto está limitado a 5 MB. Las copias de recursos y la miniatura no forman una única transacción con el proyecto.

La biblioteca reside en `~/Library/Application Support/Polar`. Un `.polar` exportado conserva referencias a sus recursos: para trasladarlo a otro equipo también hacen falta sus fotos y plantillas. La guía de uso está en [PolarMac/README.md](../PolarMac/README.md).
