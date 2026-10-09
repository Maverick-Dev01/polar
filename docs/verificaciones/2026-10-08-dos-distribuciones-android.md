# Polar 2.3.0 · dos distribuciones Android

Se revisaron los cambios de `fase-3-solidez` y se prepararon Android y Mac 2.3.0 / build 7. Un solo agente revisó importación, exportación, guía y separación de canales. `PolarVideo/` se conserva fuera de esta entrega.

## Distribución

| Archivo | Destino | Actualizaciones |
| --- | --- | --- |
| `Polar-2.3.0.apk` | Instalación desde GitHub | Ajustes → Actualizaciones |
| `Polar-2.3.0-play.aab` | Subir a Play Console | Google Play |
| `Polar-Mac-2.3.0.zip` | Apple Silicon, macOS 14+ | Descargar y sustituir la app cerrada |

Las dos variantes Android conservan `io.github.maverickdev01.polar` y la firma local permanente. No son dos instalaciones simultáneas. El AAB de subida no fija la firma final de Google Play: para mantener continuidad, hay que configurar Play App Signing con la llave existente antes de la primera publicación. No se regeneró ni se publicó la llave privada.

El cliente GitHub reside en `app/src/github/`, incluido sólo en debug y release. Play aporta `DistributionUpdates()` desde `src/play/`, excluye las fuentes del actualizador y elimina `REQUEST_INSTALL_PACKAGES`. Ajustes muestra el canal. El comando `python3 tools/android-release.py` prepara ambos; `--channel github` y `--channel play` preparan sólo uno. Los archivos `mapping` quedan separados por canal.

## Correcciones de la revisión

- Android: las regiones transparentes editadas del asistente pasan a dibujarse sobre el molde, igual que en Mac. Moverlas, redimensionarlas o cambiar su forma ya no deja la foto tapada por el arte original. La prueba reprodujo el fallo antes del cambio y pasó después.
- Ayuda compartida y ficha de Play: se corrigió la promesa de respaldo completo. El `.polar` guarda ajustes y referencias; no empaqueta fotos, máscaras ni molde. Esos recursos deben conservarse también.
- Privacidad y Seguridad de datos: se documentan los diagnósticos técnicos que Google declara para ML Kit. Las fotos se procesan localmente; no debe marcarse automáticamente «no recopila datos» ignorando los SDK.

## Evidencia automática

- Android: 383 pruebas unitarias, 0 fallos/errores/omitidas, después de separar los canales. `testDebugUnitTest lintDebug assembleDebug` y compilaciones firmadas completas.
- Play: prueba `PlayUpdateChannelTest` de ausencia de clases GitHub y permiso de instalación; falló antes del aislamiento y pasó después.
- Lint: 0 errores; debug y release 100 advertencias, Play 118 (incluye recursos no usados al excluir el actualizador). No se afirma que no existan advertencias.
- Mac: las 16 suites de `PolarMac/check.sh` pasaron; build y `codesign --verify --deep --strict` correctos.
- Emulador API 34, usuario Demo (10): 10 casos nativos reportados, **9 pasaron y 1 se omitió** (ML Kit opcional, sin fotografía de prueba). Editor con teclado real, frases, selección/copia, guías y PDF por calidad sobre fotos sintéticas. No se borró ni desinstaló la app.
- AAB: `bundletool 1.18.3 validate` correcto; manifiesto con paquete/versión esperados y sin permiso de instalación. `jarsigner` verifica la firma permanente. El `mapping` y DEX no contienen clases ni endpoint de actualizaciones GitHub.
- APK: `apksigner` verifica la misma firma permanente; SHA-256 y tamaño coinciden con `update.json`. APK no depurable y con permiso para el instalador.
- Las cuatro bibliotecas ELF de arm64-v8a/x86_64 del AAB tienen segmentos LOAD alineados al menos a 16 KiB. Esto no sustituye una ejecución en dispositivo de 16 KiB.
- `tools/check-release-channels.py` y `tools/check-release-signing.py`: selección de canales, ayuda/errores sin crear llaves y rechazo de firma incompleta.

## Archivos finales

| Archivo | Bytes | SHA-256 |
| --- | ---: | --- |
| APK | 5501729 | `018124fc51c148eece0c8a3c9c42baa7dfae196cdca215a49a165fd9f9196028` |
| AAB Play | 8643245 | `d34880e78f8ce613166a4e0c214f917dfb000c98dd11992868b338b1e53a9e0e` |
| ZIP Mac | 4086965 | `0e5391fb8b569a59fa412e07250054c38536c37b8be053b172fe62361ee0ef26` |

## Límites de esta entrega

No se publicó en Play Console ni se probó un teléfono físico o dispositivo de 16 KiB. Quedan las decisiones de Play App Signing, formulario de datos, política pública y pruebas de la cuenta. La segmentación positiva de Android depende del modelo opcional de Google; los casos de composición usan máscaras sintéticas. La versión Mac mantiene firma local, sin notarización.
