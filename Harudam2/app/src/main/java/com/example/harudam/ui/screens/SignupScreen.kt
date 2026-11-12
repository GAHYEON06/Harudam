package com.example.harudam.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.harudam.viewmodel.SignUpUiState

private val AuthBlue = Color(0xFF4968C8)
private val AuthText = Color(0xFF34445E)
private val AuthMuted = Color(0xFF929BA7)
private val AuthBorder = Color(0xFFE1E5EA)
private val AuthBackground = Color(0xFFF3F1FA)

@Composable
fun SignUpScreen(
    uiState: SignUpUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onAgreementChange: (Boolean) -> Unit,
    onSignUpClick: () -> Unit = {},
    onGoogleSignUpClick: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    isLoading: Boolean = false,
    message: String? = null,
) {
    BackHandler {
        if (!isLoading) {
            onBackClick()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AuthBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(
                    topStart = 26.dp,
                    topEnd = 26.dp,
                    bottomStart = 14.dp,
                    bottomEnd = 14.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextButton(
                        onClick = onBackClick,
                        enabled = !isLoading,
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text("‹  뒤로", color = AuthMuted)
                    }

                    Text(
                        text = "회원가입",
                        color = AuthBlue,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(36.dp))

                    AuthInputField(
                        label = "Email",
                        hint = "이메일 주소를 입력하세요",
                        value = uiState.email,
                        onValueChange = onEmailChange,
                        enabled = !isLoading,
                        errorMessage = uiState.emailError
                    )

                    Spacer(Modifier.height(12.dp))

                    AuthInputField(
                        label = "Password",
                        hint = "비밀번호를 입력하세요",
                        value = uiState.password,
                        onValueChange = onPasswordChange,
                        enabled = !isLoading,
                        password = true,
                        errorMessage = uiState.passwordError
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = uiState.agreedToPersonalData,
                            onCheckedChange = onAgreementChange,
                            enabled = !isLoading,
                            colors = CheckboxDefaults.colors(
                                checkedColor = AuthBlue
                            )
                        )

                        Text(
                            text = "개인정보 처리에 동의합니다",
                            color = AuthMuted,
                            fontSize = 12.sp
                        )

                        // 약관 내용 화면은 추후 연결
                        TextButton(
                            enabled = !isLoading,
                            onClick = {}
                        ) {
                            Text("보기", color = AuthBlue)
                        }
                    }

                    if (uiState.agreementError) {
                        Text(
                            text = "개인정보 처리에 동의해 주세요.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }

                    if (!message.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(9.dp))

                    Button(
                        onClick = onSignUpClick,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AuthBlue
                        )
                    ) {
                        Text(
                            text = if (isLoading) "가입 중…" else "회원가입",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(22.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.weight(1f).height(1.dp)
                                .background(AuthBorder)
                        )

                        Text(
                            text = "또는",
                            modifier = Modifier.padding(horizontal = 10.dp),
                            color = AuthMuted,
                            fontSize = 12.sp
                        )

                        Box(
                            Modifier.weight(1f).height(1.dp)
                                .background(AuthBorder)
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = onGoogleSignUpClick,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        Text(
                            "G",
                            color = Color(0xFF4285F4),
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.width(12.dp))

                        Text("Google 계정으로 가입", color = AuthText)
                    }

                    Spacer(Modifier.height(13.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "이미 계정이 있나요?",
                            color = AuthMuted,
                            fontSize = 12.sp
                        )

                        TextButton(
                            onClick = onLoginClick,
                            enabled = !isLoading
                        ) {
                            Text(
                                "로그인",
                                color = AuthBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthInputField(
    label: String,
    hint: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    password: Boolean = false,
    errorMessage: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, fontSize = 12.sp) },
        placeholder = {
            Text(hint, color = AuthMuted, fontSize = 13.sp)
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        isError = errorMessage != null,
        visualTransformation =
            if (password) PasswordVisualTransformation()
            else VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AuthBlue,
            unfocusedBorderColor = AuthBorder,
            focusedLabelColor = AuthBlue,
            unfocusedLabelColor = AuthText,
            focusedTextColor = AuthText,
            unfocusedTextColor = AuthText,
            cursorColor = AuthBlue
        )
    )

    if (errorMessage != null) {
        Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.error,
            fontSize = 12.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 4.dp)
        )
    }
}