# Polar: edición y calidad de impresión

> **For agentic workers:** REQUIRED SUB-SKILL: Use executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Conservar detalle al imprimir y permitir recortes de fondo, frases, selección múltiple y diseños por hoja/tarjeta en Android y Mac.

**Architecture:** Extender el proyecto existente con campos opcionales compatibles. Los originales permanecen intactos; la segmentación guarda una máscara PNG, y el renderizador compone el fondo al previsualizar/exportar. Reutilizar historial, guardado atómico, importadores y motores nativos.

**Tech Stack:** Kotlin/Compose/Canvas, ML Kit Subject Segmentation; SwiftUI/CoreGraphics/CoreImage/Vision; catálogo de texto local compartido.

**Spec:** Solicitud del usuario del 3 de octubre de 2026 en esta conversación.

## Constraints
- Android y Mac; conservar proyectos antiguos y originales.
- Un solo subagente, centrado en pixelación; no borrar datos del emulador.
- Eliminar el límite que pierde detalle al imprimir. No confundir interpolación o metadatos con resolución real.
- Aviso con Deshacer dura como máximo 1000 ms; Deshacer sigue en la barra.
- Fondos: transparente, blanco, negro, color, foto importada; ajuste de borde y sombra. Composición convencional, sin reconstruir rostros ni prometer iluminación generativa.
- Diseños independientes por página y tarjeta compatibles con su número de fotos. Mantener la cuadrícula/posiciones para no borrar imágenes. Los diseños de película se cambian a nivel colección cuando cambia el número de fotos por tarjeta.
- Catálogo de al menos 100 frases originales, pequeñas, editables y buscables; fuentes públicas o contenido aportado por el usuario sin depender de un servidor.

## Review focus
- Teclado, pantalla baja, orientación horizontal y fuente grande: input y acciones visibles/alcanzables.
- Copiar/remover varias posiciones conserva textos, filtros, recortes, originales e historial.
- Modelos no disponibles/sin conexión y ausencia de sujeto: error visible, no reemplazar original.
- Máscara/fondo persisten en duplicados y proyectos, impresión usa detalle original.
- Diseños mixtos: hitboxes, recorte, aviso de resolución y exportación usan misma geometría.

## Tasks
- [x] Pixelación: BitmapLoader/AndroidExportService; regresión A3+zoom y errores de lectura.
- [x] Modelo y geometría compartida: pageDesigns y diseño por tarjeta, validación, renderizadores y almacenamiento Android/Mac; round-trip y cambios de página.
- [x] Selección múltiple: operaciones de posiciones, copia a nueva hoja, una acción de historial; pruebas de conservación.
- [x] Catálogo compartido de frases y selector editable: búsqueda/categorías, selección nativa de texto, vista ampliada; comprobar catálogo sin duplicados.
- [x] Teclado y distribución: insets IME, panel desplazable, preview ampliada; revisión móvil/tablet, claro/oscuro y texto grande.
- [x] Segmentación nativa y composición: máscara a resolución de inferencia aplicada al original, fondo de color/imagen, borde/sombra y restauración; probar alpha y fallos.
- [x] Guías opcionales al exportar: marco completo foto+texto y guías de esquinas/continuas; PDF/PNG/JPG ambos sistemas.
- [x] Verificación, documentación y release firmado con versiones coincidentes; sólo publicar tras comprobar regresiones.

**Límite de verificación:** Vision y composición nativa aprobados. El recorte ML Kit positivo depende de que Google Play Services complete la descarga del modelo; el emulador agotó el tiempo de espera y conservó el original. Ver detalles en `docs/edicion-y-calidad-2.2.0.md`.
