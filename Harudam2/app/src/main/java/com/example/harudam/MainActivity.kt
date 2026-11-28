package com.example.harudam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.harudam.navigation.AppNavGraph
import com.example.harudam.ui.theme.HarudamTheme
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseEnvironment.configure()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )

        setContent {
            HarudamTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                    ) {
                        Column {
                            if (BuildConfig.DEBUG && BuildConfig.USE_FIREBASE_EMULATORS) {
                                androidx.compose.material3.Text("로컬 테스트 · 운영 데이터와 분리됨",
                                    modifier = Modifier.fillMaxWidth(),
                                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                            }
                            Box(Modifier.weight(1f)) { AppNavGraph() }
                        }
                    }
                }
            }
        }
    }
}
