package com.example.c001apk.compose.ui.topic

import com.example.c001apk.compose.logic.model.LikeResponse

/** Follow endpoints may acknowledge success without a data object. */
internal fun LikeResponse.topicFollowAcknowledged(): Boolean {
    if (error != null && error != 0) return false
    if (status != null && status != 1) return false
    if (!messageStatus.isNullOrBlank() && messageStatus != "1") return false
    return status == 1 || message == "关注成功" || message == "取消关注成功"
}
