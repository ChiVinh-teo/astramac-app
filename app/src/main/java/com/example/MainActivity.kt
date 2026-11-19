package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.firebase.configureAppCheck
import com.example.ui.AstraMacApp
import com.example.ui.theme.AstraMacTheme

class MainActivity : ComponentActivity() {
    Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
    val sw = java.io.StringWriter()
    throwable.printStackTrace(java.io.PrintWriter(sw))
    val errText = sw.toString()
    runOnUiThread {
        val tv = android.widget.TextView(this).apply {
            text = "LỖI KHỞI ĐỘNG APP:\n\n$errText"
            setTextColor(android.graphics.Color.RED)
            setBackgroundColor(android.graphics.Color.WHITE)
            setPadding(32, 64, 32, 32)
            setTextIsSelectable(true)
        }
        setContentView(android.widget.ScrollView(this).apply { addView(tv) })
    }
    }
    
