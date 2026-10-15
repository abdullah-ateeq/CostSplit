package com.example.costsplit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.costsplit.ui.screens.CostSplitNavGraph
import com.example.costsplit.ui.theme.CostSplitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CostSplitTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CostSplitNavGraph()
                }
            }
        }
    }
}