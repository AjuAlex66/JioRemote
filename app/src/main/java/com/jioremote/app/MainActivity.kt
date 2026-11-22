package com.jioremote.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jioremote.app.ui.RemoteScreen
import com.jioremote.app.ui.RemoteTheme
import com.jioremote.app.ui.RemoteViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: RemoteViewModel by viewModels { RemoteViewModel.factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            RemoteTheme { RemoteScreen(state, viewModel) }
        }
    }
}
