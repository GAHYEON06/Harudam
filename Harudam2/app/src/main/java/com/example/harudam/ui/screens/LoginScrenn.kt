package com.example.harudam.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.harudam.viewmodel.LoginUiState

private val LoginAccent = Color(0xFFA0E8CB)
private val LoginInk = Color(0xFF34445E)
private val LoginHint = Color(0xFF9AA3AE)
private val LoginLine = Color(0xFFE1E5EA)
private val LoginCanvas = Color(0xFFF5F4FA)

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onGoogleLoginClick: () -> Unit = {},
    onSignUpClick: () -> Unit = {},
    isLoading: Boolean = false,
    message: String? = null,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = LoginCanvas
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 30.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "HARUDAM",
                color = LoginAccent,
                fontSize = 35.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(7.dp))

            Text(
                text = "여행 기록을 이어서 계획해 보세요.",
                color = LoginHint,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(70.dp))

            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email") },
                placeholder = { Text("이메일 주소") },
                singleLine = true,
                isError = uiState.emailError != null,
                shape = RoundedCornerShape(14.dp),
                colors = loginFieldColors()
            )

            uiState.emailError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(13.dp))

            OutlinedTextField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password") },
                placeholder = { Text("비밀번호") },
                singleLine = true,
                isError = uiState.passwordError != null,
                visualTransformation =
                    if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(
                        enabled = !isLoading,
                        onClick = {
                            passwordVisible = !passwordVisible
                        }
                    ) {
                        Icon(
                            imageVector =
                                if (passwordVisible) Icons.Filled.Visibility
                                else Icons.Filled.VisibilityOff,
                            contentDescription =
                                if (passwordVisible) "비밀번호 숨기기"
                                else "비밀번호 보이기",
                            tint = LoginHint
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = loginFieldColors()
            )

            uiState.passwordError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!message.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))

                Text(
                    text = message,
                    color = LoginInk,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(22.dp))

            Button(
                onClick = onLoginClick,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LoginAccent
                )
            ) {
                Text(
                    text = if (isLoading) "로그인 중…" else "로그인",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(23.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.weight(1f).height(1.dp)
                        .background(LoginLine)
                )

                Text(
                    text = "또는 Google 계정으로 로그인",
                    modifier = Modifier.padding(horizontal = 10.dp),
                    color = LoginHint,
                    fontSize = 12.sp
                )

                Box(
                    Modifier.weight(1f).height(1.dp)
                        .background(LoginLine)
                )
            }

            Spacer(Modifier.height(17.dp))

            OutlinedButton(
                onClick = onGoogleLoginClick,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(13.dp)
            ) {
                Text(
                    text = "G",
                    color = Color(0xFF4285F4),
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.width(12.dp))

                Text("Google로 계속하기", color = LoginInk)
            }

            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "계정이 없으신가요?",
                    color = LoginHint,
                    fontSize = 13.sp
                )

                TextButton(
                    onClick = onSignUpClick,
                    enabled = !isLoading
                ) {
                    Text(
                        "회원가입",
                        color = LoginAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun loginFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = LoginAccent,
    unfocusedBorderColor = LoginLine,
    focusedLabelColor = LoginAccent,
    unfocusedLabelColor = LoginInk,
    focusedTextColor = LoginInk,
    unfocusedTextColor = LoginInk,
    cursorColor = LoginAccent
)

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun LoginScreenPreview() {
    MaterialTheme {
        LoginScreen(
            uiState = LoginUiState(),
            onEmailChange = {},
            onPasswordChange = {},
            onLoginClick = {},
        )
    }
}
