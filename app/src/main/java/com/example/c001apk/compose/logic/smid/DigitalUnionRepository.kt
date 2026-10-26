package com.example.c001apk.compose.logic.smid

import android.content.Context
import cn.shuzilm.core.Main as Shuzilm
import com.example.c001apk.compose.logic.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class DigitalUnionRepository
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val preferences: UserPreferencesRepository,
) {
    private val queryMutex = Mutex()
    private var initialized = false

    suspend fun ensureId(force: Boolean = false): Result<String> = queryMutex.withLock {
        val current = preferences.data.first()
        if (!force && current.szlmId.isNotBlank()) {
            if (current.xAppDevice.isBlank() || current.versionName == "13.4.1") {
                preferences.setSZLMId(current.szlmId)
            }
            return@withLock Result.success(current.szlmId)
        }
        try {
            // PhoneStateListener in the supplied bridge must be created on a Looper thread.
            withContext(Dispatchers.Main.immediate) {
                if (!initialized) {
                    Shuzilm.init(context, SmidConfig.DIGITAL_UNION_API_KEY, false)
                    listOf("pkglist", "operation", "cdlmt").forEach {
                        Shuzilm.setConfig(it, SmidConfig.DIGITAL_UNION_DISABLE)
                    }
                    initialized = true
                }
            }
            val id =
                queryDigitalUnionId { url ->
                    suspendCancellableCoroutine { continuation ->
                        Shuzilm.getQueryID(context, null, "", false, url) { value ->
                            if (continuation.isActive) continuation.resume(value)
                        }
                    }
                } ?: return@withLock Result.failure(IOException("暂时无法获取数盟 ID，请检查网络后重试"))
            preferences.setSZLMId(id)
            Result.success(id)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Result.failure(IOException("获取数盟 ID 失败，请稍后重试", failure))
        } catch (failure: LinkageError) {
            Result.failure(IOException("数盟组件加载失败", failure))
        }
    }
}
