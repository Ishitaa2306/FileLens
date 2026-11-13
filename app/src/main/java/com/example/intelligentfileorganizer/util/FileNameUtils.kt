package com.example.intelligentfileorganizer.util

import java.text.DecimalFormat
import java.util.Locale

object FileNameUtils {

    private val ILLEGAL_CHARS = Regex("[\\\\/:*?\"<>|\\x00-\\x1F]")

    fun sanitizeFileName(name: String, fallback: String = "file"): String {
        val cleaned = name.replace(ILLEGAL_CHARS, "_").trim()
        return if (cleaned.isEmpty() || cleaned == "." || cleaned == "..") {
            fallback
        } else {
            cleaned
        }
    }

    fun extractExtension(fileName: String): String {
        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot > 0 && lastDot < fileName.length - 1) {
            fileName.substring(lastDot + 1).lowercase(Locale.ROOT)
        } else {
            ""
        }
    }

    fun stripExtension(fileName: String): String {
        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot > 0) {
            fileName.substring(0, lastDot)
        } else {
            fileName
        }
    }

    fun ensureExtension(baseName: String, requiredExtension: String): String {
        if (requiredExtension.isBlank()) return baseName
        val normalizedExt = requiredExtension.trimStart('.').lowercase(Locale.ROOT)
        val currentExt = extractExtension(baseName)
        return if (currentExt.equals(normalizedExt, ignoreCase = true)) {
            baseName
        } else {
            "$baseName.$normalizedExt"
        }
    }

    fun generateUniqueName(desiredName: String, existingNames: Set<String>): String {
        if (!existingNames.contains(desiredName)) {
            return desiredName
        }
        val ext = extractExtension(desiredName)
        val base = stripExtension(desiredName)
        var counter = 1
        while (true) {
            val candidate = if (ext.isNotEmpty()) {
                "$base ($counter).$ext"
            } else {
                "$base ($counter)"
            }
            if (!existingNames.contains(candidate)) {
                return candidate
            }
            counter++
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        val df = DecimalFormat("#,##0.#")
        return "${df.format(value)} ${units[digitGroups]}"
    }
}

