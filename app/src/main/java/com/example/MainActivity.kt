package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.LaserStudioApp
import com.example.ui.theme.LaserProStudioTheme
import com.example.viewmodel.LaserStudioViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: LaserStudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LaserProStudioTheme {
                LaserStudioApp(viewModel = viewModel)
            }
        }
    }
}
