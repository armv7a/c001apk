package com.example.c001apk.compose.logic.export

import android.content.Context
import android.graphics.BitmapFactory
import coil.imageLoader
import com.example.c001apk.compose.logic.markdown.MarkdownContent
import com.example.c001apk.compose.logic.model.FeedArticleContentBean
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.logic.repository.BlackListRepo
import com.example.c001apk.compose.logic.repository.NetworkRepo
import com.example.c001apk.compose.logic.state.LoadingState
import com.example.c001apk.compose.util.readAtMost
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import okhttp3.*

class FeedExportService
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
    private val network: NetworkRepo,
    private val blacklist: BlackListRepo,
) {
    enum class Format(val mime: String, val extension: String) {
        Markdown("text/markdown", "md"),
        Html("text/html", "html"),
    }

    data class Prepared(
        val name: String,
        val format: Format,
        val content: String,
        val warnings: List<String>,
    )

    // Public images must never inherit Coolapk credentials or verification cookies.
    private val images =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .build()

    suspend fun prepare(
        feed: HomeFeedResponse.Data,
        article: List<FeedArticleContentBean>?,
        format: Format,
    ): Prepared =
        withContext(Dispatchers.IO) {
            val warnings = mutableListOf<String>()
            var commentNote: String? = null
            val popular =
                try {
                    when (
                        val state =
                            withTimeout(25000) {
                                network
                                    .getFeedContentReply(
                                        feed.id.orEmpty(),
                                        "popular",
                                        1,
                                        null,
                                        null,
                                        1,
                                        feed.feedType ?: "feed",
                                        0,
                                        0,
                                        verifyCaptcha = false,
                                    )
                                    .first { it !is LoadingState.Loading }
                            }
                    ) {
                        is LoadingState.Success -> state.response
                        is LoadingState.Empty -> emptyList()
                        else -> {
                            commentNote = "热门评论未能取得，可在评论区完成验证或重试后再次导出。"
                            emptyList()
                        }
                    }
                } catch (_: TimeoutCancellationException) {
                    commentNote = "读取热门评论超时，本文件只包含已取得的评论。"
                    emptyList()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    commentNote = "热门评论读取失败，本文件只包含已取得的评论。"
                    emptyList()
                }
            commentNote?.let(warnings::add)
            val comments =
                (feed.topReplyRows.orEmpty() + popular)
                    .filter {
                        !it.id.isNullOrBlank() &&
                            (it.entityType == null || it.entityType == "feed_reply")
                    }
                    .distinctBy { it.id }
                    .filterNot { blacklist.checkUid(it.uid.orEmpty()) }
                    .take(3)
            val markdown = ContentExport.markdown(feed, article, comments, commentNote)
            var output = markdown
            if (format == Format.Html) {
                val urls =
                    MarkdownContent.parse(markdown, explicit = true, coolapk = false).imageUrls
                val data = mutableMapOf<String, String>()
                var bytes = 0
                try {
                    withTimeout(45000) {
                        for (url in urls.take(60)) {
                            ensureActive()
                            try {
                                val raw = cached(url) ?: download(url)
                                require(
                                    raw.size <= 8 * 1024 * 1024 &&
                                        bytes + raw.size <= 32 * 1024 * 1024,
                                )
                                val options =
                                    BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                BitmapFactory.decodeByteArray(raw, 0, raw.size, options)
                                require(
                                    options.outWidth > 0 &&
                                        options.outHeight > 0 &&
                                        options.outWidth.toLong() * options.outHeight <= 32000000,
                                )
                                val mime = options.outMimeType
                                require(
                                    mime in
                                        setOf("image/png", "image/jpeg", "image/gif", "image/webp"),
                                )
                                // Bounds alone can accept a corrupt/truncated stream; verify a
                                // small decoded sample.
                                val sample =
                                    BitmapFactory.Options().apply {
                                        inSampleSize =
                                            maxOf(options.outWidth, options.outHeight)
                                                .div(128)
                                                .coerceAtLeast(1)
                                    }
                                val decoded =
                                    BitmapFactory.decodeByteArray(raw, 0, raw.size, sample)
                                        ?: error("Invalid image")
                                decoded.recycle()
                                data[url] =
                                    "data:$mime;base64," + Base64.getEncoder().encodeToString(raw)
                                bytes += raw.size
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                /* Missing images are explicitly confirmed below. */
                            }
                        }
                    }
                } catch (_: TimeoutCancellationException) {
                    /* Keep completed images. */
                }
                val missing = urls.size - data.size
                val imageNote =
                    if (missing > 0) "$missing 张图片未保存（下载失败、超出大小/数量限制或超时），仅保留来源链接。" else null
                imageNote?.let(warnings::add)
                output = ContentExport.offlineHtml(markdown, data, imageNote)
            }
            val title =
                ContentExport.text(feed.messageTitle ?: feed.title)
                    .ifBlank { "动态-${feed.id}" }
                    .replace(Regex("[\\p{Cntrl}\\\\/:*?\"<>|]"), "_")
                    .take(80)
            Prepared("$title.${format.extension}", format, output, warnings)
        }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    private fun cached(url: String): ByteArray? = runCatching {
        context.imageLoader.diskCache?.openSnapshot(url)?.use { snapshot ->
            snapshot.data
                .toFile()
                .inputStream()
                .use { it.readAtMost(8 * 1024 * 1024 + 1) }
                .takeIf { it.size <= 8 * 1024 * 1024 }
        }
    }
        .getOrNull()

    private suspend fun download(url: String): ByteArray =
        suspendCancellableCoroutine { continuation ->
            val call = images.newCall(Request.Builder().url(url).build())
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val result = runCatching {
                            response.use {
                                check(it.isSuccessful)
                                val body = it.body ?: error("Empty image")
                                require(body.contentLength() <= 8 * 1024 * 1024)
                                body.byteStream().readAtMost(8 * 1024 * 1024 + 1).also { raw ->
                                    require(raw.size <= 8 * 1024 * 1024)
                                }
                            }
                        }
                        if (continuation.isActive)
                            result.fold(continuation::resume, continuation::resumeWithException)
                    }
                },
            )
        }
}
