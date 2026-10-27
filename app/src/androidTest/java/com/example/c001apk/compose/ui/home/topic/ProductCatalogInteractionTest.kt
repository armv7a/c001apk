package com.example.c001apk.compose.ui.home.topic

import android.os.ParcelFileDescriptor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.c001apk.compose.logic.model.ProductBrand
import com.example.c001apk.compose.logic.model.ProductCategory
import com.example.c001apk.compose.logic.model.ProductCatalogHeader
import com.example.c001apk.compose.logic.model.TopicBean
import com.example.c001apk.compose.ui.carousel.ProductCatalogHeader
import com.example.c001apk.compose.ui.theme.C001apkComposeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import top.yukonga.miuix.kmp.basic.Text

class ProductCatalogInteractionTest {
    private val compose = createComposeRule()
    @get:Rule val rule: RuleChain = RuleChain.outerRule(object : ExternalResource() {
        override fun before() {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val appId = instrumentation.targetContext.packageName
            instrumentation.uiAutomation.executeShellCommand(
                "am start -W -n $appId/androidx.activity.ComponentActivity",
            ).use { ParcelFileDescriptor.AutoCloseInputStream(it).use { stream -> stream.readBytes() } }
        }
    }).around(compose)

    @Test fun selectingParentsExpandsChildrenAndChildSelectionChangesOnlyTheContent() {
        val categories = listOf(
            ProductCategory(TopicBean("/phones", "手机"), emptyList()),
            ProductCategory(TopicBean("/accessories", "数码配件"), listOf(
                TopicBean("/power-bank?isSecondCategory=1", "充电宝"),
                TopicBean("/accessories?filterOther=true", "其他"),
            )),
        )
        compose.setContent {
            C001apkComposeTheme(materialYou = false) {
                ProductCategoryLayout(categories) { Text("结果：${it.url}") }
            }
        }
        compose.onNodeWithText("充电宝").assertDoesNotExist()
        compose.onNodeWithText("数码配件").performClick().assertIsSelected()
        compose.onNodeWithText("充电宝").assertIsDisplayed().performClick().assertIsSelected()
        compose.onNodeWithText("结果：/power-bank?isSecondCategory=1").assertIsDisplayed()
        compose.onNodeWithText("其他").performClick().assertIsSelected()
        compose.onNodeWithText("结果：/accessories?filterOther=true").assertIsDisplayed()
        compose.onNodeWithText("数码配件").performClick().assertIsSelected()
        compose.onNodeWithText("结果：/accessories").assertIsDisplayed()
        compose.onNodeWithText("手机").performClick().assertIsSelected()
        compose.onNodeWithText("充电宝").assertDoesNotExist()
    }

    @Test fun allBrandsIncludeTheLastItemAndSortSelectionDoesNotNavigate() {
        val brands = (1..7).map { ProductBrand(TopicBean("/brand/$it?category_id=1021", "品牌$it"), null) }
        val sorts = listOf(TopicBean("/hot", "热度"), TopicBean("/score", "评分"), TopicBean("/new", "最新"))
        val opened = mutableListOf<String>()
        val selected = mutableListOf<TopicBean>()
        compose.setContent {
            C001apkComposeTheme(materialYou = false) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1.3f)) {
                    var header by remember { mutableStateOf(ProductCatalogHeader(brands, sorts, "/hot")) }
                    Box(Modifier.width(240.dp)) {
                        ProductCatalogHeader(header, { url, _ -> opened += url }) {
                            selected += it
                            header = header.copy(selectedSort = it.url)
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("品牌7").assertDoesNotExist()
        compose.onNodeWithText("全部").performClick()
        compose.onNodeWithText("品牌7").assertIsDisplayed().performClick()
        compose.onNodeWithText("评分").performClick().assertIsSelected()
        compose.onNodeWithText("评分").performClick()
        compose.runOnIdle {
            assertEquals(listOf("/brand/7?category_id=1021"), opened)
            assertEquals(listOf(sorts[1]), selected)
        }
        compose.onNodeWithText("收起").performClick()
        compose.onNodeWithText("品牌7").assertDoesNotExist()
    }
}
