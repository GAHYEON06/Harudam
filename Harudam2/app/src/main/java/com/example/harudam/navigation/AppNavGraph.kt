package com.example.harudam.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.harudam.AuthRepository
import com.example.harudam.ui.screens.HomeScreen
import com.example.harudam.ui.screens.LoginScreen
import com.example.harudam.ui.screens.SignUpScreen
import com.example.harudam.viewmodel.AuthViewModel
import com.example.harudam.ui.screens.TripInfoInputScreen
import com.example.harudam.ui.screens.TripScheduleScreen

private object Routes {
    const val LOGIN = "login"
    const val SIGN_UP = "signup"
    const val HOME = "home"
    const val CREATE_TRIP = "create_trip"
    const val TRIP_SCHEDULE = "trip_schedule"
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val repository = remember { AuthRepository() }
    val startDestination = remember {
        if (repository.isLoggedIn()) Routes.HOME else Routes.LOGIN
    }

    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    // 인증 요청 처리 중에는 뒤로 이동하지 않음
    BackHandler(enabled = loading) {}

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                uiState = authViewModel.loginUiState,
                isLoading = loading,
                message = message,

                onEmailChange = {
                    message = null
                    authViewModel.onEmailChange(it)
                },

                onPasswordChange = {
                    message = null
                    authViewModel.onPasswordChange(it)
                },

                onLoginClick = {
                    if (!loading) {
                        message = null
                        authViewModel.validateLogin()

                        val state = authViewModel.loginUiState

                        if (state.isInputValid) {
                            loading = true

                            repository.login(
                                email = state.email.trim(),
                                password = state.password,
                                onSuccess = {
                                    loading = false
                                    authViewModel.onPasswordChange("")

                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.LOGIN) {
                                            inclusive = true
                                        }
                                        launchSingleTop = true
                                    }
                                },
                                onError = {
                                    loading = false
                                    message = it
                                }
                            )
                        }
                    }
                },

                onSignUpClick = {
                    message = null

                    navController.navigate(Routes.SIGN_UP) {
                        launchSingleTop = true
                    }
                },

                onGoogleLoginClick = {
                    message = "Google 로그인은 아직 연결하지 않았어요."
                }
            )
        }

        composable(Routes.SIGN_UP) {
            SignUpScreen(
                uiState = authViewModel.signUpUiState,
                isLoading = loading,
                message = message,

                onEmailChange = {
                    message = null
                    authViewModel.onSignUpEmailChange(it)
                },

                onPasswordChange = {
                    message = null
                    authViewModel.onSignUpPasswordChange(it)
                },

                onAgreementChange = {
                    message = null
                    authViewModel.onAgreementChange(it)
                },

                onSignUpClick = {
                    if (!loading) {
                        message = null
                        authViewModel.validateSignUp()

                        val state = authViewModel.signUpUiState

                        if (state.isInputValid) {
                            if (state.password.length < 6) {
                                message = "비밀번호는 최소 6자 이상 입력해 주세요."
                            } else {
                                loading = true
                                val email = state.email.trim()

                                repository.signUp(
                                    username = email
                                        .substringBefore("@")
                                        .take(30),
                                    email = email,
                                    password = state.password,
                                    onSuccess = {
                                        loading = false

                                        // 가입한 이메일을 로그인 화면에 채움
                                        authViewModel.onEmailChange(email)
                                        authViewModel.onPasswordChange("")

                                        authViewModel.onSignUpEmailChange("")
                                        authViewModel.onSignUpPasswordChange("")
                                        authViewModel.onAgreementChange(false)

                                        message =
                                            "가입이 완료됐어요. 로그인해 주세요!"

                                        navController.popBackStack()
                                    },
                                    onError = {
                                        loading = false
                                        message = it
                                    }
                                )
                            }
                        }
                    }
                },

                onBackClick = {
                    if (!loading) {
                        message = null
                        authViewModel.onSignUpPasswordChange("")
                        navController.popBackStack()
                    }
                },

                onLoginClick = {
                    if (!loading) {
                        message = null
                        authViewModel.onSignUpPasswordChange("")
                        navController.popBackStack()
                    }
                },

                onGoogleSignUpClick = {
                    message = "Google 가입은 아직 연결하지 않았어요."
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onAddTripClick = {
                    navController.navigate(Routes.CREATE_TRIP)
                },
                onTripClick = {
                    navController.navigate(Routes.TRIP_SCHEDULE)
                }
            )
        }

        composable(Routes.CREATE_TRIP) {
            TripInfoInputScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.TRIP_SCHEDULE) {
            TripScheduleScreen()
        }
    }
}