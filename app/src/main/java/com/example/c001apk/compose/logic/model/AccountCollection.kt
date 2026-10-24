package com.example.c001apk.compose.logic.model

import com.google.gson.annotations.SerializedName

/** Fields confirmed against the official client's collection requests. */
data class AccountCollection(
    val id: String?,
    val entityId: String?,
    val title: String?,
    val isBeCollected: Int?,
    @SerializedName("item_num") val itemCount: Int?,
    @SerializedName("is_open") val isOpen: Int?,
)

data class AccountCollectionResponse<T>(
    val data: T?,
    val message: String?,
    val status: Int?,
    val error: Int?,
) {
    fun requireData(): T {
        check(error == null || error == 0) { message ?: "收藏操作失败" }
        check(status == null || status >= 0) { message ?: "收藏操作失败" }
        return checkNotNull(data) { message ?: "未收到收藏结果，请重试" }
    }
}

data class CollectionSaveResult(val collect: Int?, val favnum: String?)
