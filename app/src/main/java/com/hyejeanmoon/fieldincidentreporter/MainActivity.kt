package com.hyejeanmoon.fieldincidentreporter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hyejeanmoon.fieldincidentreporter.feature.incidents.IncidentApp
import com.hyejeanmoon.fieldincidentreporter.ui.theme.FieldIncidentReporterTheme

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
