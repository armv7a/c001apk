package com.example.c001apk.compose.logic.model

import com.example.c001apk.compose.logic.state.LoadingState
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class ProductCatalogTest {
    private fun fixture(name: String): List<HomeFeedResponse.Data> =
        javaClass.getResourceAsStream("/product-catalog/$name.json")!!.bufferedReader().use {
            Gson().fromJson(it, HomeFeedResponse::class.java).data!!
        }

    @Test fun categoriesKeepTheirChildrenAndExactFilterUrls() {
        val categories = productCategories(fixture("categories"))
        assertTrue(categories.first { it.page.title == "手机" }.children.isEmpty())
        val accessories = categories.first { it.page.title == "数码配件" }
        val powerBank = accessories.children.first { it.title == "充电宝" }
        assertTrue(powerBank.url.contains("id%3D1021%26isSecondCategory%3D1"))
        val other = accessories.children.last()
        assertEquals("其他", other.title)
        assertTrue(other.url.contains("id%3D1006%26filterOther%3Dtrue"))
        // Parent and Other share an id, but must remain distinct destinations.
        assertNotEquals(accessories.page.url, other.url)
    }

    @Test fun invalidAndRepeatedCategoryLinksAreDroppedWithoutInventingTargets() {
        val rows = Gson().fromJson("""{"data":[
            {"title":"配件","url":"/accessories","secondCategoryRows":[
                {"title":"缺少链接"},{"title":"","url":"/blank"},
                {"title":"充电宝","url":"/power-bank"},{"title":"重复","url":"/power-bank"}]},
            {"title":"重复父分类","url":"/accessories"},{"title":"无地址"}
        ]}""", HomeFeedResponse::class.java).data!!
        val categories = productCategories(rows)
        assertEquals(1, categories.size)
        assertEquals(listOf(TopicBean("/power-bank", "充电宝")), categories.single().children)
    }

    @Test fun brandLinksPreserveTheSelectedSubcategoryAndLastPartialRow() {
        val header = productCatalogHeader(fixture("power-bank"), "/product/categoryDetailList?id=1021")
        assertEquals(19, header.brands.size)
        assertEquals("品丘", header.brands.last().page.title)
        val xiaomi = header.brands.first { it.page.title == "小米" }
        assertTrue(xiaomi.page.url.contains("category_id%3D1021"))
        assertTrue(xiaomi.page.url.contains("isSecondCategory%3D1"))
        assertEquals(listOf("热度", "评分", "最新"), header.sorts.map { it.title })
        assertEquals(header.sorts.first().url, header.selectedSort)
    }

    @Test fun brandDefaultSortUsesServerSelectionAndExplicitSortTakesPrecedence() {
        val rows = fixture("xiaomi-power-bank")
        val header = productCatalogHeader(rows, "/product/seriesList?id=1016")
        assertEquals(header.sorts.last().url, header.selectedSort)
        val score = header.sorts[1]
        assertTrue(score.url.contains("limitField%3Dstar_total_count-80"))
        val sorted = productCatalogHeader(rows, score.url)
        assertEquals(score.url, sorted.selectedSort)
        assertEquals("star_average_score", productSortField(score.url))
    }

    @Test fun nestedPageWrappersAndMalformedLinksAreHandled() {
        assertEquals("hot_num", productSortField(
            "/page?url=%2Fpage%3Furl%3D%252Fproduct%252FseriesList%253FsortField%253Dhot_num",
        ))
        assertNull(productSortField("%invalid"))
        assertNull(productSortField("/product/categoryDetailList?id=1021"))
    }

    @Test fun sortingResponsesWithOnlyProductsRetainBrandAndSortControls() {
        val initial = productCatalogHeader(fixture("power-bank"), "/product/categoryDetailList?id=1021")
        val score = initial.sorts[1]
        val product = Gson().fromJson("""{"entityType":"product","id":"123"}""", HomeFeedResponse.Data::class.java)
        val sorted = productCatalogHeader(listOf(product), score.url, initial)
        assertEquals(initial.brands, sorted.brands)
        assertEquals(initial.sorts, sorted.sorts)
        assertEquals(score.url, sorted.selectedSort)
        assertEquals(sorted, productCatalogHeader(emptyList(), score.url, sorted))
    }

    @Test fun controlsDoNotAppearAsProductRowsAndEmptyProductsRemainEmpty() {
        val headers = fixture("power-bank")
        assertTrue(headers.all { it.isProductCatalogHeader() })
        assertEquals(LoadingState.Empty, productCatalogContent(LoadingState.Success(headers)))
        val product = Gson().fromJson("""{"entityType":"product","id":"123"}""", HomeFeedResponse.Data::class.java)
        val content = productCatalogContent(LoadingState.Success(headers + product)) as LoadingState.Success
        assertEquals(listOf(product), content.response)
        val error = LoadingState.Error("offline")
        assertSame(error, productCatalogContent(error))
        assertEquals(LoadingState.Loading, productCatalogContent(LoadingState.Loading))
    }

    @Test fun unrelatedCardsAreNotTakenOverByProductControls() {
        val row = Gson().fromJson("""{"entityType":"card","entityTemplate":"sortSelectCard","entityId":"unrelated"}""", HomeFeedResponse.Data::class.java)
        assertFalse(row.isProductCatalogHeader())
        assertTrue(productCatalogHeader(listOf(row), "/other").isEmpty)
        assertEquals(listOf(row), (productCatalogContent(LoadingState.Success(listOf(row))) as LoadingState.Success).response)
    }
}
