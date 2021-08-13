package com.colorlesscat.kamui

import java.io.File

data class ItemFile(
    val file: File,
    val path: String = file.absolutePath,
    var name: String = file.name,
    var isFile: Boolean = file.isFile
)
