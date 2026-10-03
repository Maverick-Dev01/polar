# Capturas de simetría · antes y después

Las PNG contienen fotografías personales, son locales y no se versionan. El inventario JSON guarda únicamente nombres y dimensiones reales.

| Grupo | Casos | Matriz |
| --- | ---: | --- |
| Android antes | 132 | 11 pantallas × 3 tamaños × 2 temas × 2 escalas |
| Mac antes | 88 | 11 vistas/paneles × 2 tamaños × 2 temas × 2 escalas |
| Android después | 216 | 18 pantallas/estados × 3 tamaños × 2 temas × 2 escalas |
| Mac después | 96 | 12 vistas/paneles × 2 tamaños × 2 temas × 2 escalas |
| Propuesta HTML | 144 | 9 escenas × 4 tamaños × 2 temas × 2 escalas |

Android usa perfiles aislados Android 34, densidad 160, tamaños solicitados 360×640, 411×891 y 1280×800, puerto 5560. `mercatto_test` presentó ANR de System UI; sus datos se conservaron. La matriz anterior se completó con la app 2.0.0 y su host compatible, construidos desde la revisión anterior en un checkout temporal. Antes/después usan las mismas condiciones y foto.

PixelCopy captura el contenido nativo del host, excluyendo decoraciones del sistema: 360×488, 412×739 y 1280×612 px. El buffer del teléfono normal mide 412 px; el inventario registra esta dimensión real. El menú anclado del después usa captura del display completo para incluir su Popup independiente: 360×640, 412×891 y 1280×800 px. No se recortan ni redibujan las vistas para simular la interfaz.

Mac usa áreas de contenido 1280×800 y 1728×1117 pt, PNG Retina 2×. El 130 % escala fuentes en copias temporales de las vistas, sin cambiar contenedores ni tipografía del renderer; es una prueba de estrés, no una preferencia global de macOS. El host muestra componentes SwiftUI reales y usa una biblioteca aislada. El después incluye Filtros. Los hosts no sustituyen una prueba física de impresora o un recorrido manual completo de accesibilidad.

Android después añade menú anclado, Filtros y las dos alturas de cada herramienta: inicio, inicio-menu, catálogo, editor, fotos/filtros/diseño/texto/papel compactos y amplios, encuadre, terminar, ajustes y bienvenida. Los estados amplios de tablet muestran su panel lateral, que mantiene altura fija de ventana.

## Repetir Mac

Desde la raíz:

```sh
python3 docs/prototipos/simetria-2026-10/capturar-mac-despues.py
```

Requiere macOS, xcrun y la foto local `Fotitos/Mejoradas/pareja nueva 1.jpg`. Compila copias temporales y las elimina al terminar. Para el antes, usar `capturar-mac.py` con las fuentes de la revisión anterior.

## Repetir Android

Copiar temporalmente `SymmetryAfterCaptureTest.kt` como `PolarAndroid/app/src/androidTest/java/com/polar/app/SymmetryAuditCaptureTest.kt`, y la foto local como `app/src/androidTest/assets/auditoria.jpg`. Compilar/instalar debug y debugAndroidTest en un emulador de pruebas estable. Para el antes se usa `SymmetryAuditCaptureTest.kt` del directorio de prototipo y el código anterior de la app.

```sh
python3 docs/prototipos/simetria-2026-10/capturar-android.py --serial emulator-5560 --phase despues --size 360x640
```

Repetir 411×891 y 1280×800 con el perfil de tamaño correspondiente. `--size` no modifica resolución ni densidad. Sin `--size`, el script recorre los tres tamaños y restaura la configuración al terminar; usar sólo un AVD estable y aislado. Retirar el host temporal y la foto después; no se incluyen en el APK entregado.

PixelCopy obtiene la raíz Compose real; para el Popup se captura el display. La ejecución falla si no termina la instrumentación o falta alguna de las 72 capturas esperadas (44 en el antes).

## Propuesta aprobada

El HTML ilustra la interacción; sus filtros CSS no prueban el renderer ni la exportación. Para verlo, servir **únicamente** su directorio público:

```sh
python3 -m http.server 8768 --bind 127.0.0.1 --directory docs/prototipos/simetria-2026-10
```

Abrir `http://127.0.0.1:8768/index.html`. El directorio servido contiene ilustración y fuentes, ninguna foto personal. Para capturar sin marco: `?capture=1&w=411&h=891&dark=1&font=130&scene=editor&expanded=1`.
