package com.example.c001apk.compose.logic.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductConfigResponseTest {

    @Test
    fun decodesNestedConfigDataIntoOrderedSections() {
        val response =
            Gson().fromJson(
                """
                {
                  "data": {
                    "id": 9135,
                    "title": "12GB+512GB",
                    "product_id": 5727,
                    "config_data": "{\"基本信息\":{\"发布时间\":\"2026年09月10日\",\"入网型号\":\"A3718\"},\"性能\":{\"RAM容量\":12,\"ROM容量\":\"256\"}}"
                  }
                }
                """.trimIndent(),
                ProductConfigResponse::class.java,
            )

        val sections = response.configSections()

        assertEquals("12GB+512GB", response.data?.title)
        assertEquals(listOf("基本信息", "性能"), sections.map { it.title })
        assertEquals(listOf("发布时间", "入网型号"), sections[0].items.map { it.name })
        assertEquals(listOf("2026年09月10日", "A3718"), sections[0].items.map { it.value })
        assertEquals(listOf("RAM容量", "ROM容量"), sections[1].items.map { it.name })
        assertEquals(listOf("12", "256"), sections[1].items.map { it.value })
    }

    @Test
    fun treatsDashAsAbsentAndFlattensArrays() {
        val response =
            Gson().fromJson(
                """
                {"data":{"config_data":"{\"屏幕\":{\"刷新率\":\"120Hz\",\"空值\":\"-\",\"列表\":[\"A\",\"B\"]}}"}}
                """.trimIndent(),
                ProductConfigResponse::class.java,
            )

        val items = response.configSections().single().items

        assertEquals(listOf("刷新率", "列表"), items.map { it.name })
        assertEquals(listOf("120Hz", "A / B"), items.map { it.value })
    }

    @Test
    fun splitsMultiValueSegmentsAndDropsBlanks() {
        val response =
            Gson().fromJson(
                """
                {"data":{"config_data":"{\"屏幕\":{\"分辨率\":\"2868x1320px|||1.5K|||460 PPI\",\"刷新率\":\"120|||120|||ProMotion\",\"充电\":\"|||支持|||USB PD\"}}"}}
                """.trimIndent(),
                ProductConfigResponse::class.java,
            )

        val items = response.configSections().single().items

        assertEquals(
            listOf(
                "2868x1320px\n1.5K\n460 PPI",
                "120\nProMotion",
                "支持\nUSB PD",
            ),
            items.map { it.value },
        )
    }

    @Test
    fun returnsEmptyWhenConfigDataIsMissingOrMalformed() {
        val missing =
            Gson().fromJson("""{"data":{"title":"x"}}""".trimIndent(), ProductConfigResponse::class.java)
        val malformed =
            Gson().fromJson(
                """{"data":{"config_data":"not-json"}}""".trimIndent(),
                ProductConfigResponse::class.java,
            )
        val emptyObject =
            Gson().fromJson("""{"data":{"config_data":"{}"}}""".trimIndent(), ProductConfigResponse::class.java)

        assertTrue(missing.configSections().isEmpty())
        assertTrue(malformed.configSections().isEmpty())
        assertTrue(emptyObject.configSections().isEmpty())
    }
}
