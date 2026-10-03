package com.polar.app.data

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import com.polar.app.engine.FontProvider
import com.polar.app.engine.styleOf
import java.util.concurrent.ConcurrentHashMap

class AndroidFontProvider(private val context: Context) : FontProvider {
    private val cache = ConcurrentHashMap<String, Typeface>()

    override fun typeface(fontName: String, bold: Boolean, italic: Boolean): Typeface {
        val choice = FontCatalog.find(fontName)
        val key = "${choice.id}|$bold|$italic"
        cache[key]?.let { return it }
        val loaded = load(choice, bold, italic)
        if (loaded == null) {
            // El respaldo no se guarda: un fallo pasajero no debe fijar la fuente del sistema.
            return Typeface.create(Typeface.DEFAULT, styleOf(bold, italic))
        }
        cache[key] = loaded
        return loaded
    }

    private fun load(choice: FontChoice, bold: Boolean, italic: Boolean): Typeface? {
        val asset = choice.asset
        if (asset != null) {
            // Fuente variable que arranca en delgado: se fija el grosor 400 o 700 en el eje wght.
            val weight = if (bold) 700 else 400
            val built = try {
                Typeface.Builder(context.assets, asset)
                    .setFontVariationSettings("'wght' $weight")
                    .setWeight(weight)
                    .build()
            } catch (e: Exception) {
                Log.w("Polar", "No se pudo cargar la fuente ${choice.id}", e)
                null
            }
            if (built == null) {
                Log.w("Polar", "No se pudo cargar la fuente ${choice.id}")
                return null
            }
            return if (italic) Typeface.create(built, Typeface.ITALIC) else built
        }
        val res = choice.res ?: return styled(Typeface.DEFAULT, bold, italic)
        val base = try {
            ResourcesCompat.getFont(context, res)
        } catch (e: Exception) {
            Log.w("Polar", "No se pudo cargar la fuente ${choice.id}", e)
            null
        }
        if (base == null) {
            Log.w("Polar", "No se pudo cargar la fuente ${choice.id}")
            return null
        }
        return styled(base, bold, italic)
    }

    private fun styled(base: Typeface, bold: Boolean, italic: Boolean): Typeface {
        val style = styleOf(bold, italic)
        return if (style == Typeface.NORMAL) base else Typeface.create(base, style)
    }
}
