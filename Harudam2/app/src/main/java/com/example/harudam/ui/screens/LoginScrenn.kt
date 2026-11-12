package com.example.harudam.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import android.util.Patterns

private val LoginAccent = Color(0xFF4968C8)
private val LoginInk = Color(0xFF34445E)
private val LoginHint = Color(0xFF9AA3AE)
private val LoginLine = Color(0xFFE1E5EA)
private val LoginCanvas = Color(0xFFF5F4FA)

@Composable
fun LoginScreen(
    onLoginClick: () -> Unit = {},
    onGoogleLoginClick: () -> Unit = {},
    onSignUpClick: () -> Unit = {},
) {
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val emailError = remember { mutableStateOf<String?>(null) }
    val passwordError = remember { mutableStateOf<String?>(null) }
    val validationPassed = remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = LoginCanvas) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "HARUDAM",
                        color = LoginAccent,
                        fontSize = 35.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(7.dp))
                    Text("여행 기록을 이어서 계획해 보세요.", color = LoginHint, fontSize = 13.sp)
                    Spacer(Modifier.height(70.dp))


                    OutlinedTextField(
                        value = email.value,
                        onValueChange = {
                            email.value = it
                            emailError.value = null
                            validationPassed.value = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email", fontSize = 12.sp) },
                        placeholder = {
                            Text("이메일 주소", color = LoginHint, fontSize = 13.sp)
                        },
                        singleLine = true,
                        isError = emailError.value != null,
                        shape = RoundedCornerShape(14.dp),
                        colors = authFieldColors(),
                    )

// 이메일 오류 메시지
                    emailError.value?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, top = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(13.dp))
                    OutlinedTextField(
                        value = password.value,
                        onValueChange = {
                            password.value = it
                            passwordError.value = null
                            validationPassed.value = false },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password", fontSize = 12.sp) },
                        placeholder = { Text("비밀번호", color = LoginHint, fontSize = 13.sp) },
                        isError = passwordError.value != null,
                        singleLine = true,
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible }
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) {
                                        Icons.Filled.Visibility
                                    } else {
                                        Icons.Filled.VisibilityOff
                                    },
                                    contentDescription = if (passwordVisible) {
                                        "비밀번호 숨기기"
                                    } else {
                                        "비밀번호 보이기"
                                    },
                                    tint = LoginHint
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = authFieldColors(),
                    )
                    passwordError.value?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, top = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(22.dp))
                    Button(

                        onClick = {
                            val inputEmail = email.value.trim()
                            val inputPassword = password.value

                            // 1. 이메일 검증
                            emailError.value = when {
                                inputEmail.isEmpty() ->
                                    "이메일을 입력해 주세요."

                                !Patterns.EMAIL_ADDRESS.matcher(inputEmail).matches() ->
                                    "올바른 이메일 형식이 아닙니다."

                                else -> null
                            }

                            // 2. 비밀번호 검증
                            passwordError.value = when {
                                inputPassword.isBlank() ->
                                    "비밀번호를 입력해 주세요."

                                else -> null
                            }

                            // 3. 전체 검증 결과
                            validationPassed.value =
                                emailError.value == null &&
                                        passwordError.value == null
                        },

                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LoginAccent),
                    ) {
                        Text("로그인", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (validationPassed.value) {
                        Text(
                            text = "입력값 검증을 통과했습니다. (API 연결 전)",
                            color = LoginAccent,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(Modifier.height(23.dp))
                    AuthDivider(label = "또는 Google 계정으로 로그인")
                    Spacer(Modifier.height(17.dp))
                    OutlinedButton(
                        onClick = onGoogleLoginClick,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(13.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Box(
                                modifier = Modifier.size(24.dp).background(Color(0xFFF1F4FC), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Spacer(Modifier.size(10.dp))
                            Text("Google로 계속하기", color = LoginInk, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("계정이 없으신가요?", color = LoginHint, fontSize = 13.sp)
                        TextButton(onClick = onSignUpClick) {
                            Text("회원가입", color = LoginAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

        }
    }
}

@Composable
private fun AuthDivider(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(LoginLine))
        Text(label, modifier = Modifier.padding(horizontal = 10.dp), color = LoginHint, fontSize = 12.sp)
        Box(Modifier.weight(1f).height(1.dp).background(LoginLine))
    }
}

@Composable
private fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = LoginAccent,
    unfocusedBorderColor = LoginLine,
    focusedLabelColor = LoginAccent,
    unfocusedLabelColor = LoginInk,
    focusedTextColor = LoginInk,
    unfocusedTextColor = LoginInk,
    cursorColor = LoginAccent,
)

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    MaterialTheme { LoginScreen() }
}
