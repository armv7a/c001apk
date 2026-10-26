package com.example.c001apk.compose.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.example.c001apk.compose.logic.providable.LocalUserPreferences
import com.example.c001apk.compose.ui.component.ReadingLayout
import com.example.c001apk.compose.ui.component.WindowWidthSizeClass
import com.example.c001apk.compose.ui.component.currentWindowSize
import com.example.c001apk.compose.ui.feed.FeedScreen
import com.example.c001apk.compose.util.ReportType
import com.example.c001apk.compose.util.getReportUrl

@Composable
fun MainNavigation(
    navigator: AppNavigator,
    badge: Int,
    resetBadge: () -> Unit,
    widthSizeClass: WindowWidthSizeClass,
) {
    val context = LocalContext.current
    var selectedMainPage by rememberSaveable { mutableIntStateOf(0) }
    val preferences = LocalUserPreferences.current
    val accountRevision = preferences.accountRevision
    var displayedAccountRevision by rememberSaveable { mutableStateOf(accountRevision) }
    val detailNavigator = rememberAppNavigator(AppDestination.DetailPlaceholder)
    var detailParent by rememberSaveable { mutableStateOf<String?>(null) }
    var detailExiting by remember { mutableStateOf(false) }

    LaunchedEffect(accountRevision) {
        if (displayedAccountRevision != accountRevision) {
            displayedAccountRevision = accountRevision
            selectedMainPage = 2
            detailParent = null
            detailExiting = false
            detailNavigator.reset(AppDestination.DetailPlaceholder)
            navigator.reset()
        }
    }
    val windowSize = currentWindowSize()
    val density = LocalDensity.current
    val canSplit = ReadingLayout.split(
        windowSize.width.value,
        minOf(windowSize.width.value, windowSize.height.value),
        density.fontScale,
        preferences.readingMode,
    )
    val paneAttached = detailParent == navigator.currentEntry.id
    val paneVisible = paneAttached && detailNavigator.canGoBack
    val split = paneVisible && canSplit
    val detailWidth = if (canSplit) (windowSize.width * .52f).coerceIn(390.dp, 760.dp) else windowSize.width

    fun onReport(id: String, type: ReportType) {
        navigator.navigateToWebView(getReportUrl(id, type))
    }

    fun onViewFeed(id: String, isViewReply: Boolean) {
        if (canSplit || paneVisible) {
            detailParent = navigator.currentEntry.id
            detailExiting = false
            val destination = AppDestination.Feed(id, isViewReply)
            if (detailNavigator.canGoBack) detailNavigator.replace(destination)
            else detailNavigator.navigate(destination)
        } else {
            navigator.navigateToFeed(id, isViewReply)
        }
    }

    fun onOpenLink(url: String, title: String?) {
        navigator.onOpenLink(context, url, title, onViewFeed = ::onViewFeed)
    }

    fun closeDetail() {
        if (detailNavigator.canGoBack) {
            detailExiting = true
            detailNavigator.popBackStack()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AppNavHost(
            navigator = navigator,
            pane = split,
            active = !paneVisible || split,
            gesturesEnabled = !paneVisible,
            modifier = (if (split) Modifier.width(windowSize.width - detailWidth) else Modifier.fillMaxWidth())
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
                .then(
                    if (paneVisible && !split) Modifier.alpha(0f).clearAndSetSemantics {}
                    else Modifier,
                ),
        ) { entry ->
            AppDestinationContent(
                route = entry.destination,
                navigator = navigator,
                selectedMainPage = selectedMainPage,
                onMainPageSelected = { selectedMainPage = it },
                badge = badge,
                resetBadge = resetBadge,
                widthSizeClass = widthSizeClass,
                onViewFeed = ::onViewFeed,
                onOpenLink = ::onOpenLink,
                onReport = ::onReport,
            )
        }
        // Keep the native host alive across parent navigation and posture changes. Hidden panes
        // retain their state, but have no hit area and cannot consume back or resume their pages.
        AppNavHost(
            navigator = detailNavigator,
            pane = canSplit,
            transparentRoot = true,
            active = paneAttached && (paneVisible || detailExiting),
            modifier = Modifier.width(if (paneAttached && (paneVisible || detailExiting)) detailWidth else 0.dp)
                .fillMaxHeight().align(Alignment.CenterStart),
            onBack = ::closeDetail,
        ) { entry ->
            val feed = entry.destination as? AppDestination.Feed
            if (feed != null) {
                DisposableEffect(entry.id) {
                    onDispose {
                        if (!detailNavigator.canGoBack) {
                            detailExiting = false
                            detailParent = null
                        }
                    }
                }
                FeedScreen(
                    isCompat = false,
                    onBackClick = ::closeDetail,
                    id = feed.id,
                    isViewReply = feed.isViewReply,
                    onViewUser = navigator::navigateToUser,
                    onViewFeed = ::onViewFeed,
                    onOpenLink = ::onOpenLink,
                    onCopyText = navigator::navigateToCopyText,
                    onReport = ::onReport,
                )
            }
        }
    }
}
