package com.racas.black.controlcenter

import java.util.Locale

/**
 * A deliberately small TOML reader for the three files the native host writes. toml++ emits flat tables, scalars and
 * [tables], never inline arrays or multi-line strings, so this covers the files exactly while keeping the preview APK
 * free of a TOML dependency.
 */
internal sealed interface TomlValue {
    data class Str(val value: String) : TomlValue
    data class Bool(val value: Boolean) : TomlValue
    data class IntValue(val value: Long) : TomlValue
    data class DoubleValue(val value: Double) : TomlValue
}

internal class TomlDocument {
    /** table name to ordered key/value pairs; "" is the top-level table. */
    val tables: LinkedHashMap<String, LinkedHashMap<String, TomlValue>> = linkedMapOf()

    fun table(name: String): Map<String, TomlValue> = tables[name].orEmpty()

    fun string(table: String, key: String): String? =
        (tables[table]?.get(key) as? TomlValue.Str)?.value

    fun bool(table: String, key: String): Boolean? =
        (tables[table]?.get(key) as? TomlValue.Bool)?.value

    fun long(table: String, key: String): Long? = when (val v = tables[table]?.get(key)) {
        is TomlValue.IntValue -> v.value
        is TomlValue.DoubleValue -> v.value.toLong()
        else -> null
    }

    fun double(table: String, key: String): Double? = when (val v = tables[table]?.get(key)) {
        is TomlValue.IntValue -> v.value.toDouble()
        is TomlValue.DoubleValue -> v.value
        else -> null
    }

    companion object {
        fun parse(text: String): TomlDocument {
            val doc = TomlDocument()
            var current = ""
            doc.tables[current] = linkedMapOf()
            for (rawLine in text.lineSequence()) {
                val line = stripComment(rawLine).trim()
                if (line.isEmpty()) continue
                if (line.startsWith("[") && line.endsWith("]")) {
                    current = line.substring(1, line.length - 1).trim().trim('"', '\'')
                    doc.tables.getOrPut(current) { linkedMapOf() }
                    continue
                }
                val eq = findEquals(line)
                if (eq < 0) continue
                val key = line.substring(0, eq).trim().trim('"', '\'')
                val raw = line.substring(eq + 1).trim()
                val value = parseValue(raw) ?: continue
                doc.tables.getOrPut(current) { linkedMapOf() }[key] = value
            }
            return doc
        }

        private fun stripComment(line: String): String {
            var quoted = false
            var escaped = false
            for (i in line.indices) {
                val c = line[i]
                if (escaped) {
                    escaped = false
                    continue
                }
                if (c == '\\' && quoted) {
                    escaped = true
                    continue
                }
                if (c == '"') quoted = !quoted
                if (c == '#' && !quoted) return line.substring(0, i)
            }
            return line
        }

        private fun findEquals(line: String): Int {
            var quoted = false
            for (i in line.indices) {
                if (line[i] == '"' && (i == 0 || line[i - 1] != '\\')) quoted = !quoted
                if (line[i] == '=' && !quoted) return i
            }
            return -1
        }

        private fun parseValue(raw: String): TomlValue? {
            if (raw.length >= 2 && raw.startsWith("\"") && raw.endsWith("\"")) {
                return TomlValue.Str(unescape(raw.substring(1, raw.length - 1)))
            }
            if (raw.length >= 2 && raw.startsWith("'") && raw.endsWith("'")) {
                return TomlValue.Str(raw.substring(1, raw.length - 1))
            }
            return when (raw.lowercase(Locale.ROOT)) {
                "true" -> TomlValue.Bool(true)
                "false" -> TomlValue.Bool(false)
                else -> {
                    val clean = raw.replace("_", "")
                    clean.toLongOrNull()?.let { return TomlValue.IntValue(it) }
                    clean.toDoubleOrNull()?.let { return TomlValue.DoubleValue(it) }
                    null
                }
            }
        }

        private fun unescape(s: String): String {
            val out = StringBuilder(s.length)
            var i = 0
            while (i < s.length) {
                val c = s[i]
                if (c != '\\' || i + 1 >= s.length) {
                    out.append(c)
                    i++
                    continue
                }
                when (val n = s[i + 1]) {
                    'n' -> out.append('\n')
                    'r' -> out.append('\r')
                    't' -> out.append('\t')
                    '"' -> out.append('"')
                    '\\' -> out.append('\\')
                    'u' -> {
                        if (i + 5 < s.length) {
                            val hex = s.substring(i + 2, i + 6).toIntOrNull(16)
                            if (hex != null) {
                                out.append(hex.toChar())
                                i += 6
                                continue
                            }
                        }
                        out.append(n)
                    }
                    else -> out.append(n)
                }
                i += 2
            }
            return out.toString()
        }
    }
}

internal object TomlWriter {
    fun scalar(value: String): String = "\"" + value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t") + "\""

    fun scalar(value: Boolean): String = if (value) "true" else "false"
    fun scalar(value: Int): String = value.toString()
    fun scalar(value: Long): String = value.toString()
    fun scalar(value: Float): String = scalar(value.toDouble())
    fun scalar(value: Double): String = String.format(Locale.US, "%.4f", value)

    fun key(value: String): String =
        if (value.matches(Regex("[A-Za-z0-9_-]+"))) value else scalar(value)

    class Builder {
        private val out = StringBuilder()
        private var current = ""

        fun topLevel(key: String, value: String): Builder = line(key, value)
        fun topLevel(key: String, value: Boolean): Builder = line(key, scalar(value))
        fun topLevel(key: String, value: Int): Builder = line(key, scalar(value))
        fun topLevel(key: String, value: Long): Builder = line(key, scalar(value))
        fun topLevel(key: String, value: Float): Builder = line(key, scalar(value))
        fun topLevel(key: String, value: Double): Builder = line(key, scalar(value))

        /** Writes a value already serialized by [TomlWriter.scalar]; used to preserve unknown keys. */
        fun rawTopLevel(key: String, value: String): Builder = line(key, value)

        fun table(name: String): Builder {
            if (current != name) {
                if (out.isNotEmpty()) out.append('\n')
                out.append('[').append(name).append("]\n")
                current = name
            }
            return this
        }

        fun value(key: String, value: String): Builder = line(key, scalar(value))
        fun value(key: String, value: Boolean): Builder = line(key, scalar(value))
        fun value(key: String, value: Int): Builder = line(key, scalar(value))
        fun value(key: String, value: Long): Builder = line(key, scalar(value))
        fun value(key: String, value: Float): Builder = line(key, scalar(value))
        fun value(key: String, value: Double): Builder = line(key, scalar(value))

        private fun line(key: String, value: String): Builder {
            out.append(TomlWriter.key(key)).append(" = ").append(value).append('\n')
            return this
        }

        override fun toString(): String = out.toString()
    }
}
