package com.example.c001apk.compose.ui.home

import com.example.c001apk.compose.logic.model.HomeMenu

/**
 * In-memory normalization of the persisted home tabs.
 *
 * Room rows can be partial or stale: a row may carry a tab that no longer exists (or FOLLOW,
 * which is never a home tab), a tab added later has no row yet, and positions can point at an
 * old order. The stored order and enabled flags of known tabs are kept as they are, missing tabs
 * are appended enabled, and positions are rewritten contiguously. Nothing is written back here;
 * the editor persists the order it actually shows.
 */
internal fun normalizeHomeMenus(
    storedMenus: List<HomeMenu>,
    types: List<TabType> = homeTabTypes,
): List<HomeMenu> = normalizeMenus(storedMenus.sortedBy(HomeMenu::position), types)

/**
 * The list the editor persists: the draft's own order wins (its `position` values are stale after
 * a drag), known tabs only, at least one of them enabled.
 */
internal fun sanitizeHomeMenus(
    draft: List<HomeMenu>,
    types: List<TabType> = homeTabTypes,
): List<HomeMenu> {
    val normalized = normalizeMenus(draft, types)
    if (normalized.isEmpty() || normalized.any(HomeMenu::isEnable)) return normalized
    // Turning every tab off would leave the home pager without content.
    val fallback = normalized.first().title
    return normalized.map { menu -> if (menu.title == fallback) menu.copy(isEnable = true) else menu }
}

private fun normalizeMenus(
    source: List<HomeMenu>,
    types: List<TabType>,
): List<HomeMenu> {
    if (types.isEmpty()) return emptyList()
    val knownNames = types.mapTo(HashSet()) { it.name }
    val seen = HashSet<String>(source.size)
    val ordered = ArrayList<HomeMenu>(types.size)

    source.forEach { menu ->
        if (menu.title in knownNames && seen.add(menu.title)) ordered += menu
    }
    types.forEach { type ->
        if (seen.add(type.name)) {
            ordered += HomeMenu(position = 0, title = type.name, isEnable = true)
        }
    }
    return ordered.mapIndexed { index, menu -> menu.copy(position = index) }
}
