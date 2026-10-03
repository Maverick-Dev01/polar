# Polar · Auditoría de simetría, interacción y filtros

2 de octubre de 2026. **Prototipo aprobado e implementación completada en Android y Mac 2.1.0.**

El registro de la fase 1 se conserva más abajo. La aprobación del usuario autorizó el nuevo encuadre y una foto individual dentro de película; además amplió los filtros a una o varias páginas y pidió revisar el modo oscuro. Ver [verificación final](verificacion-simetria-filtros-2026-10-02.md) y [contrato implementado](filtros.md).

## Resultado y comparación

220 capturas nativas anteriores (132 Android + 88 Mac), 312 posteriores (216 Android + 96 Mac), más 144 del prototipo. Matriz completa claro/oscuro y 100/130 %: Android 360×640, 411×891, 1280×800; Mac 1280×800 y 1728×1117 pt. Dimensiones reales y límites del host en el [README](capturas/simetria/README.md) y [JSON](capturas/simetria/inventario.json). PNG locales, excluidas de git.

| Pantalla / regla corregida | Antes Android | Después Android | Después Mac |
| --- | --- | --- | --- |
| Inicio: líneas reservadas y menú anclado | [Antes](capturas/simetria/antes/antes-android-411x891-oscuro-130-inicio.png) | [Después](capturas/simetria/despues/despues-android-411x891-oscuro-130-inicio.png) | [Después](capturas/simetria/despues/despues-mac-1280x800-oscuro-130-inicio.png) |
| Catálogo: título1, descripción2, simetría y marca | [Antes](capturas/simetria/antes/antes-android-411x891-oscuro-130-catalogo.png) | [Después](capturas/simetria/despues/despues-android-411x891-oscuro-130-catalogo.png) | [Después](capturas/simetria/despues/despues-mac-1280x800-oscuro-130-catalogo.png) |
| Editor: bandeja acoplada y hoja libre | [Antes](capturas/simetria/antes/antes-android-411x891-oscuro-130-editor.png) | [Después](capturas/simetria/despues/despues-android-411x891-oscuro-130-editor.png) | [Después](capturas/simetria/despues/despues-mac-1280x800-oscuro-130-editor.png) |
| Fotos: fichas y acciones simétricas | [Antes](capturas/simetria/antes/antes-android-411x891-oscuro-130-fotos.png) | [Después](capturas/simetria/despues/despues-android-411x891-oscuro-130-fotos-amplio.png) | [Después](capturas/simetria/despues/despues-mac-1280x800-oscuro-130-fotos.png) |
| Diseño: fichas y distribución | [Antes](capturas/simetria/antes/antes-android-411x891-oscuro-130-diseno.png) | [Después](capturas/simetria/despues/despues-android-411x891-oscuro-130-diseno-amplio.png) | [Después](capturas/simetria/despues/despues-mac-1280x800-oscuro-130-diseno.png) |
| Texto: alcance adaptable y controles legibles | [Antes](capturas/simetria/antes/antes-android-411x891-oscuro-130-texto.png) | [Después](capturas/simetria/despues/despues-android-411x891-oscuro-130-texto-amplio.png) | [Después](capturas/simetria/despues/despues-mac-1280x800-oscuro-130-texto.png) |
| Papel: unidades y validación | [Antes](capturas/simetria/antes/antes-android-411x891-oscuro-130-papel.png) | [Después](capturas/simetria/despues/despues-android-411x891-oscuro-130-papel-amplio.png) | [Después](capturas/simetria/despues/despues-mac-1280x800-oscuro-130-papel.png) |
| Encuadre: marco real, gesto/anillo y acciones | [Antes](capturas/simetria/antes/antes-android-411x891-oscuro-130-encuadre.png) | [Después](capturas/simetria/despues/despues-android-411x891-oscuro-130-encuadre.png) | [Después](capturas/simetria/despues/despues-mac-1280x800-oscuro-130-encuadre.png) |
| Terminar: acciones mínimas, resumen y vista completa Mac | [Antes](capturas/simetria/antes/antes-android-411x891-oscuro-130-terminar.png) | [Después](capturas/simetria/despues/despues-android-411x891-oscuro-130-terminar.png) | [Después](capturas/simetria/despues/despues-mac-1280x800-oscuro-130-terminar.png) |
| Ajustes: contraste y Settings nativo Mac | [Antes](capturas/simetria/antes/antes-android-411x891-oscuro-130-ajustes.png) | [Después](capturas/simetria/despues/despues-android-411x891-oscuro-130-ajustes.png) | [Después](capturas/simetria/despues/despues-mac-1280x800-oscuro-130-ajustes.png) |
| Bienvenida: contraste y espaciado | [Antes](capturas/simetria/antes/antes-android-411x891-oscuro-130-bienvenida.png) | [Después](capturas/simetria/despues/despues-android-411x891-oscuro-130-bienvenida.png) | [Después](capturas/simetria/despues/despues-mac-1280x800-oscuro-130-bienvenida.png) |
| Filtros: cuatro alcances, presets reales y ajustes | Nueva herramienta | [Después](capturas/simetria/despues/despues-android-411x891-oscuro-130-filtros-amplio.png) | [Después](capturas/simetria/despues/despues-mac-1280x800-oscuro-130-filtros.png) |

[Menú anclado Android](capturas/simetria/despues/despues-android-411x891-oscuro-130-inicio-menu.png). Las demás variantes y todos los antes Mac están en el inventario. Los campos opcionales, el zoom positivo menor de1 y las decisiones de implementación se explican en la verificación final. Se revisaron capturas oscuro130 % de filtros, catálogo, encuadre, menú y Terminar, además de los controles compartidos.

## Registro histórico de la fase 1 (previo a aprobación)

Se leyó completo [el encargo](prompts/2026-10-02-simetria-interaccion-y-filtros.md), el diseño previo, los README y ambas verificaciones. Se conservan las funciones de las apps: esta entrega contiene documentación, un prototipo HTML y hosts de captura. No incorpora todavía filtros ni cambia los controles de producción. Rama: `fase-2-simetria-filtros`, creada desde `fase-1-base`; identidad comprobada: José Catalino <josecatalino.code@gmail.com>. No se hará push ni merge.

## Evidencia y alcance

- **Android: matriz nativa pendiente.** Se compiló e instaló un host de los componentes Compose de producción con almacenamiento aislado y una fotografía de `Fotitos/Mejoradas`. Los intentos en `mercatto_test`, con arranque desde snapshot, arranque limpio sin borrar datos y GPU nativa, terminaron con ANR de `com.android.systemui`; también se registró un fallo de Nexus Launcher al redimensionar el display. La captura inicial mostraba ese diálogo del sistema, por lo que fue descartada; la comprobación posterior de ventana activa rechazó la matriz. **Hay cero PNG Android aceptadas.** No se declara comprobada visualmente la app en los tres tamaños. La auditoría Android se apoya en el código; Encuadre fuerza tema oscuro. Tamaño/densidad fueron restaurados y se cerró sólo el emulador iniciado para esta auditoría. El host temporal y su foto se retiraron de `androidTest`. Antes de corregir la app, se debe repetir la matriz nativa en un emulador estable, sin omitir este requisito.
- **Mac: 88 PNG válidas.** Las mismas vistas SwiftUI en un host temporal, con almacenamiento propio en `/private/tmp`. Áreas de contenido de 1280 × 800 y 1728 × 1117 pt; ambos temas y escalas. El 130 % es una prueba de estrés: escala las fuentes de copias de las vistas, sin cambiar contenedores ni tipografía del renderer. macOS no ofrece a esta app un ajuste global equivalente al `fontScale` de Android. Las PNG son Retina, a 2 píxeles por punto. Ajustes, Bienvenida y Terminar se capturan como componentes; su presentación modal actual se confirma en el código.
- **Propuesta: 144 PNG.** HTML con teléfono pequeño/normal, tablet y Mac, ambos temas y letras 100/130 %. Imágenes ilustrativas y filtros CSS de demostración. No se presenta como resultado del renderer ni como prueba de exportación. Las capturas de propuesta **no son el “después” de una app implementada**: esa segunda ronda corresponde a la fase posterior al visto bueno.
- PNG locales, excluidas de git, en [capturas/simetria](capturas/simetria/). `antes/` contiene las vistas nativas; `propuesta/` el HTML. El [inventario](capturas/simetria/inventario.json) identifica sólo las capturas aceptadas; [el README](capturas/simetria/README.md) explica cómo repetirlas. Los intentos rechazados permanecen fuera de esa carpeta. Los hosts están junto al [prototipo](prototipos/simetria-2026-10/index.html) para repetir las capturas después.

El grafo MCP no tiene indexados PolarAndroid ni PolarMac; se usaron los archivos indicados por el encargo y búsquedas de respaldo. Impeccable no está disponible en las skills instaladas; se aplicó la skill de diseño disponible, sin instalar dependencias.

## Problemas comprobados y cambios previstos

Las rutas Android de la tabla parten de `PolarAndroid/app/src/main/java/com/polar/app/`; las Mac de `PolarMac/Sources/`. Las líneas corresponden al estado auditado.

| Pantalla | Evidencia | Problema / regla | Arreglo mínimo después de aprobar |
| --- | --- | --- | --- |
| Inicio Android | `ui/home/HomeScreen.kt:206` | Nombre en altura fija 42; subtítulo sin dos líneas reservadas. A1/A6. | Nombre una línea y subtítulo dos reservadas, elipsis y nombre completo accesible. |
| Biblioteca Mac | `LibraryViews.swift:84` | Nombre con dos líneas variables, subtítulo sin reserva; padding14 y margen32. A1/A3/A6. | Nombre una línea, subtítulo dos reservadas; padding16, margen24. |
| Menú de proyecto | Android `HomeScreen.kt:135`; Mac `LibraryViews.swift:86/111` | Hoja inferior Android; menú convencional y sheet de renombrado Mac. B2/B3. | Tarjeta anclada con cinco fichas iguales; popover Mac y renombrado en línea. |
| Catálogo Android | `ui/catalog/CatalogContent.kt:117` | Imagen124/padding10, título ilimitado, descripción sólo `maxLines=2`; selección sólo por borde; etiqueta omite descripción. A1/A3/A4. | Imagen proporcional, título1, descripción2 reservadas y elipsis; padding16, marca visible, etiqueta completa. |
| Catálogo Mac | `PolarApp.swift:184` | Título dos líneas dentro de altura31, sin descripción y selección sólo por color/borde. A1/A4. | La misma estructura de tarjeta que Android, con marca y descripción accesible. |
| Editor Android | `ui/editor/EditorScreen.kt:203`; `ToolPanel.kt:39` | **El lienzo ya se reduce:** el panel está en la misma Column, sin velo. Lo genérico son radio28, sombra8 y barrita; sólo hay altura46 %. B1. | Reutilizar la estructura; bandeja acoplada, pestañas, tira de fichas y dos alturas. Conservar Atrás124. |
| Editor Mac | `PolarApp.swift:103/345` | Inspector fijo320 con selector segmentado, distinto lenguaje visual. B1. | Mantenerlo lateral y adoptar pestañas/fichas; la hoja queda libre. |
| Barra superior | Android `EditorScreen.kt:241`; `CatalogContent.kt:63` | Editor fuerza nombre1 con elipsis. A7. | Dos líneas para título, estado aparte y acceso al nombre completo. Mac ya tiene nombre editable en línea. |
| Paneles/contexto Android | `ContextBar.kt:24`; `panels/PhotosPanel.kt:60`, `DesignPanel.kt:119`, `TextPanel.kt:86` | Contexto fijo64; pares de acciones intrínsecos; alcance Texto fuerza una línea. A2/A5. | Objetivos48/56, anchos iguales y columna cuando el texto al130 % no quepa. |
| Controles Mac | `PolarApp.swift:373/544/635` | Pares de botones de ancho intrínseco; colores de25 y selección sólo por borde. A2/A4/B5. | Anchos iguales, `ViewThatFits`, zonas táctiles48 y marca. |
| Encuadre Android | `ui/editor/CropScreen.kt:64/94` | Vista fija380, sin marco real, guías permanentes y tres sliders. C. | Tarjeta del renderer, arrastre/pellizco, anillo, guías durante gesto y ajuste fino. |
| Encuadre Mac | `PolarApp.swift:559` | Miniatura original de136 con `scaledToFit` y tres sliders; no representa el encuadre efectivo. C. | Marco real, arrastre/magnificación, rueda con ⌘, teclado, anillo y ajuste fino accesible. |
| Terminar Android | `ui/editor/FinishScreen.kt:73/125` | Acciones con altura fija56 y textos compitiendo en una fila. A2. | `heightIn(min=56)`, anchos iguales y flujo adaptable. **Revisar ya abre Encuadre63.** |
| Terminar Mac | `LibraryViews.swift:30/170/193` | Sheet, cuatro botones intrínsecos y plurales fijos. A2/B3. | Vista completa, acciones iguales/adaptables, plurales; Revisar abre el nuevo encuadre. |
| Ajustes/Bienvenida Android | `ui/settings/SettingsScreen.kt:50`; `ui/onboarding/OnboardingScreen.kt:79` | Márgenes20/24 y principal mínimo52. A2/A3. | Márgenes16/24 y principal56; segmentos que admitan texto grande. |
| Ajustes/Bienvenida Mac | `LibraryViews.swift:28/154` | Ajustes sheet y botones intrínsecos en Bienvenida. B3/A2. | `Settings` scene nativa; bienvenida cuidada y acciones de igual tamaño. |
| Compartidos | Android `ui/components/Controls.kt:72`; Mac `PolarApp.swift:681` | Etiquetas/valores de sliders sin columna reservada ni baseline. A5. | Encabezado con valor alineado, reutilizado por el anillo y ajustes. |
| Tokens/movimiento | Android `ui/theme/Shape.kt:7`, `ui/Motion.kt:10` | Hay radios, pero se evaden; falta Spacing; Motion sólo consulta reducción. A3/A4/B4. | Reusar radios, tokens base4 y curvas240 ms; equivalentes Mac y movimiento reducido. |

## Propuesta visual: mesa de revelado

El prototipo mantiene crema `#F7F2EB`, vino `#7A293B`, texto `#2B2221`, mesa `#E8E3DC` y bordes `#E4DAD0`. En oscuro: fondo `#1D1A19`, superficies `#2A2524`, mesa `#141211`, acento `#FFB2BC` y texto `#EDE0DD`. Papel e impresión permanecen blancos. Gelasio para marca/títulos, Caveat para dedicatorias; ambas fuentes ya pertenecen al proyecto, con sus licencias. Los assets del navegador usan una ilustración, nunca las fotos personales.

Tokens propuestos: `xs=4`, `s=8`, `m=16`, `l=24`, `xl=32`; pantalla16 en teléfono/24 en ancho, tarjetas con gap/padding16. Radios8 para controles/miniaturas,12 para tarjetas y20 para superficies; bordes1 y selección2 con marca. Íconos24, objetivos48 y acciones principales56 **mínimos**, sin recortar textos. Reservas de líneas en todas las tarjetas; sus tamaños crecen juntos cuando aumenta la letra. Estos tokens son para la interfaz: no se sustituyen las medidas de papel ni la geometría de impresión.

Se compararon tres direcciones: mantener una hoja inferior personalizada, una barra flotante sobre el papel y la bandeja acoplada. Se elige la bandeja del encargo porque mantiene visible el papel y enlaza con los separadores de un álbum. En teléfono, las pestañas Fotos/Filtros/Diseño/Texto/Papel se integran al borde; una tira compacta de fichas permite elegir rápido. «Más opciones» y un gesto cambian a la altura amplia, aproximadamente la mitad disponible. El contenido avanzado desplaza dentro de la bandeja, sin tapar la hoja. En ancho≥840 se convierte en panel fijo. Las transiciones duran240 ms y se desactivan con reducir movimiento.

El menú se ancla al proyecto y levanta ligeramente esa tarjeta; cinco acciones en cuadrícula de 2×3. Su posición se limita al área visible de la biblioteca cuando no cabe debajo de la tarjeta. No hay velo. El encuadre muestra una polaroid real, imagen sobrante atenuada, arrastre, tercios durante el gesto, anillo1×–4×, cinco acciones iguales, navegación y ajuste fino. El HTML demuestra arrastre, doble toque, teclado, anillos, alcance, aplicar a todas con Deshacer, comparar, expansión y menú. Pellizco, vibración, VoiceOver/TalkBack, exportación y matemática del renderer se verifican al implementar las apps.

## Dos decisiones necesarias

1. **Filtro de una foto en película.** Cada tira contiene cinco fotos y el contrato indicado guarda `photoLook` por **tarjeta**, en `cardOverrides`. Seguir ese JSON exactamente filtra las cinco fotos juntas. Recomendación mínima: en esos diseños llamar al alcance «Sólo esta tira (5 fotos)». Si se desea filtrar sólo una foto de la tira, hace falta autorizar otro ajuste por colocación y su resolver; el contrato del encargo no lo representa. El prototipo usa una polaroid de una foto y no oculta esta diferencia.
2. **Ajustar sin recortar.** Los dos validadores aceptan zoom1–4; PhotoFit usa `max` para llenar. Una foto con otra proporción requiere zoom<1 para caber entera. Recomendación: conservar las claves de PhotoPlacement y permitir zoom positivo menor de1, calculado en PhotoFit/renderer. Ambos lectores nuevos deben actualizarse juntos; versiones antiguas que validan1–4 rechazarían esos nuevos encuadres. Si se exige conservar también el rango, «Ajustar» completo no se puede prometer. **No se cambia silenciosamente el modelo.**

## Contrato de filtros y pendientes

Las matrices propuestas, composición y grano se detallan en [filtros.md](filtros.md). Son decisiones documentadas para revisar, todavía sin motor implementado. Se añaden PhotoLook/LookResolver junto al modelo y TextResolver, actualizando decodificación, validación, `isEmpty` y remapeo de overrides. Desconocido→original; proyectos antiguos sin campo→neutral. Fechas Swift, UUID en mayúsculas y roles de texto se conservan.

El filtro se aplica sólo en las rutas compartidas de dibujo de fotos, incluidas las plantillas importadas. El encuadre reutiliza PhotoFit y esa ruta. La caché de presets depende de foto, encuadre, look, tamaño e índice de tarjeta; se genera en segundo plano y queda limitada. Las miniaturas de Inicio proceden del mismo renderer. Se preservan las colas de guardado y los originales.

Pendientes que sí encajan: validar antes de confirmar `edit()/change()`, capturar Exception al renombrar/duplicar Android conservando cancelación, limpiar Rehacer al editar en una transacción, plurales y «pulg.», y decode Android al tamaño de foto×zoom×300ppp con topeEXPORT_MAX. Mac ya muestrea así y comprime JPEG: no se sustituye esa solución. Android «Revisar» ya funciona; Mac se conectará al nuevo encuadre.

## Comprobaciones de esta entrega

- El host Mac compiló y capturó las 11 vistas/paneles en dos tamaños, temas y escalas: 88 PNG. Se revisó que la hoja tuviera las fotos y el formato 2×2 del proyecto aislado, usando la vista previa del renderer de producción.
- El prototipo se recorrió en el navegador. Se comprobó la misma altura de seis tarjetas del Catálogo al 130 % (240.796875 px, con descripción reservada de 43.3125 px), aplicar a todas y deshacer, comparación sin quedar activada después de soltar, anillo de zoom, ajuste fino y navegación de foto 1 a 2. Se verificó que papel y bandeja no se solaparan. La revisión de Biblioteca detectó un ancho intrínseco provocado por nombres largos: se corrigió en el prototipo y se repitieron sus capturas y las de Encuadre.
- Se contrastaron los 40 resultados RGB de la tabla de filtros con sus ocho matrices mediante cálculo directo. Esto comprueba el documento, **no** la futura rasterización nativa.
- La compilación del host Android pasó; su ejecución visual no pasó por el ANR del sistema descrito arriba. Las suites de regresión, lint, exportación y firma de ambas apps corresponden a la fase de implementación y no se declaran ejecutadas en esta entrega.
- No se modificaron fuentes de producción, originales, `Polar.app` ni `Polar.apk`. No se añadió ninguna dependencia ni se publicó nada. Las fuentes WOFF del prototipo proceden de las TTF ya presentes, conservando sus licencias.

## Salida de esta fase

La instrucción exige: **«Auditoría + prototipo (…) Para y pide visto bueno al usuario.»** Se espera aprobación del prototipo y de las dos decisiones anteriores antes del plan y la implementación. Después se sigue el orden del encargo: tokens, modelos/fixtures, motores, bandeja/menú, encuadre, herramienta Filtros y paridad Mac. La ronda final repetirá capturas nativas, las siete suites Mac, la suite/lint/build Android, recorrido con Fotitos y copia de APK. Nada de eso se declara terminado en esta primera entrega.
