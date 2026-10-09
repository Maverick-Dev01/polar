# Monetización de Polar (propuesta)

> **Estado actual: no hay cobro y nada está bloqueado.** Polar es gratis y completa en las dos apps. Existe una capa de «derechos» (`Entitlements`) que hoy devuelve «todo desbloqueado» (`AllUnlocked`) y no se consulta para limitar ninguna función. Este documento sólo describe el plan para cuando exista la cuenta de Play Console.

## Modelo propuesto (freemium, sin anuncios)

- **Sin anuncios, nunca.** Tampoco analítica ni rastreo.
- **Polar Pro:** compra única (no suscripción), entre **49 y 79 MXN**.
- **Gratis:** todo lo demás: diseños base, filtros, texto, papel, impresión, PDF e imagen, Mis moldes hasta **3** moldes.
- **Pro (`ProFeature`):**
  - `SEASONAL_DESIGNS`: diseños nuevos de temporada.
  - `REMOVE_BACKGROUND`: quitar el fondo de una foto.
  - `UNLIMITED_MOLDS`: Mis moldes sin límite (gratis hasta 3).
  - `MAX_QUALITY`: calidad de exportación Máxima.
- Los proyectos ya creados nunca se pierden ni se vuelven ilegibles si no hay Pro: sólo se limita crear/usar la función nueva.

## Capa de derechos (ya incluida)

- Android: `app/src/main/java/com/polar/app/core/billing/Entitlements.kt` (`interface Entitlements { isPro(); hasFeature(f) }`, `enum ProFeature`, `object AllUnlocked`), expuesta como `AppContainer.entitlements`. Prueba: `EntitlementsTest`.
- Mac: `PolarMac/Sources/Entitlements.swift` (`protocol Entitlements`, `enum ProFeature`, `struct AllUnlocked`). Prueba: `Tests/EntitlementsChecks.swift` (corre con `check.sh`).
- Cuando se active el cobro: cada punto de entrada (botón «Quitar fondo», selector de calidad, crear molde número 4, catálogo de temporada) consultará `hasFeature(...)` y, si es falso, mostrará una hoja «Polar Pro» en vez de bloquear en silencio.

## Cómo conectar Google Play Billing (después)

1. Requisitos: cuenta de Play Console, app subida a una pista de prueba, cuenta de comerciante y perfil de pagos.
2. Dependencia: `com.android.billingclient:billing-ktx` (última versión estable; revisar antes de añadir). Sólo en la build `play`; la build GitHub no cobra y debe seguir con `AllUnlocked`.
3. En Play Console: **Monetizar → Productos → Productos únicos**, crear el producto **`polar_pro`** (49–79 MXN, activo).
4. Implementar `PlayBillingEntitlements : Entitlements`:
   - Conectar `BillingClient`, `queryProductDetails("polar_pro")` y lanzar `launchBillingFlow`.
   - En `PurchasesUpdatedListener`: si el estado es `PURCHASED`, **reconocer la compra** (`acknowledgePurchase`) en menos de 3 días o Google la reembolsa automáticamente.
   - **Restaurar:** al abrir la app y desde Ajustes, `queryPurchasesAsync(ProductType.INAPP)` para reactivar Pro tras reinstalar o cambiar de teléfono.
   - Guardar en caché local (DataStore) el último estado conocido para funcionar sin internet.
5. Reemplazar `AllUnlocked` en `AppContainer` según el flavor/build type (`play` → Billing; resto → `AllUnlocked`).
6. Probar con testers de licencia y la pista de prueba cerrada antes de producción; actualizar la política de privacidad (Google Play procesa el pago; Polar no recibe datos de pago) y el formulario de Seguridad de datos («Compras»).

## Mac

- **Si se publica en la Mac App Store:** StoreKit 2 (`Product.products(for: ["polar_pro"])`, `purchase()`, `Transaction.currentEntitlements` para restaurar), con la misma lógica de derechos tras `Entitlements`.
- **Si sólo se distribuye fuera de la tienda (hoy):** no hace falta licencia ni cobro; se queda con `AllUnlocked`.
- La compra de Android y la de Mac son independientes (cada tienda cobra por su lado).

## Pendiente de decidir

- Precio final dentro de 49–79 MXN.
- Si los diseños de temporada ya existen antes de activar el cobro (hoy no hay ninguno marcado como de temporada).
