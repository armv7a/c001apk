package com.example.c001apk.compose.ui

import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.platform.app.InstrumentationRegistry
import com.example.c001apk.compose.R
import com.example.c001apk.compose.view.MiuixCheckboxView
import com.example.c001apk.compose.view.MiuixInputLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MiuixEditorTest {
    @Test
    fun bothEditorLayoutsPreserveEditableTextWithoutMaterialTheme() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (orientation in
                listOf(Configuration.ORIENTATION_PORTRAIT, Configuration.ORIENTATION_LANDSCAPE)) {
                val base = instrumentation.targetContext
                val configuration =
                    Configuration(base.resources.configuration).apply {
                        this.orientation = orientation
                    }
                val context =
                    ContextThemeWrapper(
                        base.createConfigurationContext(configuration),
                        R.style.Theme_C001apkCompose,
                    )
                val root =
                    LayoutInflater.from(context).inflate(R.layout.activity_reply, null, false)
                val editor = root.findViewById<EditText>(R.id.editText)
                editor.setText("miuix 草稿")
                editor.setSelection(editor.length())
                editor.editableText.append(" #话题#")
                assertEquals("miuix 草稿 #话题#", editor.text.toString())
                assertTrue(root.findViewById<View>(R.id.textInputLayout) is MiuixInputLayout)
                assertTrue(root.findViewById<View>(R.id.checkBox) is MiuixCheckboxView)
            }
        }
    }
}
