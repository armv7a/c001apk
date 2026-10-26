package com.example.c001apk.compose.ui.chat

import android.content.Context
import android.net.Uri
import com.alibaba.sdk.android.oss.*
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback
import com.alibaba.sdk.android.oss.common.auth.OSSStsTokenCredentialProvider
import com.alibaba.sdk.android.oss.model.*
import com.example.c001apk.compose.logic.model.OSSUploadPrepareModel
import com.example.c001apk.compose.logic.repository.NetworkRepo
import com.example.c001apk.compose.util.OSSUtil
import com.example.c001apk.compose.util.OSSUtil.toHex
import com.example.c001apk.compose.util.readAtMost
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class ChatImageUploader
@Inject
constructor(@ApplicationContext private val context: Context, private val network: NetworkRepo) {
    suspend fun upload(uri: Uri, uid: String, active: () -> Boolean): String =
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use {
                require(it.readAtMost(20 * 1024 * 1024 + 1).size <= 20 * 1024 * 1024) {
                    "图片不能超过 20 MiB"
                }
            } ?: error("图片无法读取")
            val (dimensions, digest) =
                OSSUtil.getImageDimensionsAndMD5(context.contentResolver, uri)
            val (width, height, mime) = dimensions ?: error("图片无法解码")
            require(width > 0 && height > 0 && width.toLong() * height <= 32_000_000L) {
                "图片尺寸无效或过大"
            }
            require(mime in listOf("image/jpeg", "image/png", "image/gif", "image/webp")) {
                "图片格式不支持"
            }
            require(active()) { "账号已切换" }
            val name = "${java.util.UUID.randomUUID()}.${mime.substringAfter('/')}"
            val response =
                network
                    .postOSSUploadPrepare(
                        hashMapOf(
                            "uploadBucket" to "message",
                            "uploadDir" to "message",
                            "is_anonymous" to "0",
                            "toUid" to uid,
                            "uploadFileList" to
                                Gson()
                                    .toJson(
                                        listOf(
                                            OSSUploadPrepareModel(
                                                name,
                                                "${width}x${height}",
                                                digest?.toHex() ?: error("图片校验失败"),
                                            ),
                                        ),
                                    ),
                        ),
                    )
                    .first()
                    .getOrThrow()
            val prepared = response.data ?: error("无法准备图片上传")
            val info = prepared.uploadPrepareInfo
            val path = prepared.fileInfo.firstOrNull()?.uploadFileName ?: error("上传配置不完整")
            require(path.startsWith("message/") && active()) { "上传配置已失效" }
            require(
                listOf(info.accessKeyId, info.accessKeySecret, info.securityToken, info.bucket)
                    .all { it.isNotBlank() },
            )
            val endpoint =
                "https://" + info.endPoint.removePrefix("https://").removePrefix("http://")
            val client =
                OSSClient(
                    context,
                    endpoint,
                    OSSStsTokenCredentialProvider(
                        info.accessKeyId,
                        info.accessKeySecret,
                        info.securityToken,
                    ),
                    ClientConfiguration().apply {
                        connectionTimeout = 15000
                        socketTimeout = 60000
                        maxErrorRetry = 0
                    },
                )
            val request =
                PutObjectRequest(info.bucket, path, uri).apply {
                    metadata = ObjectMetadata().apply { contentType = mime }
                }
            suspendCancellableCoroutine<Unit> { continuation ->
                val task =
                    client.asyncPutObject(
                        request,
                        object : OSSCompletedCallback<PutObjectRequest, PutObjectResult> {
                            override fun onSuccess(
                                request: PutObjectRequest?,
                                result: PutObjectResult?,
                            ) {
                                if (continuation.isActive) continuation.resume(Unit)
                            }

                            override fun onFailure(
                                request: PutObjectRequest?,
                                clientException: ClientException?,
                                serviceException: ServiceException?,
                            ) {
                                if (continuation.isActive)
                                    continuation.resumeWithException(java.io.IOException("图片上传失败"))
                            }
                        },
                    )
                continuation.invokeOnCancellation { task.cancel() }
            }
            require(active()) { "账号已切换" }
            "/$path"
        }
}
