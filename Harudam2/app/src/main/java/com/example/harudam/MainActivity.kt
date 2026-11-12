package com.example.harudam

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.harudam.ui.screens.HarudamHomeScreen
import com.example.harudam.ui.screens.LoginScreen
import com.example.harudam.ui.screens.SignUpScreen
import com.example.harudam.ui.theme.HarudamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HarudamTheme {
                val repository = remember { AuthRepository() }
                var screen by remember { mutableStateOf("login") }

                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                    ) {
                        when (screen) {
                            "login" -> {
                                LoginScreen(
                                    onLoginClick = {
                                        screen = "home"
                                    },
                                    onSignUpClick = {
                                        screen = "signup"
                                    },
                                    onGoogleLoginClick = {
                                        showMessage(
                                            "Google 로그인은 준비 중이에요."
                                        )
                                    }
                                )
                            }

                            "signup" -> {
                                SignUpScreen(
                                    onSignUpClick = {
                                        showMessage(
                                            "가입이 완료됐어요. 로그인해 주세요!"
                                        )
                                        screen = "login"
                                    },
                                    onLoginClick = {
                                        screen = "login"
                                    },
                                    onBackClick = {
                                        screen = "login"
                                    },
                                    onGoogleSignUpClick = {
                                        showMessage(
                                            "Google 가입은 준비 중이에요."
                                        )
                                    }
                                )
                            }

                            "home" -> {
                                Column(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    TextButton(
                                        onClick = {
                                            repository.logout()
                                            screen = "login"
                                        }
                                    ) {
                                        Text("로그아웃")
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    ) {
                                        HarudamHomeScreen()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}