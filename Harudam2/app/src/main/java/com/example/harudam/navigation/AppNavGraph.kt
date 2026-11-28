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
import com.example.harudam.ui.screens.*
import com.example.harudam.model.TravelPlace
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.window.Dialog
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import com.example.harudam.viewmodel.TravelViewModel
import com.google.firebase.auth.FirebaseAuth
import com.example.harudam.ui.screens.LoginScreen
import com.example.harudam.ui.screens.SignUpScreen
import com.example.harudam.viewmodel.AuthViewModel

private object Routes {
    const val LOGIN = "login"
    const val SIGN_UP = "signup"
    const val HOME = "home"
    const val CREATE_TRIP = "create_trip"
    const val TRIP_SCHEDULE = "trip_schedule"
    const val RESULT = "result"
    const val MANAGE = "manage"
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val repository = remember { AuthRepository() }
    val travelViewModel: TravelViewModel = viewModel()

    var deletingTrip by remember { mutableStateOf<String?>(null) }
    var addingDate by remember { mutableStateOf<String?>(null) }
    var editingPlace by remember { mutableStateOf<TravelPlace?>(null) }
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
    BackHandler(enabled = loading || travelViewModel.busy) {}

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

                onResetPasswordClick = {
                    val email = authViewModel.loginUiState.email.trim()
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) message = "재설정할 이메일을 먼저 입력해 주세요."
                    else if (!loading) {
                        loading = true
                        repository.resetPassword(email, {
                            loading = false; message = "입력한 이메일의 비밀번호 재설정 안내를 확인해 주세요."
                        }, { loading = false; message = it })
                    }
                },
                onGoogleLoginClick = signInGoogle

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
            LaunchedEffect(Unit) { travelViewModel.loadTrips() }
            HomeScreen(
                serverTrips = travelViewModel.trips.map { trip ->
                    val days = ChronoUnit.DAYS.between(LocalDate.parse(trip.startDate), LocalDate.parse(trip.endDate)) + 1
                    val start = trip.startDate.replace('-', '.')
                    val end = (if (trip.startDate.take(4) == trip.endDate.take(4)) trip.endDate.substring(5) else trip.endDate).replace('-', '.')
                    HomeTripUi(if (trip.startDate == trip.endDate) start else "$start ~ $end", trip.title,
                        (if(days == 1L) "1일" else "${days-1}박 ${days}일") + "  |  장소 ${trip.placeCount}개", trip.id)
                },
                onAddTripClick = { if(!travelViewModel.busy) navController.navigate(Routes.CREATE_TRIP) },
                onOpenTrip = { id -> travelViewModel.openTrip(id) { navController.navigate(Routes.TRIP_SCHEDULE) } },
                onDeleteTrip = { id -> travelViewModel.openTrip(id) { deletingTrip = travelViewModel.selected?.title } },
                onLogoutClick = {
                    if(!travelViewModel.busy) {
                        travelViewModel.reset()
                        repository.logout()
                        message = null
                        authViewModel.onEmailChange("")
                        authViewModel.onPasswordChange("")
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.HOME) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
            )
        }
        composable(Routes.CREATE_TRIP) {
            TripInfoInputScreen(
                onBackClick = { if(!travelViewModel.busy) navController.popBackStack() },
                onSaveTrip = { title, start, end, places ->
                    val data = places.map { p -> mapOf<String, Any?>(
                        "name" to p.name, "category" to when(p.category) { "음식·카페" -> "FOOD"; "관광" -> "TOUR"; "숙소" -> "STAY"; else -> "OTHER" },
                        "sourceUrl" to p.sourceUrl, "address" to p.address,
                        "latitude" to if(p.hasCoordinates) p.latitude else null,
                        "longitude" to if(p.hasCoordinates) p.longitude else null
                    ) }
                    travelViewModel.createPlan(title, start, end, data) {
                        navController.navigate(Routes.TRIP_SCHEDULE) {
                            popUpTo(Routes.CREATE_TRIP) { inclusive = true }
                        }
                    }
                },
                onParseUrl = { url -> travelViewModel.parsePlaceUrl(url) },
            )
        }
        composable(Routes.RESULT) {
            travelViewModel.selected?.let { trip -> ResultScreen(trip = trip, onBackClick = { navController.popBackStack() },
                onPlaceEdit = { id -> editingPlace = trip.places.find { it.id == id } }) }
        }
        composable(Routes.MANAGE) {
            travelViewModel.selected?.let { trip ->
                TripManagementScreen(trip, !travelViewModel.busy && !travelViewModel.retryAvailable, travelViewModel,
                    onBack = { navController.popBackStack() })
            }
        }
        composable(Routes.TRIP_SCHEDULE) {
            if (travelViewModel.selected == null) LaunchedEffect(Unit) {
                navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
            }
            travelViewModel.selected?.let { trip ->
                TripScheduleScreen(
                    trip = trip, enabled = !travelViewModel.busy && !travelViewModel.retryAvailable,
                    onBackClick = { if(!travelViewModel.busy) navController.popBackStack() },
                    onAssign = { id, date -> travelViewModel.change("setSchedule", mapOf("placeId" to id, "visitDate" to date)) },
                    onRemove = { id -> travelViewModel.change("removeSchedule", mapOf("placeId" to id)) },
                    onConfirmClick = {
                        if (trip.placesConfirmed) travelViewModel.openTrip(trip.id) { navController.navigate(Routes.RESULT) }
                        else travelViewModel.change("confirmPlaces") { navController.navigate(Routes.RESULT) }
                    },
                    onManage = { navController.navigate(Routes.MANAGE) },
                    onReorder = { date, ids -> travelViewModel.change("reorderSchedule", mapOf("visitDate" to date, "placeIds" to ids)) },
                )
            }
        }
    }
    if(travelViewModel.busy) Dialog(onDismissRequest = {}) { CircularProgressIndicator(color = androidx.compose.ui.graphics.Color(0xFFA0E8CB)) }
    deletingTrip?.let { title ->
        AlertDialog(onDismissRequest = { deletingTrip=null },title={Text("여행을 삭제할까요?")},text={Text(title)},
            confirmButton={TextButton(enabled=!travelViewModel.busy,onClick={travelViewModel.change("deleteTrip"){deletingTrip=null}}){Text("삭제")}},
            dismissButton={TextButton(onClick={deletingTrip=null}){Text("취소")}})
    }
    addingDate?.let { date ->
        val trip=travelViewModel.selected
        AlertDialog(onDismissRequest={addingDate=null},title={Text("$date 장소 선택")},text={
            Column {
                val candidates=trip?.places?.filter{it.visitDate != date}.orEmpty()
                if(candidates.isEmpty()) Text("다른 날짜나 미배치 상태의 장소가 없어요.")
                candidates.forEach { place ->
                    TextButton(enabled=!travelViewModel.busy,onClick={travelViewModel.change("setSchedule",mapOf("placeId" to place.id,"visitDate" to date)){addingDate=null}}){Text(place.name)}
                }
            }
        },confirmButton={TextButton(onClick={addingDate=null}){Text("닫기")}})
    }
    editingPlace?.let { place -> travelViewModel.selected?.let { trip ->
        GreenScheduleEditor(place, trip, !travelViewModel.busy && !travelViewModel.retryAvailable, { editingPlace=null },
            onSave={date,time,note->travelViewModel.change("setSchedule",mapOf("placeId" to place.id,"visitDate" to date,"visitTime" to time,"note" to note)){editingPlace=null}},
            onRemove={travelViewModel.change("removeSchedule",mapOf("placeId" to place.id)){editingPlace=null}})
    } }
    travelViewModel.message?.let { text ->
        AlertDialog(onDismissRequest = { if(!travelViewModel.retryAvailable) travelViewModel.clearMessage() },
            title = { Text("안내") }, text = { Text(text) },
            confirmButton = { TextButton(onClick = { if(travelViewModel.retryAvailable) travelViewModel.retry() else travelViewModel.clearMessage() }) { Text(if(travelViewModel.retryAvailable) "같은 요청 재시도" else "확인") } },
            dismissButton = { if(travelViewModel.retryAvailable) TextButton(onClick = { travelViewModel.discardRetry(); travelViewModel.refresh() }) { Text("최신 내용 확인") } })
    }

}
