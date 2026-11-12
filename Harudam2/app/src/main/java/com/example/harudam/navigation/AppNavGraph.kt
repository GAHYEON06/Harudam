
package com.example.harudam.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.harudam.ui.screens.LoginScreen
import com.example.harudam.ui.screens.SignUpScreen

private object Routes {
    const val LOGIN = "login"
    const val SIGN_UP = "signup"
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onSignUpClick = {
                    navController.navigate(Routes.SIGN_UP)
                }
            )
        }

        composable(Routes.SIGN_UP) {
            SignUpScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onLoginClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
