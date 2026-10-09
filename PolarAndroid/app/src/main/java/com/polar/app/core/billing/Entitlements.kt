package com.polar.app.core.billing

/** Funciones que una compra de Polar Pro podría desbloquear. Hoy ninguna está bloqueada (ver docs/monetizacion.md). */
enum class ProFeature { SEASONAL_DESIGNS, REMOVE_BACKGROUND, UNLIMITED_MOLDS, MAX_QUALITY }

/** Capa de «derechos»: la interfaz consulta si el usuario tiene Pro. Aún no se usa para bloquear nada. */
interface Entitlements {
    fun isPro(): Boolean
    fun hasFeature(f: ProFeature): Boolean
}

/** Implementación por defecto: todo desbloqueado. Se reemplazará por una basada en Google Play Billing. */
object AllUnlocked : Entitlements {
    override fun isPro(): Boolean = true
    override fun hasFeature(f: ProFeature): Boolean = true
}
