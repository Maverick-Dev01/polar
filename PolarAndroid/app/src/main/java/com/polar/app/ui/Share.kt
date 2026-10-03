package com.polar.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

object Share {
    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Devuelve false si no hay ninguna app que reciba el archivo. */
    fun file(context: Context, file: File, mime: String, title: String): Boolean {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uriFor(context, file))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return try {
            context.startActivity(Intent.createChooser(send, title))
            true
        } catch (e: ActivityNotFoundException) {
            Log.w("Polar", "No hay app para compartir $mime", e)
            false
        }
    }

    /** Devuelve false si no hay ninguna app que abra el archivo. */
    fun open(context: Context, uri: Uri, mime: String): Boolean {
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return try {
            context.startActivity(view)
            true
        } catch (e: ActivityNotFoundException) {
            Log.w("Polar", "No hay app para abrir $mime", e)
            false
        }
    }
}
