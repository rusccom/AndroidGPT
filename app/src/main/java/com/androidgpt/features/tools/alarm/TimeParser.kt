package com.androidgpt.features.tools.alarm

import java.util.Calendar

/**
 * Парсит русские формулировки времени в epoch-ms триггера.
 * Поддерживает: "через N (сек/мин/час)", "через полчаса", "через час",
 * "в HH[:MM]", "в HH утра|вечера", "сегодня/завтра/послезавтра в HH",
 * "в <день недели> [в HH]".
 */
object TimeParser {

    private val NUMBERS = mapOf(
        "один" to 1, "одну" to 1, "одна" to 1,
        "два" to 2, "две" to 2,
        "три" to 3, "четыре" to 4, "пять" to 5,
        "шесть" to 6, "семь" to 7, "восемь" to 8,
        "девять" to 9, "десять" to 10,
        "пятнадцать" to 15, "двадцать" to 20, "тридцать" to 30,
    )

    private val WEEKDAYS = mapOf(
        "понедельник" to Calendar.MONDAY, "понедельника" to Calendar.MONDAY,
        "вторник" to Calendar.TUESDAY, "вторника" to Calendar.TUESDAY,
        "среду" to Calendar.WEDNESDAY, "среда" to Calendar.WEDNESDAY, "среды" to Calendar.WEDNESDAY,
        "четверг" to Calendar.THURSDAY, "четверга" to Calendar.THURSDAY,
        "пятницу" to Calendar.FRIDAY, "пятница" to Calendar.FRIDAY, "пятницы" to Calendar.FRIDAY,
        "субботу" to Calendar.SATURDAY, "суббота" to Calendar.SATURDAY,
        "воскресенье" to Calendar.SUNDAY, "воскресения" to Calendar.SUNDAY,
    )

    fun parse(text: String, now: Long = System.currentTimeMillis()): Long? {
        val t = text.lowercase().trim()
        relativeOffsetMs(t)?.let { return now + it }
        absoluteTime(t, now)?.let { return it }
        weekdayTime(t, now)?.let { return it }
        dayWordTime(t, now)?.let { return it }
        return null
    }

    private fun relativeOffsetMs(t: String): Long? {
        if ("полчаса" in t) return 30 * 60_000L
        if (Regex("через\\s+час\\b").containsMatchIn(t)) return 60 * 60_000L
        val m = Regex("через\\s+([\\p{L}\\d]+)\\s*([\\p{L}]+)?").find(t) ?: return null
        val n = m.groupValues[1].toIntOrNull() ?: NUMBERS[m.groupValues[1]] ?: return null
        val unit = m.groupValues.getOrNull(2).orEmpty()
        return when {
            unit.startsWith("сек") || unit == "с" -> n * 1000L
            unit.startsWith("мин") || unit == "м" -> n * 60_000L
            unit.startsWith("час") || unit == "ч" -> n * 3600_000L
            unit.isBlank() -> n * 60_000L
            else -> null
        }
    }

    private fun absoluteTime(t: String, now: Long): Long? {
        val m = Regex("в\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(утра|вечера|дня|ночи)?").find(t) ?: return null
        var h = m.groupValues[1].toIntOrNull() ?: return null
        val min = m.groupValues[2].toIntOrNull() ?: 0
        val ampm = m.groupValues[3]
        h = adjustAmPm(h, ampm)
        if (h !in 0..23 || min !in 0..59) return null
        return nextOccurrence(h, min, now, dayOffset = 0)
    }

    private fun weekdayTime(t: String, now: Long): Long? {
        val day = WEEKDAYS.entries.firstOrNull { (k, _) -> Regex("\\b$k\\b").containsMatchIn(t) }
            ?: return null
        val tm = Regex("в\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(утра|вечера|дня|ночи)?")
            .findAll(t).lastOrNull()
        var h = tm?.groupValues?.get(1)?.toIntOrNull() ?: 9
        val min = tm?.groupValues?.get(2)?.toIntOrNull() ?: 0
        h = adjustAmPm(h, tm?.groupValues?.get(3).orEmpty())
        return nextWeekdayOccurrence(day.value, h, min, now)
    }

    private fun dayWordTime(t: String, now: Long): Long? {
        val offset = when {
            "послезавтра" in t -> 2
            "завтра" in t -> 1
            "сегодня" in t -> 0
            else -> return null
        }
        val m = Regex("в\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(утра|вечера|дня|ночи)?").find(t)
        var h = m?.groupValues?.get(1)?.toIntOrNull() ?: 9
        val min = m?.groupValues?.get(2)?.toIntOrNull() ?: 0
        h = adjustAmPm(h, m?.groupValues?.get(3).orEmpty())
        return nextOccurrence(h, min, now, dayOffset = offset, forceFutureWhenSameDay = offset == 0)
    }

    private fun adjustAmPm(h: Int, ampm: String): Int = when (ampm) {
        "вечера", "дня" -> if (h in 1..11) h + 12 else h
        "ночи" -> if (h == 12) 0 else h
        "утра" -> if (h == 12) 0 else h
        else -> h
    }

    private fun nextOccurrence(
        h: Int, m: Int, now: Long, dayOffset: Int, forceFutureWhenSameDay: Boolean = false,
    ): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.DATE, dayOffset)
            if (forceFutureWhenSameDay && timeInMillis <= now) add(Calendar.DATE, 1)
        }
        return cal.timeInMillis
    }

    private fun nextWeekdayOccurrence(targetDow: Int, h: Int, m: Int, now: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        var diff = (targetDow - cal.get(Calendar.DAY_OF_WEEK) + 7) % 7
        if (diff == 0 && cal.timeInMillis <= now) diff = 7
        cal.add(Calendar.DATE, diff)
        return cal.timeInMillis
    }
}
