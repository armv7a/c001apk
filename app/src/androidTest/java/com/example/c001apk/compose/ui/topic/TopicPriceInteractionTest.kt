package com.example.c001apk.compose.ui.topic

import android.os.ParcelFileDescriptor
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.logic.model.feedPriceDisplay
import com.example.c001apk.compose.logic.model.purchasePriceSummary
import com.example.c001apk.compose.logic.model.tradeLink
import com.example.c001apk.compose.ui.component.cards.FeedPriceRow
import com.example.c001apk.compose.ui.component.cards.PurchasePriceSummaryCard
import com.example.c001apk.compose.ui.component.cards.FeedProductInfo
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import top.yukonga.miuix.kmp.theme.MiuixTheme

class TopicPriceInteractionTest {
    private val compose = createComposeRule()
    @get:Rule val rule: RuleChain = RuleChain.outerRule(object : ExternalResource() {
        override fun before() {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            instrumentation.uiAutomation.executeShellCommand(
                "am start -W -n ${instrumentation.targetContext.packageName}/androidx.activity.ComponentActivity",
            ).use { ParcelFileDescriptor.AutoCloseInputStream(it).use { stream -> stream.readBytes() } }
        }
    }).around(compose)
    private fun item(json: String) = Gson().fromJson(json, HomeFeedResponse.Data::class.java)

    @Test fun pricesAndConfigurationStayVisibleWithLargerTextAndNarrowWidth() {
        val arrival=item("""{"tsubid":6,"tsubdata":"{\"final_price\":2648,\"config_name\":\"12GB+256GB\"}"}""").feedPriceDisplay()!!
        val trade=item("""{"feedType":"ershou","ershou_info":{"product_price":3900,"product_config_data":"{\"configTitle\":\"16GB+512GB\"}"}}""").feedPriceDisplay()!!
        compose.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,1.3f)) {
                MiuixTheme {
                    Column(Modifier.width(280.dp).padding(12.dp)) {
                        FeedPriceRow(arrival,Modifier.fillMaxWidth())
                        FeedPriceRow(trade,Modifier.fillMaxWidth())
                    }
                }
            }
        }
        for (text in listOf("到手价","¥2648","12GB+256GB","交易价","¥3900","16GB+512GB"))
            compose.onNodeWithText(text).assertIsDisplayed()
        val amount = compose.onNodeWithText("¥2648").fetchSemanticsNode().boundsInRoot
        val configuration = compose.onNodeWithText("12GB+256GB").fetchSemanticsNode().boundsInRoot
        assertTrue(configuration.left >= amount.right)
        assertEquals(amount.center.y, configuration.center.y, 1f)
    }

    @Test fun priceSummaryOpensTheSourceFeed() {
        val summary=item("""{"entityTemplate":"subTabFeedCard6","extraData":"{\"final_price\":\"2508.00\",\"update_time\":\"2026-10-25 14:58\",\"feed_id\":12345}"}""").purchasePriceSummary()!!
        var opened:String?=null
        compose.setContent { MiuixTheme { PurchasePriceSummaryCard(summary,{ id,_ -> opened=id }) } }
        compose.onNodeWithText("¥2508").assertIsDisplayed()
        compose.onNodeWithText("更新于 2026-10-25 14:58").assertIsDisplayed()
        compose.onNodeWithText("查看价格来源").performClick()
        compose.runOnIdle { assertEquals("12345",opened) }
    }

    @Test fun tradeIconBesidePriceOpensTheExactListingUrl() {
        val url = "https://m.tb.cn/h.example?tk=synthetic"
        val data = item("""{"feedType":"ershou","ershou_info":{"product_price":3900,"product_config_data":{"configTitle":"16GB+512GB"},"link_source":"闲鱼","link_url":"$url"}}""")
        val link = data.tradeLink()!!
        var opened: String? = null
        compose.setContent { MiuixTheme { Column(Modifier.width(280.dp)) { FeedProductInfo(data.feedPriceDisplay(), link, true, { opened = it }) } } }
        val amount = compose.onNodeWithText("¥3900").fetchSemanticsNode().boundsInRoot
        val icon = compose.onNodeWithContentDescription("查看闲鱼").fetchSemanticsNode().boundsInRoot
        val configuration = compose.onNodeWithText("16GB+512GB").fetchSemanticsNode().boundsInRoot
        assertTrue(icon.left >= amount.right)
        assertTrue(icon.top < amount.bottom && icon.bottom > amount.top)
        assertTrue(configuration.left >= amount.right)
        assertTrue(configuration.top < amount.bottom && configuration.bottom > amount.top)
        compose.onNodeWithContentDescription("查看闲鱼").performClick()
        compose.runOnIdle { assertEquals(url, opened) }
    }

    @Test fun previewOnlyMarksTheLinkWithoutAnExternalAction() {
        val link = item("""{"feedType":"ershou","ershou_info":{"link_source":"闲鱼","link_url":"https://m.tb.cn/h.example"}}""").tradeLink()!!
        compose.setContent { MiuixTheme { FeedProductInfo(null, link, false, { error("Preview must not open external links") }) } }
        compose.onNodeWithContentDescription("含闲鱼链接").assertIsDisplayed().assertHasNoClickAction()
        compose.onNodeWithText("含闲鱼链接").assertDoesNotExist()
        compose.onNodeWithContentDescription("查看闲鱼").assertDoesNotExist()
    }

    @Test fun detailHintIsBelowConfigurationAndCanBeDismissed() {
        val data = item("""{"feedType":"ershou","ershou_info":{"product_price":3900,"product_config_data":{"configTitle":"16GB+512GB"},"link_source":"闲鱼","link_url":"https://m.tb.cn/h.example"}}""")
        val visible = mutableStateOf(true)
        compose.setContent {
            MiuixTheme {
                Column(Modifier.width(280.dp)) {
                    FeedProductInfo(data.feedPriceDisplay(), data.tradeLink(), true, {},
                        showLinkHint = visible.value, onDismissLinkHint = { visible.value = false })
                }
            }
        }
        val configuration = compose.onNodeWithText("16GB+512GB").fetchSemanticsNode().boundsInRoot
        val hint = compose.onNodeWithText("点击图标即可跳转二手平台").fetchSemanticsNode().boundsInRoot
        assertTrue(hint.top >= configuration.bottom)
        compose.onNodeWithText("不再显示").performClick()
        compose.onNodeWithText("点击图标即可跳转二手平台").assertDoesNotExist()
        compose.onNodeWithContentDescription("查看闲鱼").assertIsDisplayed()
    }

    @Test fun ratingOptionsSelectTheMatchingRequestWithoutRepeatingSelection() {
        val selected=mutableStateOf(ProductSortType.OWNER)
        val url="/page?url=/product/feedList?type=rating&isOwner=1&id=5111"
        var calls=0
        compose.setContent {
            MiuixTheme {
                TopicSortOptions(topicSortOptions(url),selected.value) {
                    selected.value=it
                    calls++
                }
            }
        }
        compose.onNodeWithText("机主").assertIsSelected()
        for (label in listOf("最新","好评","差评","热度"))
            compose.onNodeWithText(label).performClick().assertIsSelected()
        compose.onNodeWithText("热度").performClick()
        compose.runOnIdle {
            assertEquals(4,calls)
            assertTrue(sortedTopicUrl(url,selected.value).contains("rank_score"))
        }
    }
}
