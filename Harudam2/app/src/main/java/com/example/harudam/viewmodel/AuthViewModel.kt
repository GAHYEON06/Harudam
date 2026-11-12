// AuthViewModel  회원가입/로그인 입력값과 검증 상태 관리
package com.example.harudam.viewmodel

import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

// 로그인 화면에서 사용할 상태
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isInputValid: Boolean = false
)
// 회원가입 화면에서 사용할 상태
data class SignUpUiState(
    val email: String = "",
    val password: String = "",
    val agreedToPersonalData: Boolean = false,

    val emailError: String? = null,
    val passwordError: String? = null,
    val agreementError: Boolean = false,

    val isInputValid: Boolean = false
)


class AuthViewModel : ViewModel() {

    var loginUiState by mutableStateOf(LoginUiState())
        private set

    // 이메일 변경
    fun onEmailChange(value: String) {
        loginUiState = loginUiState.copy(
            email = value,
            emailError = null,
            isInputValid = false
        )
    }

    // 비밀번호 변경
    fun onPasswordChange(value: String) {
        loginUiState = loginUiState.copy(
            password = value,
            passwordError = null,
            isInputValid = false
        )
    }

    // 로그인 입력값 검증
    fun validateLogin() {
        val email = loginUiState.email.trim()
        val password = loginUiState.password

        val emailError = when {
            email.isEmpty() ->
                "이메일을 입력해 주세요."

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                "올바른 이메일 형식이 아닙니다."

            else -> null
        }

        val passwordError = when {
            password.isBlank() ->
                "비밀번호를 입력해 주세요."

            else -> null
        }

        loginUiState = loginUiState.copy(
            emailError = emailError,
            passwordError = passwordError,
            isInputValid = emailError == null &&
                    passwordError == null
        )
    }

    var signUpUiState by mutableStateOf(SignUpUiState())
        private set

    // 이메일 입력값 변경
    fun onSignUpEmailChange(value: String) {
        signUpUiState = signUpUiState.copy(
            email = value,
            emailError = null,
            isInputValid = false
        )
    }

    // 비밀번호 입력값 변경
    fun onSignUpPasswordChange(value: String) {
        signUpUiState = signUpUiState.copy(
            password = value,
            passwordError = null,
            isInputValid = false
        )
    }

    // 개인정보 동의 상태 변경
    fun onAgreementChange(agreed: Boolean) {
        signUpUiState = signUpUiState.copy(
            agreedToPersonalData = agreed,
            agreementError = false,
            isInputValid = false
        )
    }

    // 회원가입 입력값 검증
    fun validateSignUp() {
        val email = signUpUiState.email.trim()
        val password = signUpUiState.password

        val emailError = when {
            email.isEmpty() ->
                "이메일을 입력해 주세요."

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                "올바른 이메일 형식이 아닙니다."

            else -> null
        }

        val passwordError = when {
            password.isEmpty() ->
                "비밀번호를 입력해 주세요."

            else -> null
        }

        val agreementError =
            !signUpUiState.agreedToPersonalData

        signUpUiState = signUpUiState.copy(
            emailError = emailError,
            passwordError = passwordError,
            agreementError = agreementError,
            isInputValid =
                emailError == null &&
                        passwordError == null &&
                        !agreementError
        )
    }

}
