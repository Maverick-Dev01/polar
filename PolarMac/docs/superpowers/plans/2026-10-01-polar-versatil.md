# Polar versátil Implementation Plan

> For agentic workers: ejecutar unidades independientes en paralelo y verificar la integración completa.

**Goal:** Ampliar Polar con edición de texto, papel, distribución e importación de moldes sin perder compatibilidad.

**Architecture:** Mantener modelos Codable, dibujante nativo compartido y Studio observable. Regiones de moldes normalizadas y análisis ImageIO local; una sola interfaz SwiftUI existente.

**Tech Stack:** Swift 5, SwiftUI, AppKit, Core Graphics, ImageIO, PDFKit; macOS 14+ Apple Silicon.

**Spec:** ../specs/2026-10-01-polar-versatil-design.md

## Global Constraints
- Proyectos antiguos y fotografías intactos.
- PNG a 300 ppp según papel; exportación atómica y protegida.
- Sin servicios ni dependencias descargadas.

## Review Focus
- Nuevos defaults Codable no deben impedir abrir proyectos antiguos.
- Una tarjeta vacía debe desaparecer completamente de la impresión.
- Cambios de capacidad deben conservar asignaciones y selección visible.
- Un molde con huecos alpha y blancos debe aceptar ambas clases de foto.
- La hoja horizontal/personalizada debe concordar en lienzo, PDF y PNG.

## Task 1: Modelos y dibujante
Files: Sources/Models.swift, Sources/Renderer.swift; Tests/ModelChecks.swift, Tests/RendererChecks.swift.
- [x] Check rojo para nuevos formatos/defaults y tarjeta vacía.
- [x] Implementar medidas, tipografía, diseños y exportación compartida.
- [x] Check verde de compatibilidad, geometría, texto y salidas.

## Task 2: Importación de moldes
Files: Sources/TemplateImport.swift, Tests/TemplateChecks.swift.
Interfaces: TemplateImporter.read(url:) -> (template: ImportedTemplate, detectedCount: Int), ImportedTemplate, TemplateRegion con rect/clamp.
- [x] Check rojo de detección.
- [x] Detectar componentes rectangulares alpha/blancos encerrados; fallback editable.
- [x] Check verde de orientación, límites y conservación del archivo.

## Task 3: Controles e integración
Files: Sources/Studio.swift, Sources/PolarApp.swift, Tests/StudioChecks.swift, check.sh, README.md.
- [x] Check rojo de distribución sin pérdida de fotos.
- [x] Conectar inspector, papel, fuentes, presets y corrección manual de moldes.
- [x] Compilar, revisar defectos, probar flujo Mac real y exportar ejemplo.

## Verificación final

`./check.sh` completo pasó: modelos, importación de fotos, dibujante, Studio e importación de moldes. Cubre 20 estilos (19 internos y el importado), 8 papeles en ambas orientaciones, compatibilidad v1, omisión de tarjetas vacías, texto PDF, PNG a 300 ppp, protección de originales, límites y persistencia.

Ventana Mac: se comprobó 44 fotos → 11 hojas de 4, edición/guardado de Georgia, tamaño 13, negrita y posición; Oficio horizontal; detección real de dos huecos, ajuste, añadir/quitar/deshacer; guardado del molde y exportación PNG desde el menú. Se corrigió la compactación de nil al redistribuir y se activa la app antes de presentar paneles para que aparezcan al frente.

Ejemplo PDF actualizado: cinco hojas, última con ocho títulos/subtítulos y noveno espacio íntegramente blanco. Ejemplo Oficio horizontal: cuatro tarjetas, fuente Georgia. Compilación local 1.1 y firma verificadas. La impresora física no se operó.
