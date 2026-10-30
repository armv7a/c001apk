package com.example.c001apk.compose.ui.component.settings

/**
 * Resolve one pointer event in its travel direction only. Testing both directions after a swap
 * can oscillate forever at the midpoint (and for differently sized rows). Bounds keep the
 * dragged row inside its card, including fast drags crossing multiple neighbours.
 */
internal fun dragReorderTarget(
    from: Int,
    heights: List<Int>,
    currentOffset: Float,
    deltaY: Float,
): Pair<Int, Float> {
    if (from !in heights.indices || heights.any { it <= 0 }) return from to currentOffset
    val before = heights.take(from).sum().toFloat()
    val after = heights.drop(from + 1).sum().toFloat()
    var offset = (currentOffset + deltaY).coerceIn(-before, after)
    var target = from
    if (deltaY > 0f) {
        while (target < heights.lastIndex && offset > heights[target + 1] / 2f) {
            offset -= heights[target + 1]
            target++
        }
    } else if (deltaY < 0f) {
        while (target > 0 && -offset > heights[target - 1] / 2f) {
            offset += heights[target - 1]
            target--
        }
    }
    return target to offset
}
