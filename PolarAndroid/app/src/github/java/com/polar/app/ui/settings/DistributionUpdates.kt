package com.polar.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.polar.app.data.UpdateRepository

@Composable
fun DistributionUpdates() {
    val context = LocalContext.current.applicationContext
    val updates: UpdateViewModel = viewModel(factory = viewModelFactory {
        initializer { UpdateViewModel(UpdateRepository(context)) }
    })
    UpdateSection(updates)
}
