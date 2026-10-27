package com.example.c001apk.compose.logic.model

import com.example.c001apk.compose.logic.state.LoadingState
import java.net.URI
import java.net.URLDecoder

internal data class ProductCategory(val page: TopicBean, val children: List<TopicBean>)

internal fun productCategories(rows: List<HomeFeedResponse.Data>): List<ProductCategory> =
    rows.mapNotNull { row ->
        catalogLink(row.title, row.url)?.let { page ->
            ProductCategory(
                page,
                row.secondCategoryRows.orEmpty().mapNotNull { catalogLink(it.title, it.url) }
                    .distinctBy { it.url },
            )
        }
    }.distinctBy { it.page.url }

private fun catalogLink(title: String?, url: String?): TopicBean? =
    if (title.isNullOrBlank() || url.isNullOrBlank()) null else TopicBean(url, title)

internal data class ProductBrand(val page: TopicBean, val logo: String?)

internal data class ProductCatalogHeader(
    val brands: List<ProductBrand> = emptyList(),
    val sorts: List<TopicBean> = emptyList(),
    val selectedSort: String? = null,
) {
    val isEmpty: Boolean get() = brands.isEmpty() && sorts.isEmpty()
}

internal fun HomeFeedResponse.Data.isProductCatalogHeader(): Boolean =
    (entityTemplate == "iconGridHorizonCard" && entityId == "category-detail") ||
        (entityTemplate == "sortSelectCard" &&
            entityId in setOf("productCategorySortSelect", "productSeriesSortSelect"))

internal fun productCatalogHeader(
    rows: List<HomeFeedResponse.Data>,
    requestUrl: String,
    previous: ProductCatalogHeader = ProductCatalogHeader(),
): ProductCatalogHeader {
    val headers = rows.filter { it.isProductCatalogHeader() }
    val brandCard = headers.firstOrNull { it.entityTemplate == "iconGridHorizonCard" }
    val brands = if (brandCard == null) previous.brands else
        brandCard.entities.orEmpty().mapNotNull { entity ->
            catalogLink(entity.title, entity.url)?.let { ProductBrand(it, entity.logo) }
        }.distinctBy { it.page.url }
    val sortCard = headers.firstOrNull { it.entityTemplate == "sortSelectCard" }
    val sorts = if (sortCard == null) previous.sorts else
        sortCard.entities.orEmpty().mapNotNull { catalogLink(it.title, it.url) }.distinctBy { it.url }
    val field = productSortField(requestUrl)
    val selected = sorts.firstOrNull { field != null && productSortField(it.url) == field }
        ?: sorts.firstOrNull { it.title == sortCard?.extraDataArr?.selectedTab }
        ?: sorts.firstOrNull { it.url == previous.selectedSort }
        ?: sorts.firstOrNull()
    return ProductCatalogHeader(brands, sorts, selected?.url)
}

// Decode each /page wrapper separately; never rebuild a server-provided filter URL.
internal fun productSortField(url: String): String? = runCatching {
    var current = url
    repeat(4) {
        val query = URI(current).rawQuery.orEmpty().split('&').associate {
            URLDecoder.decode(it.substringBefore('='), "UTF-8") to
                URLDecoder.decode(it.substringAfter('=', ""), "UTF-8")
        }
        query["sortField"]?.let { return@runCatching it }
        current = query["url"] ?: return@runCatching null
    }
    null
}.getOrNull()

internal fun productCatalogContent(
    state: LoadingState<List<HomeFeedResponse.Data>>,
): LoadingState<List<HomeFeedResponse.Data>> =
    if (state is LoadingState.Success) {
        val rows = state.response.filterNot { it.isProductCatalogHeader() }
        if (rows.isEmpty()) LoadingState.Empty else LoadingState.Success(rows)
    } else state
