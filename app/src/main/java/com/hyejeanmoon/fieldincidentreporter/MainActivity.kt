package com.hyejeanmoon.fieldincidentreporter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hyejeanmoon.fieldincidentreporter.feature.IncidentApp
import com.hyejeanmoon.fieldincidentreporter.ui.theme.FieldIncidentReporterTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FieldIncidentReporterTheme {
                IncidentApp()
            }
        }
    }
}
