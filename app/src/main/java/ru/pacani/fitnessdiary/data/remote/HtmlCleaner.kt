package ru.pacani.fitnessdiary.data.remote

/**
 * Превращает HTML-описание из wger в обычный текст.
 *
 * Абзацы и пункты списка становятся отдельными строками, пункты начинаются с «• ».
 * Написано без android.text.Html, чтобы работало в обычных юнит-тестах.
 */
object HtmlCleaner {

    private val lineBreak = Regex("(?i)<br\\s*/?>")
    private val blockTag = Regex("(?i)</?(p|div|ul|ol|h[1-6]|blockquote)(\\s[^>]*)?>")
    private val listItemStart = Regex("(?i)<li(\\s[^>]*)?>")
    private val listItemEnd = Regex("(?i)</li>")
    private val anyTag = Regex("<[^>]+>")
    private val numericEntity = Regex("&#(\\d+);")
    private val hexEntity = Regex("(?i)&#x([0-9a-f]+);")
    private val spaces = Regex("[ \\t\\u00A0]+")

    fun clean(html: String?): String {
        if (html.isNullOrBlank()) return ""
        val text = html
            .replace(lineBreak, "\n")
            .replace(listItemStart, "\n$BULLET")
            .replace(listItemEnd, "\n")
            .replace(blockTag, "\n")
            .replace(anyTag, "")
            .let(::decodeEntities)

        return text.lines()
            .map { it.replace(spaces, " ").trim() }
            .filter { it.isNotEmpty() && it != BULLET.trim() }
            .joinToString("\n")
    }

    private fun decodeEntities(text: String): String = text
        .replace(numericEntity) { it.groupValues[1].toIntOrNull()?.let(::codePointToString) ?: it.value }
        .replace(hexEntity) { it.groupValues[1].toIntOrNull(16)?.let(::codePointToString) ?: it.value }
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&#39;", "'")
        // &amp; последним, чтобы «&amp;lt;» не превратился в «<»
        .replace("&amp;", "&")

    private fun codePointToString(codePoint: Int): String = String(Character.toChars(codePoint))

    const val BULLET = "• "
}
