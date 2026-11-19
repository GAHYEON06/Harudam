package com.example.harudam.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.harudam.googleIdToken
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.harudam.AuthRepository
import com.example.harudam.ui.screens.TravelFlow
import com.example.harudam.viewmodel.TravelViewModel
import com.google.firebase.auth.FirebaseAuth
import com.example.harudam.ui.screens.LoginScreen
import com.example.harudam.ui.screens.SignUpScreen
import com.example.harudam.viewmodel.AuthViewModel

private object Routes {
    const val LOGIN = "login"
    const val SIGN_UP = "signup"
    const val HOME = "home"
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val repository = remember { AuthRepository() }
    val travelViewModel: TravelViewModel = viewModel()

    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val signInGoogle: () -> Unit = {
        if (!loading) {
            loading = true
            message = null
            scope.launch {
                try {
                    val token = googleIdToken(context)
                    repository.loginWithGoogle(token, onSuccess = {
                        loading = false
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                            launchSingleTop = true
                        }
                    }, onError = { loading = false; message = it })
                } catch (e: GetCredentialCancellationException) {
                    loading = false
                } catch (e: CancellationException) {
                    loading = false
                    throw e
                } catch (e: Exception) {
                    loading = false
                    message = "Google 로그인 설정 또는 기기의 Google 계정을 확인해 주세요. 이메일 로그인도 사용할 수 있어요."
                }
            }
        }
    }

    // 인증 요청 처리 중에는 뒤로 이동하지 않음
    BackHandler(enabled = loading) {}

    NavHost(
        navController = navController,
        startDestination = if (FirebaseAuth.getInstance().currentUser != null) Routes.HOME else Routes.LOGIN
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

                onGoogleLoginClick = signInGoogle
                ,onResetPasswordClick = {
                    val email = authViewModel.loginUiState.email.trim()
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        message = "이메일을 입력한 다음 비밀번호 재설정을 눌러 주세요."
                    } else if (!loading) {
                        loading = true
                        repository.resetPassword(email, onSuccess = {
                            loading = false
                            message = "해당 이메일로 재설정 안내를 요청했어요. 받은편지함과 스팸함을 확인해 주세요."
                        }, onError = { loading = false; message = it })
                    }
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

                onGoogleSignUpClick = signInGoogle
            )
        }

        composable(Routes.HOME) {
            TravelFlow(travelViewModel) {
                repository.logout()
                message = null
                authViewModel.onEmailChange("")
                authViewModel.onPasswordChange("")
                navController.navigate(Routes.LOGIN) {
                    popUpTo(Routes.HOME) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }
}
