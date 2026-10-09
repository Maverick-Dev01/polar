# Polar en Google Play · lista de lanzamiento

Marca con [x] a medida que avances. Lo que requiere cuenta de Play Console lo hace el dueño de la cuenta; el resto ya está listo en el repositorio.

## 0. Distribuciones 2.3.0

- [x] Android **2.3.0, `versionCode` 7**: `release` para GitHub y `play` para Google Play. Cada nueva publicación necesita un `versionCode` mayor.
- [x] Mac: mismo número de versión (2.3.0 / 7) en `PolarMac/Info.plist`.

## 1. Antes de la primera subida

- [ ] Confirmar el `applicationId` definitivo (hoy `io.github.maverickdev01.polar`): no se puede cambiar después de publicar.
- [ ] **Marca:** buscar «Polar» en Google Play, en la base de marcas (IMPI/EUIPO/USPTO) y en la web. *Polar Electro* es una marca conocida (relojes deportivos); por eso el nombre sugerido lleva descriptor: «Polar: fotos para imprimir». Si hay conflicto, usar otro nombre.
- [ ] Conservar la llave de distribución existente en `.polar-signing/`. No generar otra para reemplazar APK ya distribuidos. El AAB local también se firma con esta llave; al configurar Play App Signing, decide la llave de firma final antes de publicar.
- [ ] Agregar a `~/.gradle/gradle.properties` (nunca al repositorio): `POLAR_STORE_FILE`, `POLAR_STORE_PASSWORD`, `POLAR_KEY_ALIAS`, `POLAR_KEY_PASSWORD`. Las cuatro deben estar juntas; si faltan, la publicación falla (no hay firma debug de respaldo).
- [ ] Si ya distribuiste el APK de GitHub, conserva su llave privada y lee [actualizaciones-android.md](actualizaciones-android.md) antes de elegir la firma de Play.

## 2. Compilar el paquete (AAB)

- [ ] `cd PolarAndroid && ./gradlew bundlePlay` (el tipo de compilación se llama `play`, no `release`; la tarea `bundlePlayRelease` no existe). Salida: `app/build/outputs/bundle/play/app-play.aab`.
- Alternativa recomendada desde la raíz: `python3 tools/android-release.py` prepara ambos canales usando la firma privada existente. El AAB para subir está en `release-assets/Polar-2.3.0-play.aab`; conserva su `Polar-2.3.0-play-mapping.txt`. `--channel play` prepara sólo Play; `--channel github` sólo el APK y `update.json`.
- [ ] La build `play` desactiva el actualizador de GitHub (`GITHUB_UPDATES_ENABLED=false`) y elimina `REQUEST_INSTALL_PACKAGES` (`app/src/play/AndroidManifest.xml`), como exige Google Play.
- [ ] En Play Console: **Integridad de la app → Firma de apps (Play App Signing) activado**; tu llave es sólo la de *subida*.
- [ ] Nivel de API: `targetSdk = 36`, `compileSdk = 36`, `minSdk = 26` (Android 8). Google exige apuntar al nivel más reciente o a uno cercano; confirma el requisito vigente en Play Console antes de subir.

## 3. Política de privacidad pública (GitHub Pages)

- [ ] Repositorio: `https://github.com/Maverick-Dev01/polar` (cuenta personal).
- [ ] En GitHub: **Settings → Pages → Build and deployment → Source: Deploy from a branch → Branch: `global` (o la rama principal) y carpeta `/docs` → Save**. Si el repositorio es privado, Pages puede requerir plan de pago o hacerlo público.
- [ ] URL resultante (confírmala en Pages al terminar el despliegue): `https://maverick-dev01.github.io/polar/privacidad` (GitHub Pages sirve `docs/privacidad.md` como página; si no, usa `.../privacidad.html`).
- [ ] Abrirla en una ventana privada y pegarla en **Contenido de la app → Política de privacidad**.

## 4. Prueba cerrada

- [ ] **Cuentas personales nuevas:** según la política de Google vigente en 2026, antes de pedir acceso a producción hay que correr una **prueba cerrada con al menos 12 testers que permanezcan 14 días seguidos**. Confirma el requisito exacto y si aplica a tu cuenta en Play Console (**Prueba y lanzamiento → Prueba cerrada**), porque Google lo ajusta.
- [ ] Crear la pista cerrada, subir el AAB, crear una lista de correos de testers (Gmail) y compartirles el enlace de opt-in.
- [ ] Llevar la cuenta: 12 testers aceptados, 14 días corridos, y recoger sus comentarios.
- [ ] Después: **Solicitar acceso a producción** y contestar el cuestionario.

## 5. Seguridad de los datos (Data safety)

- [ ] No marcar automáticamente «no recopila datos»: además del código propio hay que declarar los diagnósticos de **ML Kit**. Según su [documentación oficial](https://developers.google.com/ml-kit/android-data-disclosure), recopila información del dispositivo y app, identificadores técnicos, rendimiento, configuración, tamaños y eventos/errores. Clasifica esos datos y finalidades según el formulario vigente y la versión del SDK. Polar no tiene cuentas ni analítica propia y no envía fotos a servidores.
- [ ] Quitar fondo: **ML Kit de segmentación de sujetos corre en el dispositivo**, y Google Play Services descarga el modelo. La foto permanece local; modelo y diagnósticos están descritos en `privacidad.md`.
- [ ] ¿Datos cifrados en tránsito? Sí: Google documenta HTTPS para los datos de ML Kit.
- [ ] ¿Puede el usuario pedir que se borren sus datos? No se guardan datos fuera del teléfono; desinstalar la app o borrar diseños dentro de ella elimina todo.
- [ ] La build Play no consulta ni descarga actualizaciones de GitHub; la de GitHub (fuera de Play) sí, a petición, con HTTPS.

## 6. Clasificación de contenido (cuestionario IARC)

- [ ] Categoría: **Utilidad / Productividad** (no juego).
- [ ] Violencia, sangre, contenido sexual, lenguaje soez, drogas, apuestas: **No** a todo.
- [ ] ¿Contenido generado por usuarios compartido con otros? **No** (los archivos sólo salen cuando el usuario los guarda o comparte).
- [ ] ¿Comparte la ubicación? **No**. ¿Permite compras? **No** (hasta activar Polar Pro; ver [monetizacion.md](monetizacion.md)). ¿Anuncios? **No**.
- [ ] Resultado esperado: apta para todo público.
- [ ] Otros formularios: **Público objetivo** (13+ o todo público; no dirigida a niños), **Anuncios: no contiene**, **Declaración de permisos** (ninguno sensible), **Apps de gobierno / salud / finanzas: no**.

## 7. Recursos de la ficha

- [ ] Ícono 512 × 512 px, PNG de 32 bits, ≤ 1 MB.
- [ ] Gráfico de funciones (feature graphic) 1024 × 500 px, JPG o PNG sin transparencia.
- [ ] Capturas de teléfono: **mínimo 2**, recomendado 4–8 (relación 16:9 a 9:16, lado menor ≥ 320 px). Sugeridas: Tus diseños, Catálogo, Editor, Texto por tarjeta, Terminar; en claro y oscuro (`docs/capturas/`).
- [ ] Capturas de tablet de 7" y 10": opcionales, 2–4 cada una.
- [ ] Opcional: video de YouTube.

## 8. Texto de la ficha (listo para pegar)

- **Nombre (≤ 30 caracteres):** `Polar: fotos para imprimir` (26). Ver nota de marca en el punto 1.
- **Descripción corta (≤ 80):** `Polaroids, boletos y más listos para imprimir. Sin cuentas ni anuncios.` (71 caracteres)
- **Categoría:** Fotografía. **Etiquetas:** impresión de fotos, polaroid, collage.
- **Contacto:** josecatalino.code@gmail.com. **Política:** URL del punto 3.
- **Descripción completa (≤ 4000):**

```
Polar convierte tus fotos en tarjetas listas para imprimir: polaroids, fotomatón, vinilo, casete, collage, cinta washi y muchos diseños más. Eliges un diseño, pones tus fotos y se acomodan solas en la hoja.

DISEÑOS
• Clásicos: Polaroid, Instantánea ancha, Fotomatón y más.
• Música: Vinilo y Casete, y "Foto + canción" con un código QR que abre tu canción en el celular de quien lo escanee.
• Libre y ocasiones: Collage, Cinta washi, boletos, película y calendarios.
• Busca un diseño por nombre, sin importar acentos ni mayúsculas, y cámbialo cuando quieras: tus fotos y textos se conservan.

TUS FOTOS
• Agrega fotos sueltas o una carpeta completa.
• Encuadra cada foto con los dedos (acércala y muévela).
• Quita el fondo de una foto con un toque: el recorte se hace en tu teléfono.
• Filtros: blanco y negro, sepia, luz, contraste y grano. Mantén pulsada la hoja para comparar con el original.
• Avisos cuando una foto es muy chica para verse nítida en papel.

TU TEXTO
• Escribe una vez para todas las tarjetas, o dale a una su propio texto.
• 120 frases listas para adaptar, la fecha de la foto, colores y 20 tipos de letra.
• Color y letra de todo el diseño con un solo toque.

TUS MOLDES
• Importa una imagen con huecos para tus fotos y úsala como diseño propio.
• Todos tus moldes quedan guardados en "Mis moldes".

IMPRIME A TAMAÑO REAL
• Papel Carta, Oficio, A4, 4 × 6 y más, con orientación y márgenes ajustables.
• Marcas de corte para recortar cada tarjeta.
• Guarda un PDF o una imagen, o imprime directo desde el teléfono. Elige entre un archivo ligero para enviar o la mejor calidad para imprimir.
• Si imprimes, hazlo al 100 % (sin "ajustar a la página") para que cada tarjeta salga de su medida exacta.

TUS DISEÑOS, SIEMPRE A SALVO
• Se guardan solos; deshacer y rehacer cuantas veces quieras.
• Lo que borras se puede recuperar durante 7 días.
• Guarda los ajustes de tu diseño en un archivo .polar. Conserva también las imágenes originales y los moldes: las fotos no van incluidas en ese archivo.

PRIVACIDAD
• Sin cuentas, sin anuncios y sin analítica.
• Tus fotos nunca salen de tu teléfono: se procesan en el dispositivo.
• Diseñar, guardar e imprimir funciona sin internet. (Quitar fondo puede descargar una vez un modelo de Google Play Services.)

Polar es una app independiente y no está afiliada a ninguna otra marca.
```

## 9. Envío

- [ ] Completar todas las secciones de **Contenido de la app** (privacidad, anuncios, acceso a la app, clasificación, público objetivo, seguridad de los datos).
- [ ] Subir primero a la pista cerrada; tras los 14 días, pedir producción y publicar por etapas (p. ej. 20 % → 100 %).
- [ ] Guardar el AAB y su `mapping.txt` de cada versión.
