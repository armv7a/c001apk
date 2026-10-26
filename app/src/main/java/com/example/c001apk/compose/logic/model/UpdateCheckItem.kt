package com.example.c001apk.compose.logic.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Created by bggRGjQaUbCoE on 2024/6/12
 */
@Parcelize
@Serializable
data class UpdateCheckItem(
    val key: String,
    val value: String,
) : Parcelable
