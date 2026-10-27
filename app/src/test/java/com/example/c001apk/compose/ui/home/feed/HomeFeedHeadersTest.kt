package com.example.c001apk.compose.ui.home.feed

import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.logic.state.LoadingState
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class HomeFeedHeadersTest {
    private fun item(id: String, template: String, type: String = "card") = Gson().fromJson(
        """{"id":"$id","entityType":"$type","entityTemplate":"$template"}""",
        HomeFeedResponse.Data::class.java,
    )

    private val rows = listOf(
        item("banner", "imageCarouselCard_1"),
        item("channels", "iconLinkGridCard"),
        item("topics", "iconMiniScrollCard"),
        item("topic-grid", "iconMiniGridCard"),
        item("post", "feed", "feed"),
        item("other-card", "imageSquareScrollCard"),
        item("post-with-card-template", "iconMiniGridCard", "feed"),
    )

    @Test
    fun switchesAreIndependentAndPreservePostsAndOtherCardsInOrder() {
        val source = LoadingState.Success(rows, isPublic = true)
        for (mask in 0..7) {
            val visible = source.withHomeFeedHeaders(
                showCarousel = mask and 1 != 0,
                showChannels = mask and 2 != 0,
                showTopics = mask and 4 != 0,
            ) as LoadingState.Success
            val expectedIds = buildList {
                if (mask and 1 != 0) add("banner")
                if (mask and 2 != 0) add("channels")
                if (mask and 4 != 0) addAll(listOf("topics", "topic-grid"))
                addAll(listOf("post", "other-card", "post-with-card-template"))
            }
            assertEquals(expectedIds, visible.response.map { it.id })
            assertEquals(true, visible.isPublic)
        }
    }

    @Test
    fun hiddenRowsCanReappearFromTheSameLoadedPageWithoutFetching() {
        val source = LoadingState.Success(rows)
        source.withHomeFeedHeaders(false, false, false)
        val restored = source.withHomeFeedHeaders(true, true, true) as LoadingState.Success
        assertEquals(rows, restored.response)
        rows.zip(restored.response).forEach { (original, visible) -> assertSame(original, visible) }
        assertEquals(rows, source.response)
    }

    @Test
    fun loadingEmptyAndErrorStatesAreNotReplacedBySuccessfulLists() {
        listOf(LoadingState.Loading, LoadingState.Empty, LoadingState.Error("offline")).forEach {
            assertSame(it, it.withHomeFeedHeaders(false, false, false))
        }
        assertEquals(
            LoadingState.Success(emptyList<HomeFeedResponse.Data>()),
            LoadingState.Success(rows.take(4)).withHomeFeedHeaders(false, false, false),
        )
    }
}
