package com.example.harudam

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun signUp(
        username: String,
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user

                if (user == null) {
                    onError("가입 결과를 확인하지 못했어요. 다시 로그인해 주세요.")
                    return@addOnSuccessListener
                }

                val profile = UserProfileChangeRequest.Builder()
                    .setDisplayName(username)
                    .build()

                user.updateProfile(profile)
                    .addOnSuccessListener {
                        ensureProfile(
                            user = user,
                            onSuccess = {
                                auth.signOut()
                                onSuccess()
                            },
                            onError = {
                                auth.signOut()
                                onError(
                                    "계정은 만들어졌지만 정보 저장에 실패했어요. " +
                                            "로그인하면 다시 저장을 시도해요."
                                )
                            }
                        )
                    }
                    .addOnFailureListener {
                        auth.signOut()
                        onError(
                            "계정은 만들어졌지만 이름 저장에 실패했어요. " +
                                    "로그인 후 사용자 정보 저장을 다시 시도해 주세요."
                        )
                    }
            }
            .addOnFailureListener { onError(errorMessage(it)) }
    }

    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user

                if (user == null) {
                    onError("로그인 결과를 확인하지 못했어요.")
                    return@addOnSuccessListener
                }

                ensureProfile(
                    user = user,
                    onSuccess = onSuccess,
                    onError = {
                        auth.signOut()
                        onError(
                            "사용자 정보를 불러오거나 저장하지 못했어요. " +
                                    "인터넷과 Firestore 규칙을 확인해 주세요."
                        )
                    }
                )
            }
            .addOnFailureListener { onError(errorMessage(it)) }
    }

    private fun ensureProfile(
        user: FirebaseUser,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val ref = db.collection("users").document(user.uid)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(ref)

            if (!snapshot.exists()) {
                val name = user.displayName
                    ?.takeIf { it.isNotBlank() }
                    ?: user.email?.substringBefore("@")
                    ?: "여행자"

                transaction.set(
                    ref,
                    mapOf(
                        "username" to name.take(30),
                        "email" to user.email.orEmpty(),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            true
        }
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun logout() {
        auth.signOut()
    }
    fun isLoggedIn(): Boolean = auth.currentUser != null

    private fun errorMessage(error: Exception): String =
        when (error) {
            is FirebaseAuthUserCollisionException ->
                "이미 가입된 이메일이에요. 로그인해 주세요."

            is FirebaseAuthWeakPasswordException ->
                "비밀번호가 설정된 보안 조건을 충족하지 않아요."

            is FirebaseAuthInvalidCredentialsException,
            is FirebaseAuthInvalidUserException ->
                "이메일과 비밀번호를 확인해 주세요."

            is FirebaseNetworkException ->
                "인터넷 연결을 확인해 주세요."

            else ->
                "요청을 처리하지 못했어요. Firebase 설정을 확인해 주세요."
        }
}