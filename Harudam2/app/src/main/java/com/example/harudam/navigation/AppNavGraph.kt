
package com.example.harudam.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.harudam.ui.screens.LoginScreen
import com.example.harudam.ui.screens.SignUpScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.harudam.viewmodel.AuthViewModel

private object Routes {
    const val LOGIN = "login"
    const val SIGN_UP = "signup"
}


@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    // 로그인과 회원가입에서 공유할 ViewModel
    val authViewModel: AuthViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                uiState = authViewModel.loginUiState,

                onEmailChange =
                    authViewModel::onEmailChange,

                onPasswordChange =
                    authViewModel::onPasswordChange,

                onLoginClick =
                    authViewModel::validateLogin,

                onSignUpClick = {
                    navController.navigate(Routes.SIGN_UP)
                }
            )
        }

        composable(Routes.SIGN_UP) {
            SignUpScreen(
                uiState = authViewModel.signUpUiState,

                onEmailChange =
                    authViewModel::onSignUpEmailChange,

                onPasswordChange =
                    authViewModel::onSignUpPasswordChange,

                onAgreementChange =
                    authViewModel::onAgreementChange,

                onSignUpClick =
                    authViewModel::validateSignUp,

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

