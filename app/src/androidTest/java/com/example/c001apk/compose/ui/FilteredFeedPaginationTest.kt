package com.example.c001apk.compose.ui

import androidx.datastore.core.DataStoreFactory
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.example.c001apk.compose.logic.dao.StringEntityDao
import com.example.c001apk.compose.logic.datastore.UserPreferencesSerializer
import com.example.c001apk.compose.logic.filter.*
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.logic.network.ApiService
import com.example.c001apk.compose.logic.repository.BlackListRepo
import com.example.c001apk.compose.logic.repository.NetworkRepo
import com.example.c001apk.compose.logic.state.*
import com.example.c001apk.compose.ui.base.BaseViewModel
import com.google.gson.Gson
import java.lang.reflect.Proxy
import java.nio.file.Files
import java.util.Collections
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Test

class FilteredFeedPaginationTest {
    private inline fun <reified T> stub(noinline response: (String) -> Any?): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ -> response(method.name) } as T

    @Test fun rawCursorAndManualRetryRemainCorrectWhenEveryVisiblePostIsFiltered() = runBlocking {
        val directory = Files.createTempDirectory("feed-pagination").toFile()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val models = ViewModelStore()
        try {
            val data = DataStoreFactory.create(UserPreferencesSerializer(), scope = scope, produceFile = { directory.resolve("prefs.pb") })
            val filters = ContentFilterStore(data)
            filters.save(ContentRule("r", "广告", terms = listOf("广告")))
            filters.update { it.copy(enabled = true) }
            val api = stub<ApiService> { error("Unexpected network access: $it") }
            val dao = stub<StringEntityDao> { method -> when (method) {
                "isExist", "isContain" -> false
                else -> error("Unexpected database access: $method")
            } }
            val requests = Collections.synchronizedList(mutableListOf<Pair<Int, String?>>())
            val model = object : BaseViewModel(NetworkRepo(api, api, api, api), BlackListRepo(dao, dao), filters) {
                override suspend fun customFetchData() = flowOf<LoadingState<List<HomeFeedResponse.Data>>>(
                    if (page > 4) LoadingState.Empty else {
                        requests += page to lastItem
                        val text = if (page <= 3) "广告" else "正常动态"
                        val feed = Gson().fromJson("""{"id":"$page","entityId":"feed$page","entityType":"feed","message":"$text"}""", HomeFeedResponse.Data::class.java)
                        val tail = Gson().fromJson("""{"id":"raw$page","entityId":"raw$page","entityType":"unsupported"}""", HomeFeedResponse.Data::class.java)
                        LoadingState.Success(listOf(feed, tail))
                    },
                )
            }
            models.put("feed", model)
            model.fetchData()
            withTimeout(5000) { while (!model.automaticLoadPaused) delay(10) }
            assertEquals(listOf(1 to null, 2 to "raw1", 3 to "raw2"), requests.toList())
            assertFalse(model.isEnd)
            assertTrue(model.stopAutomaticLoading)
            assertTrue(model.footerState is FooterState.Error)
            assertEquals(3, (model.loadingState as LoadingState.Success).response.size)
            model.loadMore()
            withTimeout(5000) { while (model.isLoadMore || requests.size < 4) delay(10) }
            assertEquals(4 to "raw3", requests.last())
            assertFalse(model.stopAutomaticLoading)
            assertEquals(listOf("1", "2", "3", "4"), (model.loadingState as LoadingState.Success).response.map { it.id })
            model.loadMore()
            withTimeout(5000) { while (!model.isEnd) delay(10) }
            assertTrue(model.footerState is FooterState.End)
            assertEquals(4, (model.loadingState as LoadingState.Success).response.size)
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { models.clear() }
            scope.coroutineContext[Job]!!.cancelAndJoin()
            directory.deleteRecursively()
        }
    }
}
