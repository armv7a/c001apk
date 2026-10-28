package com.example.c001apk.compose.logic.model

import android.os.Parcelable
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class PurchasePriceInfo(
    @SerializedName("final_price") val finalPrice: String? = null,
    @SerializedName("config_name") val configName: String? = null,
    @SerializedName("config_id") val configId: String? = null,
    @SerializedName("update_time") val updateTime: String? = null,
    @SerializedName("feed_id") val feedId: String? = null,
) : Parcelable

@Parcelize
data class SecondHandInfo(
    @SerializedName("product_price") val productPrice: String? = null,
    @SerializedName("link_url") val linkUrl: String? = null,
    @SerializedName("link_source") val linkSource: String? = null,
    @SerializedName("product_config_data")
    @JsonAdapter(EmbeddedObjectAdapter::class)
    val configuration: TradeConfiguration? = null,
) : Parcelable

@Parcelize
data class TradeConfiguration(
    val configTitle: String? = null,
    val ram: String? = null,
    val rom: String? = null,
) : Parcelable

data class FeedPriceDisplay(
    val label: String,
    val amount: String,
    val configuration: String?,
    val inlineConfiguration: Boolean = false,
)

data class TradeLink(val label: String, val url: String, val previewLabel: String, val isXianyu: Boolean = false)

fun HomeFeedResponse.Data.tradeLink(): TradeLink? {
    if (feedType !in setOf("ershou", "trade")) return null
    val info = secondHandInfo ?: return null
    val url = info.linkUrl?.trim()?.takeIf {
        com.example.c001apk.compose.util.CoolapkLink.external(it)
    } ?: return null
    return if (info.linkSource == "闲鱼") TradeLink("查看闲鱼", url, "含闲鱼链接", isXianyu = true)
    else TradeLink("查看交易链接", url, "含交易链接")
}

/** Do not turn missing/zero/invalid values into a free item or invent a negotiated price. */
internal fun String?.asProductPrice(): String? {
    val value = this?.trim() ?: return null
    if (!Regex("[0-9]{1,12}(\\.[0-9]{1,2})?").matches(value)) return null
    return value.toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
        ?.stripTrailingZeros()?.toPlainString()?.let { "¥$it" }
}

fun HomeFeedResponse.Data.feedPriceDisplay(): FeedPriceDisplay? {
    if (subTabId == "6") {
        purchasePrice?.finalPrice.asProductPrice()?.let { amount ->
            return FeedPriceDisplay("到手价", amount, purchasePrice?.configName?.takeIf { it.isNotBlank() }, inlineConfiguration = true)
        }
    }
    if (feedType in setOf("ershou", "trade")) {
        secondHandInfo?.productPrice.asProductPrice()?.let { amount ->
            val config = secondHandInfo?.configuration
            val name = config?.configTitle?.takeIf { it.isNotBlank() }
                ?: listOfNotNull(config?.ram?.takeIf { it.isNotBlank() }, config?.rom?.takeIf { it.isNotBlank() })
                    .joinToString("+").takeIf { it.isNotBlank() }
            return FeedPriceDisplay("交易价", amount, name, inlineConfiguration = true)
        }
    }
    return null
}

fun HomeFeedResponse.Data.purchasePriceSummary(): PurchasePriceInfo? =
    priceSummary?.takeIf {
        entityTemplate == "subTabFeedCard6" && it.finalPrice.asProductPrice() != null
    }
