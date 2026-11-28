package com.example.harudam

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.example.harudam.repository.TripRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class LocalTravelUiTest {
    @get:Rule val compose = createEmptyComposeRule()

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(800) // Wait for the emulator compositor, after Compose semantics are ready.
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val file = java.io.File(instrumentation.targetContext.getExternalFilesDir(null), "$name.png")
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun savedTripOpensScheduleAndRealResult() {
        assertTrue(BuildConfig.DEBUG && BuildConfig.USE_FIREBASE_EMULATORS)
        FirebaseEnvironment.configure()
        org.junit.Assert.assertEquals("demo-harudam", com.google.firebase.FirebaseApp.getInstance().options.projectId)
        val auth = FirebaseAuth.getInstance()
        Tasks.await(auth.createUserWithEmailAndPassword("ui-${UUID.randomUUID()}@example.test", "UiTest123!"), 30, TimeUnit.SECONDS)
        val api = TripRepository()
        fun call(data: Map<String, Any?>) = runBlocking { api.call(data) }
        val created = call(mapOf("action" to "createTrip", "requestId" to UUID.randomUUID().toString(),
            "title" to "저장된 UI 여행", "startDate" to "2026-12-01", "endDate" to "2026-12-02",
            "places" to listOf(mapOf("name" to "실제 저장 장소", "address" to "서울 확인 주소", "category" to "TOUR"))))
        val tripId = created["tripId"] as String
        val detail = call(mapOf("action" to "getTrip", "tripId" to tripId))
        val placeId = ((detail["places"] as List<*>)[0] as Map<*, *>)["id"] as String
        call(mapOf("action" to "setSchedule", "requestId" to UUID.randomUUID().toString(), "tripId" to tripId,
            "expectedVersion" to 1, "placeId" to placeId, "visitDate" to "2026-12-01", "visitTime" to "10:30", "note" to "저장한 메모"))
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(30000) { compose.onAllNodesWithText("저장된 UI 여행").fetchSemanticsNodes().isNotEmpty() }
            screenshot("home")
            compose.onNodeWithText("저장된 UI 여행").performClick()
            compose.waitUntil(30000) { compose.onAllNodesWithText("일정 배정 현황").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("실제 저장 장소").assertExists()
            screenshot("schedule")
            compose.onNodeWithText("일정 확정하기").assertIsEnabled().performClick()
            compose.waitUntil(30000) { compose.onAllNodesWithText("서울 확인 주소").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("저장된 UI 여행").assertExists()
            compose.onNodeWithText("방문 시간: 10:30").assertExists()
            compose.onNodeWithText("저장한 메모").assertExists()
            compose.onNodeWithText("지도에서 장소 열기").assertExists()
            screenshot("result")
        }
    }
}
