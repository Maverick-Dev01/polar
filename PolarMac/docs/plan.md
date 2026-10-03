# Polar Implementation Plan

> For agentic workers: implementar las unidades independientes en paralelo y revisar la integración final.

**Goal:** Crear Polar.app funcional para importar fotos, aplicar moldes y exportar carta.

**Architecture:** Un modelo Codable común, un dibujante Core Graphics compartido entre vista previa y exportación y una ventana SwiftUI con estado local. Exportaciones atómicas mediante diálogos de guardado.

**Tech Stack:** Swift 5, SwiftUI, AppKit, Core Graphics, ImageIO, PDFKit. macOS 14+.

**Spec:** design.md

## Global Constraints

- Carta: 612×792 pt; PNG 2550×3300 a 300 ppp.
- Sin servicios, dependencias externas ni modificación de originales.
- NSOpenPanel/NSSavePanel para rutas elegidas; .polar guarda referencias locales.

## Review Focus

- HEIC y orientación EXIF se dibujan correctamente.
- Fotos ausentes no se omiten silenciosamente al exportar.
- Filas/columnas fuera de límites en proyectos se rechazan.
- Muchas fotos producen todas las páginas, sin pérdida de asignaciones.
- Cambiar diseño conserva fotos y corrige página/espacio seleccionado.

## Task 1: Modelo e importación

Files: Sources/Models.swift; Tests/ModelChecks.swift; Sources/Studio.swift.
Interfaces: TemplateStyle, PrintSettings, PhotoAsset, PhotoPlacement, PolarProject Codable. capacity, pageCount, normalized(), validated().

- [x] Escribir y ejecutar una comprobación que falle para capacidad, páginas y validación.
- [x] Implementar modelo, importación nativa con thumbnails orientados y estado de edición.
- [x] Verificar round-trip de proyecto, grillas inválidas y fotos suficientes para varias páginas.

## Task 2: Dibujante y exportación

Files: Sources/Renderer.swift; Tests/RendererChecks.swift.
Interfaces: PolarRenderer.cardRects(settings:), photoRects(in:style:settings:), preview(project:page:), writePDF(project:to:), writePNG(project:page:to:).

- [x] Comprobar primero geometría y exportaciones con una prueba fallida.
- [x] Dibujar todos los moldes, recortes, calendario real, QR opcional y marcas de corte.
- [x] Verificar tamaño, número de páginas, bordes y error de archivo ausente.

## Task 3: Interfaz y entrega

Files: Sources/PolarApp.swift; build.sh; README.md.

- [x] Conectar diseños, galería, reemplazo, encuadre, textos y navegación.
- [x] Conectar guardado/apertura y exportación sin cambiar originales.
- [x] Compilar y empaquetar Polar.app; comprobar tests y UI nativa.
- [x] Revisión independiente, resolver fallos importantes y abrir la app.

## Verificación final

`./check.sh` pasó para modelo, importación, once moldes, PDF carta, PNG a 300 ppp, orientación EXIF, QR, calendario y deshacer. `./build.sh` compiló el paquete y `codesign --verify --deep --strict` verificó la firma local.

Desde la interfaz se importaron las 44 fotos, se guardaron y reabrieron los proyectos, y se exportó `Ejemplo - Polaroid.pdf`: cinco hojas de 612×792 pt y 9,549,061 bytes. `Ejemplo - Polaroid.png` mide 2550×3300 píxeles a 300 ppp y fue revisado visualmente. La app final quedó abierta con el proyecto de cinco hojas. `verificar-fotitos.py` confirmó originales y versiones anteriores intactos.
