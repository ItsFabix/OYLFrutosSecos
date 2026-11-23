package com.example.oylfrutossecos.ui.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oylfrutossecos.data.repository.PickupWeatherRepository
import com.example.oylfrutossecos.ui.screens.PickupMapScreen
import com.example.oylfrutossecos.ui.viewmodel.PickupWeatherViewModel

@Composable
fun PickupMapRoute(onBack: () -> Unit) {
    val factory = remember { PickupWeatherViewModel.Factory(PickupWeatherRepository.shared) }
    val weather: PickupWeatherViewModel = viewModel(factory = factory)
    val state by weather.state.collectAsStateWithLifecycle()
    PickupMapScreen(weatherState = state, onWeatherPoint = weather::select,
        onRetryWeather = weather::retry, onBack = onBack)
}
