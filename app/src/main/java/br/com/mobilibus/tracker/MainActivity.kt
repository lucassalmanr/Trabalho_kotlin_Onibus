package br.com.mobilibus.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import br.com.mobilibus.tracker.ui.MobilibusApp
import br.com.mobilibus.tracker.ui.TrackerTheme

class MainActivity : ComponentActivity() {
    private val graph by lazy { AppGraph(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TrackerTheme { MobilibusApp(graph) }
        }
    }
}
