package com.example.c001apk.compose.ui.home.feed

import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.logic.state.LoadingState

internal enum class HomeFeedHeader { Carousel, Channels, Topics }

internal fun HomeFeedResponse.Data.homeFeedHeader(): HomeFeedHeader? =
    if (entityType != "card") null
    else when (entityTemplate) {
        "imageCarouselCard_1" -> HomeFeedHeader.Carousel
        "iconLinkGridCard" -> HomeFeedHeader.Channels
        "iconMiniScrollCard", "iconMiniGridCard" -> HomeFeedHeader.Topics
        else -> null
    }

/** Keep the fetched list intact so hidden sections can return without a refresh. */
internal fun LoadingState<List<HomeFeedResponse.Data>>.withHomeFeedHeaders(
    showCarousel: Boolean,
    showChannels: Boolean,
    showTopics: Boolean,
): LoadingState<List<HomeFeedResponse.Data>> =
    if (this !is LoadingState.Success) this
    else copy(response = response.filter {
        when (it.homeFeedHeader()) {
            HomeFeedHeader.Carousel -> showCarousel
            HomeFeedHeader.Channels -> showChannels
            HomeFeedHeader.Topics -> showTopics
            null -> true
        }
    })
