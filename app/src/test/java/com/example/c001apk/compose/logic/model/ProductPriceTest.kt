package com.example.c001apk.compose.logic.model

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class ProductPriceTest {
    private val gson = Gson()
    private fun item(json: String) = gson.fromJson(json, HomeFeedResponse.Data::class.java)

    @Test fun capturedResponseShapesKeepPricesAndConfigurations() {
        val json = javaClass.getResource("/product-price/captured-shapes.json")!!.readText()
        val items = gson.fromJson(json, HomeFeedResponse::class.java).data!!
        assertEquals("¥2508", items[0].purchasePriceSummary()!!.finalPrice.asProductPrice())
        assertEquals("12345", items[0].purchasePriceSummary()!!.feedId)
        assertEquals(FeedPriceDisplay("到手价", "¥2648", "12GB+256GB"), items[1].feedPriceDisplay())
        assertEquals("¥3708", items[4].feedPriceDisplay()!!.amount)
        assertEquals(FeedPriceDisplay("交易价", "¥3900", "16GB+512GB", inlineConfiguration = true), items[5].feedPriceDisplay())
        assertEquals(FeedPriceDisplay("交易价", "¥3900", null, inlineConfiguration = true), items[7].feedPriceDisplay())
    }

    @Test fun objectAndStringEncodingBothWork() {
        val data = item("""{"feedType":"ershou","ershou_info":{"product_price":"12.50","product_config_data":{"ram":"12GB","rom":"256GB"}}}""")
        assertEquals(FeedPriceDisplay("交易价", "¥12.5", "12GB+256GB", inlineConfiguration = true), data.feedPriceDisplay())
        assertEquals("¥99.99", item("""{"tsubid":6,"tsubdata":{"final_price":"99.99"}}""").feedPriceDisplay()!!.amount)
    }

    @Test fun missingAndMalformedOptionalFieldsDoNotBreakFeedParsing() {
        for (value in listOf("null", "0", "[]", "\"\"", "\"{broken\"", "{\"final_price\":[]}")) {
            val data = item("""{"id":"kept","tsubid":6,"tsubdata":$value,"ershou_info":$value,"extraData":$value}""")
            assertEquals("kept", data.id)
            assertNull(data.feedPriceDisplay())
            assertNull(data.purchasePriceSummary())
        }
    }

    @Test fun invalidOrZeroPricesAreNotDisplayedAsFreeItems() {
        for (price in listOf(null, "", "0", "0.00", "-1", "NaN", "1e30", "100.123", "¥12")) {
            assertNull(price.asProductPrice())
        }
        assertEquals("¥1234.56", " 1234.56 ".asProductPrice())
        assertEquals("¥2508", "2508.00".asProductPrice())
    }

    @Test fun unrelatedSubtabsAndUnrelatedCardsNeverShowPriceMetadata() {
        assertNull(item("""{"tsubid":4,"tsubdata":{"final_price":2000}}""").feedPriceDisplay())
        assertNull(item("""{"entityTemplate":"sortSelectCard","extraData":"{\"final_price\":2000}"}""").purchasePriceSummary())
        assertNull(item("""{"entityTemplate":"subTabFeedCard6","extraData":"{\"final_price\":0}"}""").purchasePriceSummary())
    }

    @Test fun tradeLinkKeepsTheOriginalProductUrlEvenWithoutPrice() {
        val url = "https://m.tb.cn/h.example?tk=synthetic"
        val data = item("""{"feedType":"ershou","ershou_info":{"link_source":"闲鱼","link_url":"$url","product_price":0}}""")
        assertEquals(TradeLink("查看闲鱼", url, "含闲鱼链接", isXianyu = true), data.tradeLink())
        assertNull(data.feedPriceDisplay())
        assertNull(data.copy(feedType = "feed").tradeLink())
    }

    @Test fun missingOrUnsupportedTradeLinksDoNotCreateButtons() {
        for (url in listOf("", "javascript:alert(1)", "intent://example", "file:///sdcard/test", "https://user:secret@example.com")) {
            val data = item("""{"feedType":"ershou"}""").copy(secondHandInfo = SecondHandInfo(linkUrl = url))
            assertNull(data.tradeLink())
        }
    }
}
