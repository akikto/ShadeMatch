package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.DentalShadeDatabase
import com.example.data.repository.DentalRepository
import com.example.ui.DentalApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.DentalViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: DentalViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = DentalShadeDatabase.getDatabase(applicationContext)
                val repo = DentalRepository(db)
                return DentalViewModel(repo) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                DentalApp(viewModel = viewModel)
            }
        }
    }
}
