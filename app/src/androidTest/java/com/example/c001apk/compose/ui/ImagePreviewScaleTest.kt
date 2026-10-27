package com.example.c001apk.compose.ui

import android.graphics.Bitmap
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.test.platform.app.InstrumentationRegistry
import net.mikaelzero.mojito.view.sketch.SketchContentLoaderImpl
import net.mikaelzero.mojito.view.sketch.core.SketchImageView
import net.mikaelzero.mojito.loader.OnLongTapCallback
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

class ImagePreviewScaleTest {
    @Test
    fun longImageReadingScaleStillAllowsLongPress() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val called = CountDownLatch(1)
        val bitmap = Bitmap.createBitmap(900, 6000, Bitmap.Config.ARGB_8888)
        lateinit var view: SketchImageView
        val downTime = SystemClock.uptimeMillis()
        instrumentation.runOnMainSync {
            val loader = SketchContentLoaderImpl()
            loader.init(instrumentation.targetContext, "", null, null)
            view = loader.providerRealView() as SketchImageView
            loader.providerView().apply {
                measure(
                    View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(2400, View.MeasureSpec.EXACTLY),
                )
                layout(0, 0, 1080, 2400)
            }
            assertTrue(loader.isLongImage(900, 6000))
            view.setImageDrawable(BitmapDrawable(view.resources, bitmap))
            val zoomer = checkNotNull(view.zoomer)
            assertTrue(zoomer.reset("long-preview-regression"))
            assertEquals(zoomer.fillZoomScale, zoomer.zoomScale, 0.001f)
            assertEquals(zoomer.fullZoomScale, zoomer.minZoomScale, 0.001f)
            loader.onLongTapCallback(object : OnLongTapCallback {
                override fun onLongTap(view: View, x: Float, y: Float) { called.countDown() }
            })
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 540f, 1200f, 0).also {
                view.dispatchTouchEvent(it)
                it.recycle()
            }
        }
        val received = called.await(3, TimeUnit.SECONDS)
        instrumentation.runOnMainSync {
            MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 540f, 1200f, 0).also {
                view.dispatchTouchEvent(it)
                it.recycle()
            }
            view.zoomer?.recycle("test-finished")
            view.setImageDrawable(null)
            bitmap.recycle()
        }
        assertTrue("Long-image reading mode must keep its long-press menu", received)
    }

    @Test
    fun landscapeAndPortraitFitAfterLateImageLoadAndCanZoomBackOut() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for ((width, height) in listOf(1600 to 900, 900 to 1600)) {
                val loader = SketchContentLoaderImpl()
                loader.init(instrumentation.targetContext, "", null, null)
                val view = loader.providerRealView() as SketchImageView
                val host = loader.providerView()
                host.measure(
                    View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(2400, View.MeasureSpec.EXACTLY),
                )
                host.layout(0, 0, 1080, 2400)
                // Original loading commonly completes after the opening animation.
                loader.loadAnimFinish()
                assertFalse(loader.isLongImage(width, height))
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                view.setImageDrawable(BitmapDrawable(view.resources, bitmap))
                val zoomer = checkNotNull(view.zoomer)
                assertTrue(zoomer.reset("preview-regression"))
                assertEquals(zoomer.fullZoomScale, zoomer.zoomScale, 0.001f)
                assertEquals(zoomer.fullZoomScale, zoomer.minZoomScale, 0.001f)
                val rect = RectF()
                zoomer.getDrawRect(rect)
                assertTrue(rect.width() <= 1080.1f && rect.height() <= 2400.1f)
                assertTrue(zoomer.zoom(zoomer.maxZoomScale, false))
                assertTrue(loader.dispatchTouchEvent(false, false, false, false))
                assertTrue(zoomer.zoom(zoomer.fullZoomScale, false))
                zoomer.getDrawRect(rect)
                assertTrue(rect.width() <= 1080.1f && rect.height() <= 2400.1f)
                zoomer.recycle("test-finished")
                view.setImageDrawable(null)
                bitmap.recycle()
            }
        }
    }
}
