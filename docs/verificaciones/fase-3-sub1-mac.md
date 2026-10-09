# Fase 3 · Subproyecto 1 · Verificación Mac

Fecha: 2026-10-08. Verificación automática (`PolarMac/check.sh` y `PolarMac/build.sh`), sin abrir la app real ni la carpeta personal de fotos.

## Tamaños de exportación (suite `ExportCleanupChecks`)
Proyecto: una foto de ruido 3000 × 2000 px (peor caso para JPEG), Carta, 9 tarjetas Polaroid, foto en la primera.

| Calidad | PDF (bytes) | JPG de la hoja (bytes) | Densidad JPG |
|---|---|---|---|
| Ligero (200 ppp, JPEG 85) | 113 361 | 173 226 | 200 ppp (1700 px de ancho) |
| Alta (300 ppp, JPEG 94) | 361 815 | 463 771 | 300 ppp (2550 px) |
| Máxima (300 ppp, sin recomprimir) | 1 107 275 | 463 771 (igual que Alta) | 300 ppp |

Se cumple Ligero < Alta < Máxima en PDF. Antes de este cambio, el PDF por defecto equivalía a «Alta» (JPEG 94, 300 ppp) y «PDF sin compresión JPEG» a «Máxima».

## Pruebas agregadas
- Límites de resolución: 149.9 baja, 150 aceptable, 219.9 aceptable, 220 buena.
- Las capas de foto, fondo y el contexto JPEG del PDF usan interpolación alta.
- Orden de pesos, densidad declarada del JPG y persistencia de la preferencia (y lectura de `settings.json` antiguos sin la clave).
- QR: un enlace de 3000 caracteres no tumba PDF ni PNG y dibuja un marcador; un enlace normal se decodifica con `CIDetector`.
- Papelera: purga a 7 días, `emptyTrash` respeta el diseño que aún se puede deshacer y no toca proyectos activos.
- Ninguna fuente contiene «Fotitos» ni el respaldo `#filePath`.

## Pendiente
El recorrido manual con la copia demo (HOME apuntando a una carpeta demo) no se hizo en esta pasada, para no usar ratón ni teclado en el Mac del usuario.
