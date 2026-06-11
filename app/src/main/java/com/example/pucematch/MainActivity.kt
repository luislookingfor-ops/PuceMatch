package com.example.pucematch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.pucematch.domain.Screen
import com.example.pucematch.presentation.chat.ChatDetailScreenStateful
import com.example.pucematch.presentation.home.HomeScreenStateful
import com.example.pucematch.presentation.login.LoginScreenStateful
import com.example.pucematch.presentation.register.RegisterScreenStateful
import com.example.pucematch.ui.theme.PuceMatchTheme

/**
 * Activity principal de PuceMatch.
 * Configura el NavHost con rutas @Serializable fuertemente tipadas.
 * NO usa Scaffold propio — cada pantalla maneja su propio Scaffold
 * para evitar doble padding con enableEdgeToEdge().
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PuceMatchTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = Screen.Login,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable<Screen.Login> {
                        LoginScreenStateful(navController = navController)
                    }

                    composable<Screen.Register> {
                        RegisterScreenStateful(navController = navController)
                    }

                    composable<Screen.Home> {
                        HomeScreenStateful(navController = navController)
                    }

                    composable<Screen.ChatDetail> { backStackEntry ->
                        val chatDetail: Screen.ChatDetail = backStackEntry.toRoute()
                        ChatDetailScreenStateful(
                            navController = navController,
                            matchId = chatDetail.matchId
                        )
                    }
                }
            }
        }
    }
}
