package com.pmj.util

import io.ktor.http.content.*
import io.ktor.utils.io.jvm.javaio.*
import java.io.File
import java.util.*

fun PartData.FileItem.save(path: String): String {
    val fileBytes = provider().toInputStream().readBytes()
    val fileExtension = originalFileName?.takeLastWhile { it != '.' }
    val fileName = UUID.randomUUID().toString() + "." + fileExtension
    val folder = File(path)
    if (!folder.parentFile.exists()) {
        folder.parentFile.mkdirs()
    }
    folder.mkdir()
    File("$path$fileName").writeBytes(fileBytes)
    return fileName
}

