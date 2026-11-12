package com.example.harudam.ui.screens

import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.harudam.AuthRepository

private val AuthBlue = Color(0xFF4968C8)
private val AuthText = Color(0xFF34445E)
private val AuthMuted = Color(0xFF929BA7)
private val AuthBorder = Color(0xFFE1E5EA)
private val AuthBackground = Color(0xFFF3F1FA)

@Composable
fun SignUpScreen(
    onSignUpClick: () -> Unit = {},
    onGoogleSignUpClick: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
) {
    val repository = remember { AuthRepository() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var agreedToPersonalData by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var agreementError by remember { mutableStateOf(false) }
    var serverError by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    BackHandler {
        if (!loading) {
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
                    modifier = Modifier.padding(
                        horizontal = 22.dp,
                        vertical = 22.dp
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextButton(
                        onClick = onBackClick,
                        enabled = !loading,
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(
                            "‹  뒤로",
                            color = AuthMuted,
                            fontSize = 14.sp
                        )
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
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = null
                            serverError = null
                        },
                        enabled = !loading,
                        errorMessage = emailError
                    )

                    Spacer(Modifier.height(12.dp))

                    AuthInputField(
                        label = "Password",
                        hint = "비밀번호를 입력하세요",
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = null
                            serverError = null
                        },
                        enabled = !loading,
                        password = true,
                        errorMessage = passwordError
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = agreedToPersonalData,
                            enabled = !loading,
                            onCheckedChange = {
                                agreedToPersonalData = it
                                agreementError = false
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = AuthBlue
                            )
                        )

                        Text(
                            "개인정보 처리에 동의합니다",
                            color = AuthMuted,
                            fontSize = 12.sp
                        )

                        // 기존 '보기' 버튼. 약관 화면 연결은 별도 작업.
                        TextButton(
                            enabled = !loading,
                            onClick = {}
                        ) {
                            Text(
                                "보기",
                                color = AuthBlue,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (agreementError) {
                        Text(
                            "개인정보 처리에 동의해 주세요.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }

                    if (serverError != null) {
                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = serverError.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(9.dp))

                    Button(
                        enabled = !loading,
                        onClick = {
                            val inputEmail = email.trim()
                            val inputPassword = password

                            serverError = null

                            emailError = when {
                                inputEmail.isEmpty() ->
                                    "이메일을 입력해 주세요."

                                !Patterns.EMAIL_ADDRESS
                                    .matcher(inputEmail).matches() ->
                                    "올바른 이메일 형식이 아닙니다."

                                else -> null
                            }

                            passwordError = when {
                                inputPassword.isEmpty() ->
                                    "비밀번호를 입력해 주세요."

                                inputPassword.length < 6 ->
                                    "비밀번호는 최소 6자 이상 입력해 주세요."

                                else -> null
                            }

                            agreementError = !agreedToPersonalData

                            if (
                                emailError == null &&
                                passwordError == null &&
                                !agreementError
                            ) {
                                loading = true

                                repository.signUp(
                                    username = inputEmail
                                        .substringBefore("@")
                                        .take(30),
                                    email = inputEmail,
                                    password = inputPassword,
                                    onSuccess = {
                                        loading = false
                                        password = ""
                                        onSignUpClick()
                                    },
                                    onError = {
                                        loading = false
                                        serverError = it
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AuthBlue
                        )
                    ) {
                        Text(
                            text = if (loading) "가입 중…" else "회원가입",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(22.dp))

                    AuthSignUpDivider()

                    Spacer(Modifier.height(14.dp))

                    OutlinedButton(
                        enabled = !loading,
                        onClick = onGoogleSignUpClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(23.dp)
                                    .background(
                                        Color(0xFFF1F4FC),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "G",
                                    color = Color(0xFF4285F4),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            Spacer(Modifier.size(9.dp))

                            Text(
                                "Google 계정으로 가입",
                                color = AuthText,
                                fontSize = 14.sp
                            )
                        }
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
                            enabled = !loading,
                            onClick = onLoginClick
                        ) {
                            Text(
                                "로그인",
                                color = AuthBlue,
                                fontSize = 12.sp,
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
        label = {
            Text(label, fontSize = 12.sp)
        },
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

@Composable
private fun AuthSignUpDivider() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(AuthBorder)
        )

        Text(
            "또는",
            modifier = Modifier.padding(horizontal = 10.dp),
            color = AuthMuted,
            fontSize = 12.sp
        )

        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(AuthBorder)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SignUpScreenPreview() {
    MaterialTheme {
        SignUpScreen()
    }
}