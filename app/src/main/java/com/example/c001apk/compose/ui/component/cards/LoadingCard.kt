package com.example.c001apk.compose.ui.component.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.c001apk.compose.R
import com.example.c001apk.compose.logic.state.FooterState
import com.example.c001apk.compose.logic.state.LoadingState
import com.example.c001apk.compose.logic.state.State
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Created by bggRGjQaUbCoE on 2024/6/5 */
enum class Type {
    TEXT,
    INDICATOR,
    NONE,
}

@Composable
fun LoadingCard(
    modifier: Modifier = Modifier,
    state: State,
    onClick: (() -> Unit)? = null,
    isFeed: Boolean = false,
) {

    val type: Pair<Type, String?> =
        when (state) {
            FooterState.End -> Pair(Type.TEXT, stringResource(id = R.string.loading_end))
            is FooterState.Error -> Pair(Type.TEXT, state.errMsg)
            FooterState.Loading -> Pair(Type.INDICATOR, null)
            FooterState.Success -> Pair(Type.NONE, null)
            LoadingState.Empty -> Pair(Type.TEXT, stringResource(id = R.string.loading_empty))
            is LoadingState.Error -> Pair(Type.TEXT, state.errMsg)
            LoadingState.Loading -> Pair(Type.INDICATOR, null)
            is LoadingState.Success<*> -> Pair(Type.NONE, null)
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(if (type.first == Type.NONE) 0.dp else 80.dp)
                .squircleClip(if (isFeed) 0.dp else 16.dp)
                .clickable(
                    enabled = onClick != null,
                    onClick = onClick ?: {},
                ),
    ) {
        when (type.first) {
            Type.TEXT ->
                Text(
                    modifier = Modifier.padding(16.dp).align(Alignment.Center),
                    text =
                        if (type.second == "response body is null")
                            stringResource(
                                if (onClick != null) R.string.loading_error_retry
                                else R.string.loading_error,
                            )
                        else type.second ?: stringResource(id = R.string.loading_empty),
                    textAlign = TextAlign.Center,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )

            Type.INDICATOR ->
                androidx.compose.animation.AnimatedVisibility(
                    visible = type.first == Type.INDICATOR,
                    enter =
                        androidx.compose.animation.fadeIn(
                            animationSpec = androidx.compose.animation.core.tween(400),
                        ),
                    exit =
                        androidx.compose.animation.fadeOut(
                            animationSpec = androidx.compose.animation.core.tween(200),
                        ),
                    modifier = Modifier.align(Alignment.Center),
                ) {
                    CircularProgressIndicator()
                }

            Type.NONE -> {}
        }
    }
}
