package com.example.c001apk.compose.util

import org.junit.Assert.*
import org.junit.Test

class EmojiUtilsTest {
    @Test fun pickerCategoriesExposeEveryNiuniuEmojiWithoutLosingDefaultOrCoinEntries() {
        val categories = EmojiUtils.pickerCategories
        assertEquals(listOf("默认", "牛牛", "酷币"), categories.map { it.title })
        val niuniu = categories[1].pages
        assertEquals(listOf(27, 5), niuniu.map { it.size })
        assertTrue(niuniu.flatten().all { it.first.startsWith("[牛牛") })
        assertEquals("[牛牛爆赞]", niuniu.last().last().first)
        val all = categories.flatMap { it.pages.flatten() }
        assertEquals(EmojiUtils.dataList.drop(4).toSet(), all.toSet())
        assertEquals(all.size, all.distinctBy { it.first }.size)
        assertTrue(categories.all { category -> category.pages.all { it.size in 1..27 } })
        all.forEach { assertEquals(it.second, EmojiUtils.emojiMap[it.first]) }
    }
}
