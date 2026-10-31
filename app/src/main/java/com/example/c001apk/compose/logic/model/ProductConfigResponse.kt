package com.example.c001apk.compose.logic.model

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

data class ProductConfigResponse(
    val data: ProductConfigDetail?,
    val status: Int?,
    val message: String?,
)

data class ProductConfigDetail(
    val id: Long?,
    val title: String?,
    @SerializedName("product_id")
    val productId: Long?,
    @SerializedName("config_data")
    val configData: String?,
)

data class ProductConfigData(
    val title: String,
    val sections: List<ProductSpecSection>,
)

data class ProductSpecSection(
    val title: String,
    val items: List<ProductSpecItem>,
)

data class ProductSpecItem(
    val name: String,
    val value: String,
    val group: String? = null,
)

private val nestedJsonType = object : TypeToken<Map<String, Map<String, JsonElement>>>() {}.type

fun ProductConfigResponse.configSections(gson: Gson = Gson()): List<ProductSpecSection> {
    val raw = data?.configData?.trim().orEmpty()
    if (raw.isEmpty() || raw == "{}") return emptyList()
    val sections =
        runCatching { gson.fromJson<Map<String, Map<String, JsonElement>>>(raw, nestedJsonType) }
            .getOrNull()
            ?: return emptyList()
    return sections.mapNotNull { (title, fields) ->
        val items =
            fields.mapNotNull { (name, value) ->
                val shown = value.displayValue() ?: return@mapNotNull null
                val trimmedName = name.trim()
                if (trimmedName.isEmpty()) null else ProductSpecItem(trimmedName, shown)
            }
        items.takeIf { it.isNotEmpty() }?.let { ProductSpecSection(title.trim(), it) }
    }
}

private const val MULTI_VALUE_SEPARATOR = "|||"

private fun JsonElement.displayValue(): String? {
    if (isJsonNull) return null
    val text =
        when {
            isJsonPrimitive -> asJsonPrimitive.asString.cleanMultiValue()
            isJsonArray -> asJsonArray.mapNotNull { it.displayValue() }.joinToString(" / ")
            isJsonObject ->
                asJsonObject.entrySet().mapNotNull { (name, value) ->
                    value.displayValue()?.let { "$name: $it" }
                }.joinToString(" / ")
            else -> toString()
        }
    return text.trim().takeIf { it.isNotEmpty() && it != "-" }
}

private fun String.cleanMultiValue(): String =
    if (!contains(MULTI_VALUE_SEPARATOR)) {
        this
    } else {
        split(MULTI_VALUE_SEPARATOR)
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "-" }
            .distinct()
            .joinToString("\n")
    }
