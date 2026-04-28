package com.okamilang.mysteria

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.okamilang.mysteria.ui.MysteriaApp
import com.okamilang.mysteria.ui.theme.BrunTresSombre
import com.okamilang.mysteria.ui.theme.MysteriaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MysteriaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BrunTresSombre
                ) {
                    MysteriaApp()
                }
            }
        }
    }
}
