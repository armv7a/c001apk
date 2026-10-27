package com.example.c001apk.compose.ui.home.topic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.c001apk.compose.logic.model.ProductCategory
import com.example.c001apk.compose.logic.model.TopicBean
import com.example.c001apk.compose.ui.carousel.CarouselContentScreen
import com.example.c001apk.compose.ui.component.LocalTabContentPadding
import com.example.c001apk.compose.ui.component.TopBarScrollEffect
import com.example.c001apk.compose.ui.theme.cardBg
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ProductCategoryScreen(
    categories: List<ProductCategory>,
    paddingValues: PaddingValues,
    onViewUser: (String) -> Unit,
    onViewFeed: (String, Boolean) -> Unit,
    onOpenLink: (String, String?) -> Unit,
    onCopyText: (String?) -> Unit,
) {
    ProductCategoryLayout(categories, paddingValues) { page ->
        CarouselContentScreen(
            url = page.url,
            title = page.title,
            paddingValues = paddingValues,
            refreshState = null,
            resetRefreshState = {},
            onViewUser = onViewUser,
            onViewFeed = onViewFeed,
            onOpenLink = onOpenLink,
            onCopyText = onCopyText,
            isHomeFeed = true,
        )
    }
}

@Composable
internal fun ProductCategoryLayout(
    categories: List<ProductCategory>,
    paddingValues: PaddingValues = PaddingValues(),
    content: @Composable (TopicBean) -> Unit,
) {
    var parentUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var childUrl by rememberSaveable { mutableStateOf<String?>(null) }
    val parent = categories.firstOrNull { it.page.url == parentUrl } ?: categories.firstOrNull()
        ?: return
    val target = parent.children.firstOrNull { it.url == childUrl } ?: parent.page
    val listState = rememberLazyListState()
    val contentState = rememberSaveableStateHolder()
    TopBarScrollEffect { listState.animateScrollToItem(0) }

    Row(Modifier.fillMaxSize().background(cardBg())) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxHeight().weight(0.22f).selectableGroup(),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() +
                    LocalTabContentPadding.current.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding(),
            ),
        ) {
            categories.forEach { category ->
                val expanded = category.page.url == parent.page.url
                item(key = category.page.url) {
                    CategoryRow(category.page.title, target.url == category.page.url, expanded) {
                        parentUrl = category.page.url
                        childUrl = null
                    }
                }
                if (expanded) {
                    category.children.forEach { child ->
                        item(key = category.page.url + ":" + child.url) {
                            CategoryRow(child.title, target.url == child.url, false, true) {
                                childUrl = child.url
                            }
                        }
                    }
                }
            }
        }
        Box(Modifier.fillMaxHeight().weight(0.78f)) {
            contentState.SaveableStateProvider(target.url) { content(target) }
        }
    }
}

@Composable
private fun CategoryRow(
    title: String,
    selected: Boolean,
    expanded: Boolean,
    child: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min)
            .background(if (expanded || child) cardBg() else MiuixTheme.colorScheme.surface)
            .selectable(selected, role = Role.Tab, onClick = onClick)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight().then(
            if (selected) Modifier.background(MiuixTheme.colorScheme.primary) else Modifier,
        ))
        Text(
            title,
            modifier = Modifier.weight(1f).padding(horizontal = 6.dp, vertical = 8.dp),
            textAlign = TextAlign.Center,
            fontSize = if (child) 12.sp else 14.sp,
            fontWeight = if (expanded || selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MiuixTheme.colorScheme.primary
                else if (child) MiuixTheme.colorScheme.onSurfaceVariantSummary
                else MiuixTheme.colorScheme.onSurface,
        )
    }
}
