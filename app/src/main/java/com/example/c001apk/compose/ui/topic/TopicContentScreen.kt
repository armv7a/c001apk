package com.example.c001apk.compose.ui.topic

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.ui.component.CommonScreen
import com.example.c001apk.compose.util.ReportType
import com.example.c001apk.compose.util.makeToast

/** Created by bggRGjQaUbCoE on 2024/6/9 */
@Composable
fun TopicContentScreen(
    refreshState: Boolean,
    resetRefreshState: () -> Unit,
    entityType: String,
    id: String?,
    url: String,
    title: String,
    productTitle: String,
    productLogo: String?,
    productConfigRows: List<HomeFeedResponse.ProductConfig>,
    productSheetTopPadding: Dp,
    sortType: ProductSortType,
    paddingValues: PaddingValues,
    onViewUser: (String) -> Unit,
    onViewFeed: (String, Boolean) -> Unit,
    onOpenLink: (String, String?) -> Unit,
    onCopyText: (String?) -> Unit,
    onReport: (String, ReportType) -> Unit,
    isScrollingUp: ((Boolean) -> Unit)? = null,
) {

    val viewModel =
        hiltViewModel<TopicContentViewModel, TopicContentViewModel.ViewModelFactory>(
            key = "$entityType:${id.orEmpty()}:$url:$title",
        ) { factory ->
            factory.create(url, title)
        }

    val comparisonSelections by viewModel.productComparisonSelections.collectAsStateWithLifecycle()
    val selectedComparisonIds = comparisonSelections.mapNotNullTo(linkedSetOf()) { it.config.id }

    val sortable = supportsTopicSort(entityType, title, url)
    if (sortable) {
        LaunchedEffect(sortType, url) {
            viewModel.changeSort(sortType, sortedTopicUrl(url, entityType, id, sortType), title)
        }
    }

    key(if (sortable) sortType else null) {
        CommonScreen(
            viewModel = viewModel,
            refreshState = refreshState,
            resetRefreshState = resetRefreshState,
            paddingValues = paddingValues,
            onViewUser = onViewUser,
            onViewFeed = onViewFeed,
            onOpenLink = onOpenLink,
            onCopyText = onCopyText,
            onReport = onReport,
            isScrollingUp = isScrollingUp,
            productConfigRows = productConfigRows,
            selectedProductConfigIds = selectedComparisonIds,
            onViewProductConfig = viewModel::showProductConfig,
            onToggleProductConfigComparison = {
                viewModel.toggleGlobalProductComparison(it, productTitle, productLogo)
            },
            onShowProductConfigComparison = {
                viewModel.showCurrentProductConfigComparison(
                    productConfigRows,
                    productTitle,
                    productLogo,
                )
            },
            onShowGlobalProductComparison = viewModel::showGlobalProductComparison,
            onToggleProductEntityComparison = { viewModel.toggleGlobalProductComparison(it) },
        )
    }

    ProductConfigSheet(
        state = viewModel.productConfigSheetState,
        topPadding = productSheetTopPadding,
        onDismiss = viewModel::dismissProductConfigSheet,
        onRemoveComparison = viewModel::removeGlobalProductComparison,
    )

    val context = LocalContext.current
    viewModel.toastText?.let {
        viewModel.resetToastText()
        context.makeToast(it)
    }
}
