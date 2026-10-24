package com.example.c001apk.compose.ui.topic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.c001apk.compose.ui.component.rememberHapticClick
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.SinkFeedback

internal fun supportsTopicSort(entityType: String, title: String, url: String): Boolean =
    (entityType == "product" && title == "讨论") ||
        (entityType == "topic" &&
            (url.contains("feedList", ignoreCase = true) || title in listOf("讨论", "全部", "广场")))

/** Keep the server's default URL intact, including the topic and active section filters. */
internal fun sortedTopicUrl(
    original: String,
    entityType: String,
    id: String?,
    sort: ProductSortType,
): String {
    if (sort == ProductSortType.REPLY) return original
    if (entityType == "product") {
        val query =
            if (sort == ProductSortType.HOT) "listType=rank_score"
            else "ignoreEntityById=1&listType=dateline_desc"
        return "/page?url=/product/feedList?type=feed&id=$id&$query"
    }
    val listType = if (sort == ProductSortType.HOT) "popular" else "dateline_desc"
    val parameter = Regex("([?&])listType=[^&#]*")
    return if (parameter.containsMatchIn(original)) {
        parameter.replace(original) { "${it.groupValues[1]}listType=$listType" }
    } else original + (if ('?' in original) "&" else "?") + "listType=$listType"
}

@Composable
internal fun TopicSortOptions(selected: ProductSortType, onSelect: (ProductSortType) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp).selectableGroup(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
                ProductSortType.REPLY to "默认",
                ProductSortType.DATELINE to "最新",
                ProductSortType.HOT to "热度",
            )
            .forEach { (sort, label) ->
                val active = selected == sort
                Box(
                    Modifier.selectable(
                            selected = active,
                            role = Role.RadioButton,
                            interactionSource = null,
                            indication = SinkFeedback(),
                            onClick = rememberHapticClick { if (!active) onSelect(sort) },
                        )
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        color =
                            if (active) MiuixTheme.colorScheme.primary
                            else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                    )
                }
            }
    }
}
