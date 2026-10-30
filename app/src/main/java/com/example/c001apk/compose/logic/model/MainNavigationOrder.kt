package com.example.c001apk.compose.logic.model

/** Stable page IDs, independent of their positions in the bottom bar or navigation rail. */
object MainNavigationOrder {
    const val HOME = 0
    const val FOLLOW = 1
    const val PROFILE = 2
    val default: List<Int> = listOf(HOME, FOLLOW, PROFILE)

    fun normalize(stored: List<Int>): List<Int> =
        (stored.filter { it in default } + default).distinct()
}
