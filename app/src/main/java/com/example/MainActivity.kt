package com.example

import android.graphics.Color
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.firebase.configureAppCheck
import com.example.ui.AstraMacApp
import com.example.ui.theme.AstraMacTheme
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val errText = sw.toString()
            runOnUiThread {
                showErrorScreen(errText)
            }
        }

        try {
            try {
                configureAppCheck(this, intent)
            } catch (e: Throwable) {
                e.printStackTrace()
            }

            enableEdgeToEdge()
            setContent {
                AstraMacTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AstraMacApp()
                    }
                }
            }
        } catch (t: Throwable) {
            val sw = StringWriter()
            t.printStackTrace(PrintWriter(sw))
            showErrorScreen(sw.toString())
        }
    }

    private fun showErrorScreen(errText: String) {
        val tv = TextView(this).apply {
            text = "LỖI KHỞI ĐỘNG APP:\n\n$errText"
            setTextColor(Color.RED)
            setBackgroundColor(Color.WHITE)
            setPadding(32, 64, 32, 32)
            setTextIsSelectable(true)
        }
        setContentView(ScrollView(this).apply { addView(tv) })
    }
}
