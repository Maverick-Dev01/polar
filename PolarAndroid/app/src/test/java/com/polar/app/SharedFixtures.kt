package com.polar.app

import java.io.File

/** Las fixtures compartidas con la Mac viven en `shared-fixtures/`, en la raíz del repositorio. */
object SharedFixtures {
    val root: File by lazy {
        generateSequence(File(System.getProperty("user.dir")!!).absoluteFile) { it.parentFile }
            .map { File(it, "shared-fixtures") }.first { it.isDirectory }
    }
    fun file(path: String): File = File(root, path).also { check(it.exists()) { "Falta ${it.path}" } }
}
