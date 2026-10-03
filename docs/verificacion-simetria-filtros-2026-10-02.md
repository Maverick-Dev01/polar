# Polar 2.1.0 · Simetría, encuadre y filtros

2 de octubre de 2026. Implementación del prototipo aprobado y de la ampliación del usuario: Android y Mac, modo oscuro legible y filtros por proyecto, página, varias páginas o foto. Sin dependencias nuevas, publicación ni cambios en las fotos originales.

## Cambios por pantalla

| Pantalla | Resultado en Android y Mac |
| --- | --- |
| Inicio / Biblioteca | Nombre de una línea y subtítulo de dos líneas reservadas; menú anclado con Abrir, Renombrar, Duplicar, Compartir y Borrar. Mac renombra en línea. |
| Catálogo | Imagen proporcional, título de una línea, descripción de dos líneas reservadas, tarjeta simétrica, selección con marca y texto completo accesible. |
| Editor | Cinco pestañas: Fotos, Filtros, Diseño, Texto y Papel. En teléfono, bandeja acoplada compacta/amplia con control explícito y gesto; el lienzo ocupa el espacio restante. Panel lateral en tablet/Mac. Transición de 240 ms y reducción de movimiento. |
| Fotos | Fichas y acciones simétricas; selector nativo de galería y Rellenar. Se conservan el orden y las copias de fotos. |
| Diseño | Distribuciones y controles existentes conservados; selección visible y aviso opcional de blanco y negro para película. No se aplica automáticamente. |
| Texto | Alcance por tarjeta y tipografías existentes; controles adaptables al 130 %. Colores del papel conservados. |
| Papel | Formatos y orientación existentes, unidades «pulg.»/mm y validación antes de guardar una distribución imposible. |
| Encuadre | Tarjeta dibujada por el motor; arrastre, pellizco/trackpad, doble toque Llenar/Ajustar, anillo, sobrante atenuado, tercios durante gesto y aviso de resolución. Cinco acciones, navegación entre fotos, ajuste fino 1 %/zoom 0.1 y acciones accesibles. |
| Filtros | Ocho presets con foto real, intensidad y ajustes progresivos; selección del alcance, Quitar filtro, Aplicar a todas con Deshacer y comparación temporal. Miniaturas pequeñas en segundo plano y caché limitada. |
| Terminar | Acciones de altura mínima, resumen de filtros y resolución; Revisar abre Encuadre. Mac usa una vista completa dentro de la ventana. |
| Ajustes / Bienvenida | Espaciado y colores acordes al tema; en Mac, ventana nativa Settings. Acciones que admiten texto ampliado. |
| Controles compartidos | Márgenes 16/24, escala 4/8/16/24/32, líneas de texto reservadas, valores alineados y objetivos mínimos 48/56. En Android, el color de contenido raíz sigue el tema y corrige Text oscuros sobre fondos oscuros. |

El papel sigue blanco en modo oscuro. Se corrigió además el selector Categoría/Todos de Mac y se revisó con letra 130 %.

Contrastes calculados de tokens Android (luminancia sRGB; revisión de colores, no certificación de todas las combinaciones):

| Uso | Texto / fondo | Relación |
| --- | --- | ---: |
| Claro · principal | `#2B2221` / `#F7F2EB` | 13.93:1 |
| Claro · secundario | `#6B5D5A` / `#F1EAE2` | 5.27:1 |
| Claro · acción | `#FFFFFF` / `#7A293B` | 9.51:1 |
| Oscuro · principal | `#EDE0DD` / `#1D1A19` | 13.43:1 |
| Oscuro · secundario | `#B9AAA7` / `#332D2C` | 6.04:1 |
| Oscuro · acción | `#561D2B` / `#FFB2BC` | 7.69:1 |

## Presets y contrato de archivos

Original, Blanco y negro, Película, Sepia, Cálido, Frío, Desvanecido y Vivo. Las ocho matrices 4×5 exactas, sus 40 valores RGB de referencia, orden de composición y grano están en [filtros.md](filtros.md).

`settings.photoLook`, `cardOverrides[].photoLook` y `placements[].photoLook` son opcionales. Precedencia: colocación > tarjeta > general > neutral. Una foto de una tira de cinco puede tener su propio filtro. Página(s) aplica a todas sus tarjetas y elimina las excepciones de fotos en esas páginas; proyecto elimina todas las excepciones, conservando texto y fechas. Quitar filtro escribe neutral, para no recuperar por herencia un filtro general. Cambiar la cantidad de fotos por tarjeta conserva los looks efectivos antes de remapear.

Ambas apps leen proyectos anteriores y normalizan presets desconocidos a Original. Se mantienen versión 1 del JSON, segundos Swift desde 2001, UUID en mayúsculas, roles de texto y fuentes. El fixture compartido es [photo_looks.polar](../PolarAndroid/app/src/test/resources/fixtures/photo_looks.polar).

El mismo dibujo de foto alimenta hoja, miniaturas, encuadre y exportación, también en moldes importados. PDF Android decodifica al tamaño útil del espacio × zoom × 300 ppp, con `EXPORT_MAX`. Los filtros afectan sólo las fotos; marcos y textos mantienen sus colores.

## Pruebas

- Android: `clean testDebugUnitTest lintDebug assembleDebug assembleRelease` terminó con **exit 0**, **187 pruebas, 0 fallos, 0 omitidas** en 34 suites y **0 errores de lint**. Hay 63 avisos de lint (dependencias, plurales candidatos, recursos y recomendaciones de KTX), que no impiden compilar; no se añadieron dependencias para silenciarlos.
- Emulador: **6 pruebas nativas, 0 fallos**, `DeviceSmokeTest` y `PhotoFiltersFlowTest` en una ejecución conjunta. Capturas posteriores: 72 por tamaño, 216 Android en total.
- Mac: `check.sh` pasa **10 suites** (las 7 originales y 3 nuevas), `build.sh` produce la app y `codesign --verify --deep --strict` valida la firma local. Capturas posteriores: 96.
- [Build Android](verificaciones/simetria-2026-10/android-build.txt), [resultados Android](verificaciones/simetria-2026-10/android-resultados.txt), [recorrido nativo](verificaciones/simetria-2026-10/android-recorrido.txt); [pruebas Mac](verificaciones/simetria-2026-10/mac-check.txt), [build Mac](verificaciones/simetria-2026-10/mac-build.txt) y [firma Mac](verificaciones/simetria-2026-10/mac-firma.txt).

Entregables en la raíz: `Polar.apk` de depuración y `Polar.app`, ambos versión **2.1.0**, build **3**. El APK no incluye el host temporal ni la foto de auditoría. `assembleRelease` comprueba compilación y reducción de recursos; la configuración existente usa firma de depuración si no hay firma de publicación, por lo que ese build de prueba no se presenta como paquete listo para Play Store.

Se añadieron comprobaciones de resolver, JSON cruzado, desconocidos, rangos, remapeo, cuatro alcances, Aplicar a todas/Deshacer, comparación sin guardar y agrupación de gestos. Los motores nativos prueban ocho presets × cinco muestras RGB, tolerancia ±1; ambos usan los mismos valores literales. El grano repite los mismos píxeles y en Mac se contrasta también a dos escalas. Exportación comprueba BW en áreas de fotos del PNG/PDF y conservación del marco de color.

El recorrido Android prueba navegación, encuadre/giro, texto, papel/orientación, Deshacer/Rehacer, recreación de actividad, autoguardado/reapertura, todas las fuentes/diseños y PDF/PNG. La nueva prueba de Filtros usa toques reales sobre presets/alcances y Deshacer, y mantiene/suelta la hoja para comparar; comprueba que el proyecto y los archivos fuente no cambien al comparar.

Durante ese recorrido se detectó un cierre nativo al rasterizar emoji cuando la animación medía temporalmente la hoja a cero. El motor omite escalas no positivas/no finitas; una prueba de regresión cubre esta medición y el recorrido completo se repitió correctamente.

## Capturas y límites

La matriz y la comparación por pantalla están en [auditoria-simetria-2026-10.md](auditoria-simetria-2026-10.md), con [inventario](capturas/simetria/inventario.json) e [instrucciones para repetir](capturas/simetria/README.md). Las PNG personales son locales y se excluyen de git; ningún servidor sirve Fotitos ni las capturas nativas.

`mercatto_test` presentó ANR de System UI en la auditoría inicial. Se conservó su almacenamiento y se usaron tres perfiles temporales Android 34, densidad 160, puerto 5560, con tamaños físicos 360×640, 411×891 y 1280×800. Las fotos de Fotitos se cargaron en bibliotecas aisladas de los hosts de captura. Esto verifica el emulador y componentes nativos, sin afirmar compatibilidad probada en todos los teléfonos físicos.

Mac captura las vistas SwiftUI reales en 1280×800 y 1728×1117 pt, Retina 2×; letra 130 % mediante copias de fuentes del host, sin cambiar la tipografía del renderer. Los hosts verifican la apariencia de las vistas, no un recorrido manual de cada popover/ventana del sistema.

Pendiente fuera de esta verificación: prueba con impresora física, dispositivos Android físicos y recorrido manual completo con VoiceOver/TalkBack y trackpad. Los eventos, etiquetas y acciones accesibles están implementados; no se declara una certificación manual de lectores de pantalla.

## Decisiones y costo de cambiarlas

| Decisión | Motivo y costo si cambia |
| --- | --- |
| Look opcional por colocación | Permite una sola foto dentro de una tira. Lectores antiguos ignoran el campo; para conservar estos filtros hay que usar ambas apps 2.1.0. No introduce otro nivel persistente por página. |
| Zoom positivo menor de 1 | Necesario para Ajustar sin recortar. Apps antiguas con rango 1…4 pueden rechazar esos proyectos; actualizar ambas antes de intercambiarlos. |
| Páginas mediante overrides de tarjetas | Reutiliza el formato existente. Cambiar la distribución conserva las fotos filtradas; la selección de páginas es de edición y no una regla permanente sobre futuras páginas. |
| Matrices y grano fijos | Cálido/Frío y otros valores no cuantificados por el encargo se fijaron en el contrato. Cambiarlos después alteraría proyectos guardados: requiere nueva versión de preset o migración. |
| Capa de foto filtrada a 300 ppp en PDF | Mantiene el grano espacial y la fidelidad del filtro; puede aumentar memoria de una foto grande. Existe tope de decodificación y manejo del fallo de memoria; el texto normal permanece vectorial. |
| Menú nativo anclado | Reutiliza posicionamiento de Popup/Popover y limita la tarjeta al área visible. No requiere biblioteca ni permisos adicionales. |
| Emuladores temporales | Evita modificar datos del AVD del usuario. Diferencias con sus dispositivos reales requieren la prueba física posterior indicada arriba. |

Código y documentación quedan en `fase-2-simetria-filtros`, con commits convencionales en español. No se hizo push ni merge ni se generaron llaves de publicación.
