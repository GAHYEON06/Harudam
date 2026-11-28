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
                            run {
                                androidx.compose.material3.Text(if (BuildConfig.DEBUG && BuildConfig.USE_FIREBASE_EMULATORS) "테스트 계정 · 이 기기에 저장" else "이 기기에 저장 · 기기 간 동기화 없음",
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
