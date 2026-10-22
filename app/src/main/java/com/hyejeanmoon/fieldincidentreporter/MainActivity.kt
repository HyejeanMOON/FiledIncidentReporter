package com.hyejeanmoon.fieldincidentreporter

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.hyejeanmoon.fieldincidentreporter.navigation.IncidentNavHost
import com.hyejeanmoon.fieldincidentreporter.ui.theme.FieldIncidentReporterTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val lightSystemBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(
            statusBarStyle = lightSystemBarStyle,
            navigationBarStyle = lightSystemBarStyle,
        )
        super.onCreate(savedInstanceState)
        setContent {
            FieldIncidentReporterTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    IncidentNavHost()
                }
            }
        }
    }
}
