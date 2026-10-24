package com.example.c001apk.compose.logic.update

import android.content.Context
import android.os.Build
import com.example.c001apk.compose.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*

@Singleton
class AppUpdates @Inject constructor(@param:ApplicationContext context: Context) {
    data class Preferences(val automatic: Boolean, val repository: String)

    data class State(
        val checking: Boolean = false,
        val release: ReleaseInfo? = null,
        val message: String? = null,
        val manual: Boolean = false,
    )

    private val storage = context.getSharedPreferences("miuix_app_updates", Context.MODE_PRIVATE)
    private val _preferences =
        MutableStateFlow(
            Preferences(
                storage.getBoolean("automatic", true),
                storage.getString("repository", BuildConfig.UPDATE_REPOSITORY).orEmpty(),
            ),
        )
    val preferences = _preferences.asStateFlow()
    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val client =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .followRedirects(false)
            .build()
    private var coldChecked = false
    private var job: Job? = null
    private var manualRequested = false

    fun setAutomatic(value: Boolean) {
        storage.edit().putBoolean("automatic", value).apply()
        _preferences.value = _preferences.value.copy(automatic = value)
        if (!value && !manualRequested) {
            job?.cancel()
            _state.value = State()
        }
    }

    fun setRepository(value: String): Boolean {
        val repo = if (value.isBlank()) "" else ReleaseInfo.repository(value) ?: return false
        job?.cancel()
        storage.edit().putString("repository", repo).remove("skipped").apply()
        _preferences.value = preferences.value.copy(repository = repo)
        _state.value = State()
        return true
    }

    fun coldStart() {
        if (coldChecked) return
        coldChecked = true
        if (preferences.value.automatic && preferences.value.repository.isNotEmpty())
            check(manual = false)
    }

    fun check(manual: Boolean = true) {
        if (job?.isActive == true) {
            manualRequested = manualRequested || manual
            return
        }
        val repository = preferences.value.repository
        if (repository.isBlank()) {
            if (manual) _state.value = State(message = "请先设置本 miuix 版本的发布仓库。", manual = true)
            return
        }
        manualRequested = manual
        _state.value = State(checking = true, manual = manual)
        job = scope.launch {
            try {
                val json = fetch(repository)
                val release =
                    withContext(Dispatchers.Default) {
                        ReleaseInfo.parse(json, repository, Build.SUPPORTED_ABIS.toList())
                    }
                if (repository != preferences.value.repository) return@launch
                val current =
                    AppVersion.parse(BuildConfig.APP_VERSION) ?: error("Invalid local version")
                val skipped = storage.getString("skipped", "") == "$repository:${release.version}"
                _state.value =
                    if (release.version > current && (manualRequested || !skipped))
                        State(release = release, manual = manualRequested)
                    else
                        State(
                            message = if (manualRequested) "当前已是最新版本" else null,
                            manual = manualRequested,
                        )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (repository == preferences.value.repository)
                    _state.value =
                        State(
                            message =
                                if (manualRequested)
                                    when (e) {
                                        is ReleaseFailure -> e.message
                                        is IOException -> "无法连接更新服务，请检查网络后重试"
                                        else -> "发布信息格式不受支持，请查看发布页"
                                    }
                                else null,
                            manual = manualRequested,
                        )
            }
        }
    }

    fun dismiss(skip: Boolean = false) {
        if (skip)
            state.value.release?.let {
                storage
                    .edit()
                    .putString("skipped", "${preferences.value.repository}:${it.version}")
                    .apply()
            }
        _state.value = State()
    }

    private class ReleaseFailure(message: String) : IOException(message)

    private suspend fun fetch(repository: String): String =
        suspendCancellableCoroutine { continuation ->
            val call =
                client.newCall(
                    Request.Builder()
                        .url("https://api.github.com/repos/$repository/releases/latest")
                        .header("Accept", "application/vnd.github+json")
                        .header("X-GitHub-Api-Version", "2026-03-10")
                        .header("User-Agent", "c001apk-miuix/${BuildConfig.APP_VERSION}")
                        .build(),
                )
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val result = runCatching {
                            response.use {
                                if (!it.isSuccessful)
                                    throw ReleaseFailure(
                                        when (it.code) {
                                            404 -> "该仓库尚无正式版本，或仓库地址不可访问"
                                            403,
                                            429 -> "更新服务请求过于频繁，请稍后重试"
                                            else -> "更新服务暂不可用（${it.code}）"
                                        },
                                    )
                                val bytes =
                                    it.body?.byteStream()?.readNBytes(1024 * 1024 + 1)
                                        ?: error("Empty release")
                                require(bytes.size <= 1024 * 1024)
                                bytes.toString(Charsets.UTF_8)
                            }
                        }
                        if (continuation.isActive)
                            result.fold(continuation::resume, continuation::resumeWithException)
                    }
                },
            )
        }
}
