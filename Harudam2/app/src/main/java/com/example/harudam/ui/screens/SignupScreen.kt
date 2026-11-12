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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Patterns
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
) {

    Surface(modifier = Modifier.fillMaxSize(), color = AuthBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp, bottomStart = 14.dp, bottomEnd = 14.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TextButton(
                        onClick = onBackClick,
                        modifier = Modifier.align(Alignment.Start),
                    ) {
                        Text("‹  뒤로", color = AuthMuted, fontSize = 14.sp)
                    }

                    Text(
                        text = "회원가입",
                        color = AuthBlue,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(24.dp))

                    Spacer(Modifier.height(12.dp))

                    AuthInputField(
                        label = "Email",
                        hint = "이메일 주소를 입력하세요",
                        value = uiState.email,
                        onValueChange = onEmailChange,
                        errorMessage = uiState.emailError,
                    )

                    Spacer(Modifier.height(12.dp))

                    AuthInputField(
                        label = "Password",
                        hint = "비밀번호를 입력하세요",
                        value = uiState.password,
                        onValueChange = onPasswordChange,
                        password = true,
                        errorMessage = uiState.passwordError,
                    )


                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = uiState.agreedToPersonalData,
                            onCheckedChange = onAgreementChange,
                            colors = androidx.compose.material3.CheckboxDefaults.colors(
                                checkedColor = AuthBlue
                            ),
                        )
                        Text("개인정보 처리에 동의합니다", color = AuthMuted, fontSize = 12.sp)
                        TextButton(onClick = {}) {
                            Text("보기", color = AuthBlue, fontSize = 12.sp)
                        }
                    }
                    if (uiState.agreementError) {
                        Text(
                            text = "개인정보 처리에 동의해 주세요.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(Modifier.height(9.dp))
                    Button(
                        onClick = onSignUpClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AuthBlue
                        ),
                    ) {
                        Text(
                            "회원가입",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (uiState.isInputValid) {
                        Text(
                            text = "입력값 검증을 통과했습니다. (API 연결 전)",
                            color = AuthBlue,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(Modifier.height(22.dp))
                    AuthSignUpDivider()
                    Spacer(Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = onGoogleSignUpClick,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(13.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(23.dp).background(Color(0xFFF1F4FC), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Spacer(Modifier.size(9.dp))
                            Text("Google 계정으로 가입", color = AuthText, fontSize = 14.sp)
                        }
                    }

                    Spacer(Modifier.height(13.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("이미 계정이 있나요?", color = AuthMuted, fontSize = 12.sp)
                        TextButton(onClick = onLoginClick) {
                            Text("로그인", color = AuthBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
    password: Boolean = false,
    errorMessage: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, fontSize = 12.sp) },
        placeholder = { Text(hint, color = AuthMuted, fontSize = 13.sp) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        isError = errorMessage != null,
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AuthBlue,
            unfocusedBorderColor = AuthBorder,
            focusedLabelColor = AuthBlue,
            unfocusedLabelColor = AuthText,
            focusedTextColor = AuthText,
            unfocusedTextColor = AuthText,
            cursorColor = AuthBlue,
        ),
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
        Box(Modifier.weight(1f).height(1.dp).background(AuthBorder))
        Text("또는", modifier = Modifier.padding(horizontal = 10.dp), color = AuthMuted, fontSize = 12.sp)
        Box(Modifier.weight(1f).height(1.dp).background(AuthBorder))
    }
}

@Preview(showBackground = true)
@Composable
private fun SignUpScreenPreview() {
    MaterialTheme {
        SignUpScreen(
            uiState = SignUpUiState(),
            onEmailChange = {},
            onPasswordChange = {},
            onAgreementChange = {}
        )
    }
}
