package com.example.c001apk.compose.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.c001apk.compose.R
import com.example.c001apk.compose.ui.component.BackButton
import com.example.c001apk.compose.ui.component.MiuixActionButton as TextButton
import com.example.c001apk.compose.ui.component.MiuixAlertDialog as AlertDialog
import com.example.c001apk.compose.ui.component.MiuixScaffold as Scaffold
import com.example.c001apk.compose.ui.component.cards.SearchHistoryCard
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Created by bggRGjQaUbCoE on 2024/6/6 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    title: String?,
    onSearch: (String) -> Unit,
) {

    val viewModel = hiltViewModel<SearchViewModel>(key = title)
    val searchHistory by
        viewModel.searchHistory.collectAsStateWithLifecycle(initialValue = emptyList())

    var textInput by
        rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue())
        }

    val focusRequest = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        try {
            focusRequest.requestFocus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    val textStyle = MiuixTheme.textStyles.main
    var showClearDialog by remember { mutableStateOf(false) }
    val layoutDirection = LocalLayoutDirection.current

    fun onCheckSearch(keyword: String = textInput.text) {
        with(keyword) {
            if (this.isNotEmpty()) {
                viewModel.saveHistory(this)
                onSearch(this)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .windowInsetsPadding(
                            WindowInsets.statusBars.union(WindowInsets.displayCutout),
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BackButton(onBackClick)
                InputField(
                    modifier = Modifier.weight(1f).focusRequester(focusRequest),
                    query = textInput.text,
                    onQueryChange = {
                        textInput = TextFieldValue(it, selection = TextRange(it.length))
                    },
                    onSearch = { onCheckSearch() },
                    expanded = true,
                    onExpandedChange = {},
                    label =
                        if (!title.isNullOrEmpty()) stringResource(R.string.search_hint_in, title)
                        else stringResource(R.string.search_hint),
                )
                IconButton(onClick = { onCheckSearch() }) {
                    Icon(MiuixIcons.Search, contentDescription = "搜索")
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier.padding(
                    top = paddingValues.calculateTopPadding(),
                ),
        ) {
            HorizontalDivider()

            androidx.compose.animation.AnimatedVisibility(
                modifier =
                    Modifier.padding(
                        start =
                            paddingValues.calculateLeftPadding(
                                layoutDirection,
                            ),
                    ),
                visible = searchHistory.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "搜索历史",
                        modifier = Modifier.weight(1f).padding(start = 10.dp),
                        style =
                            MiuixTheme.textStyles.headline2.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                    )

                    IconButton(
                        onClick = {
                            showClearDialog = true
                        },
                    ) {
                        Icon(imageVector = MiuixIcons.Delete, contentDescription = null)
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier =
                    Modifier.padding(start = paddingValues.calculateLeftPadding(layoutDirection))
                        .padding(horizontal = 10.dp),
            ) {
                searchHistory.forEach {
                    SearchHistoryCard(
                        data = it.data,
                        onSearch = {
                            onCheckSearch(it.data)
                        },
                        onDelete = {
                            viewModel.delete(it.data)
                        },
                    )
                }
            }
        }
    }

    when {
        showClearDialog -> {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showClearDialog = false
                            viewModel.clearAll()
                        },
                    ) {
                        Text(text = stringResource(id = android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showClearDialog = false
                        },
                    ) {
                        Text(text = stringResource(id = android.R.string.cancel))
                    }
                },
                title = {
                    Text(
                        text = stringResource(id = R.string.search_clear_history_confirm),
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
            )
        }
    }
}
