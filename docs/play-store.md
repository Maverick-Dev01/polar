# Polar en Play Store

## Antes de la primera subida (lo hace el dueño de la cuenta)

1. Confirmar el `applicationId` definitivo (hoy `io.github.maverickdev01.polar`): no se puede cambiar después de publicar.
2. Crear la llave de subida (pide una contraseña; guárdala en un gestor de contraseñas):
   `keytool -genkeypair -v -keystore ~/polar-upload.jks -alias polar -keyalg RSA -keysize 4096 -validity 10000`
3. Agregar a `~/.gradle/gradle.properties` (nunca al repositorio):
   `POLAR_STORE_FILE=/Users/<usuario>/polar-upload.jks`, `POLAR_STORE_PASSWORD=…`, `POLAR_KEY_ALIAS=polar`, `POLAR_KEY_PASSWORD=…`
   Las cuatro propiedades `POLAR_*` deben estar juntas. Ya no hay firma debug como alternativa; si faltan, la publicación falla. Si ya distribuiste la versión GitHub, conserva su llave privada; revisa [actualizaciones-android.md](actualizaciones-android.md) antes de elegir la firma de Play.
4. `cd PolarAndroid && ./gradlew bundlePlay` → subir `app/build/outputs/bundle/play/app-play.aab` con *Play App Signing* activado. La build `play` desactiva el actualizador externo y elimina `REQUEST_INSTALL_PACKAGES`, como exige Google Play.
5. Publicar `docs/privacidad.md` en una URL pública (por ejemplo GitHub Pages de la cuenta personal) y pegarla en Play Console.

## Ficha

- **Nombre:** Polar · Fotos para imprimir
- **Descripción corta (≤ 80):** Convierte tus fotos en polaroids, boletos y calendarios listos para imprimir.
- **Descripción completa:** Elige un diseño (Polaroid, foto con canción y QR, boleto, película, calendario y más), pon tus fotos y se acomodan solas. Escribe un pie de foto para todas o uno distinto para cada tarjeta, con fecha, color y 20 tipos de letra. Imprime desde tu teléfono o guarda un PDF a tamaño real en Carta, Oficio, A4, 4 × 6 y más. Funciona sin internet, sin cuentas y sin anuncios: tus fotos nunca salen de tu teléfono.
- **Categoría:** Fotografía.
- **Capturas:** teléfono (4–8) y tablet de 10" (2–4), en claro y oscuro: Tus diseños, Catálogo, Editor, Texto por tarjeta, Terminar.

## Seguridad de los datos (Data safety)

- ¿Recopila datos? **No.** ¿Comparte datos? **No.**
- La build Play no consulta ni descarga actualizaciones de GitHub. Eliminación de datos: el usuario borra sus diseños dentro de la app. La versión distribuida por GitHub usa HTTPS para consultar/descargar actualizaciones a petición; revisa la política de privacidad por canal.

## Clasificación de contenido

Sin violencia, sin contenido generado por usuarios compartido públicamente, sin compras ni anuncios.
