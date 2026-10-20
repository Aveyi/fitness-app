package ru.pacani.fitnessdiary.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class HtmlCleanerTest {

    @Test
    fun `абзацы становятся отдельными строками`() {
        val html = "<p>Лягте на скамью.</p><p>Возьмите гриф.</p>"
        assertEquals("Лягте на скамью.\nВозьмите гриф.", HtmlCleaner.clean(html))
    }

    @Test
    fun `пункты списка получают маркер`() {
        val html = "<ol><li>Опустите штангу</li><li>Выжмите вверх</li></ol>"
        assertEquals("• Опустите штангу\n• Выжмите вверх", HtmlCleaner.clean(html))
    }

    @Test
    fun `теги внутри текста удаляются, переносы сохраняются`() {
        val html = "Держите <strong>спину</strong> ровно<br/>Не <em>спешите</em>"
        assertEquals("Держите спину ровно\nНе спешите", HtmlCleaner.clean(html))
    }

    @Test
    fun `html-сущности раскодируются`() {
        val html = "<p>Вес &gt; 50&nbsp;кг &amp; 8&#8211;10 повторений, &quot;медленно&quot;</p>"
        assertEquals("Вес > 50 кг & 8–10 повторений, \"медленно\"", HtmlCleaner.clean(html))
    }

    @Test
    fun `двойное экранирование не раскодируется дважды`() {
        assertEquals("&lt;", HtmlCleaner.clean("&amp;lt;"))
    }

    @Test
    fun `пустой и null дают пустую строку`() {
        assertEquals("", HtmlCleaner.clean(null))
        assertEquals("", HtmlCleaner.clean("   "))
        assertEquals("", HtmlCleaner.clean("<p></p><ul><li></li></ul>"))
    }

    @Test
    fun `лишние пробелы схлопываются`() {
        assertEquals("Медленно опустите", HtmlCleaner.clean("<p>  Медленно    опустите  </p>"))
    }
}
