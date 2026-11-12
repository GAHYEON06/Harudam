
package com.example.harudam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.harudam.navigation.AppNavGraph
import com.example.harudam.ui.theme.HarudamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HarudamTheme {
                AppNavGraph()
            }
        }
    }
}
