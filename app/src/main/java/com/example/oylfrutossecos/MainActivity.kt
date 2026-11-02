package com.example.oylfrutossecos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.example.oylfrutossecos.data.local.OylDatabase
import com.example.oylfrutossecos.data.repository.AuthRepository
import com.example.oylfrutossecos.ui.screens.*
import com.example.oylfrutossecos.ui.navigation.StoreNavigation
import com.example.oylfrutossecos.ui.theme.OYLFrutosSecosTheme
import com.example.oylfrutossecos.ui.viewmodel.AuthViewModel
import com.example.oylfrutossecos.ui.components.storeMessage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        setContent {
            OYLFrutosSecosTheme {
                Surface(Modifier.fillMaxSize()) {
                    OylApp()
                }
            }
        }
    }
}

@Composable
fun OylApp() {
    val context = LocalContext.current.applicationContext
    val factory = remember(context) {
        AuthViewModel.Factory(AuthRepository(OylDatabase.getInstance(context)))
    }
    val auth: AuthViewModel = viewModel(factory = factory)
    val currentUser = auth.user

    if (auth.restoring) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Abriendo OYL…")
            }
        }
    } else if (currentUser != null) {
        key(currentUser.id) {
            StoreNavigation(currentUser, auth.loggingOut, auth.sessionError, auth::logout)
        }
    } else if (auth.sessionError != null) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
                verticalArrangement = Arrangement.Center) {
                Text(storeMessage(auth.sessionError.orEmpty()))
                Button(onClick = auth::restoreSession) { Text("Reintentar") }
            }
        }
    } else {
        AuthNavigation(auth)
    }
    // Se compone después del NavHost para tener prioridad mientras se guarda.
    BackHandler(enabled = auth.loginState.busy || auth.registerState.busy || auth.loggingOut) {}
}

@Composable
private fun AuthNavigation(auth: AuthViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "welcome") {
        composable("welcome") {
            WelcomeScreen(onStartClick = {
                navController.navigate("login") { launchSingleTop = true }
            })
        }
        composable("login") {
            LoginScreen(
                state = auth.loginState,
                onAliasChange = { auth.changeLogin(alias = it) },
                onPasswordChange = { auth.changeLogin(password = it) },
                onRoleChange = { auth.changeLogin(role = it) },
                onLoginClick = auth::login,
                onRegisterClick = {
                    auth.clearForms()
                    navController.navigate("register") { launchSingleTop = true }
                },
                onBackClick = {
                    auth.clearForms()
                    navController.popBackStack()
                }
            )
        }
        composable("register") {
            LaunchedEffect(auth.registerState.completed) {
                if (auth.registerState.completed) {
                    navController.popBackStack("login", false)
                    auth.consumeRegistration()
                }
            }
            RegisterScreen(
                state = auth.registerState,
                onFormChange = auth::changeRegistration,
                onRegisterClick = auth::register,
                onBackClick = {
                    auth.clearForms()
                    navController.popBackStack()
                }
            )
        }
    }
}
