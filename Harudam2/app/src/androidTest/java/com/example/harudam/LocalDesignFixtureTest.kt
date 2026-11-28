package com.example.harudam

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.example.harudam.repository.TripRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Local-only fixture matching FE preview cards; refuses to run against production services. */
@RunWith(AndroidJUnit4::class)
class LocalDesignFixtureTest {
    @Test
    fun createFeReferenceTripsInLocalEmulators() {
        assertTrue("Use -PuseFirebaseEmulators=true for this local fixture", BuildConfig.DEBUG && BuildConfig.USE_FIREBASE_EMULATORS)
        FirebaseEnvironment.configure()
        assertEquals("demo-harudam", com.google.firebase.FirebaseApp.getInstance().options.projectId)
        val email = "design-${UUID.randomUUID()}@example.test"
        val auth = FirebaseAuth.getInstance()
        Tasks.await(auth.createUserWithEmailAndPassword(email, "LocalFixture123!"), 30, TimeUnit.SECONDS)
        assertNotNull(auth.currentUser)
        val api = TripRepository()
        fun call(data: Map<String, Any?>): Map<*, *> =
            runBlocking { api.call(data) }

        data class Reference(val title: String, val start: String, val end: String, val count: Int)
        val references = listOf(
            Reference("부산여행", "2026-09-01", "2026-09-05", 25),
            Reference("강릉 가족여행", "2026-03-21", "2026-03-22", 7),
            Reference("대전 당일치기", "2025-12-27", "2025-12-27", 6),
        )
        for (reference in references) {
            val names = listOf("초량밀면", "코이", "광안리해수욕장", "AREA6", "자매국밥")
            val places = List(reference.count) { index -> mapOf(
                "name" to names.getOrElse(index) { "디자인 확인 장소 ${index + 1}" },
                "category" to if(index in listOf(0, 1, 4)) "FOOD" else "TOUR",
            ) }
            val created = call(mapOf("action" to "createTrip", "requestId" to UUID.randomUUID().toString(),
                "title" to reference.title, "startDate" to reference.start, "endDate" to reference.end, "places" to places))
            val tripId = created["tripId"] as String
            var version = (created["version"] as Number).toLong()
            val trip = call(mapOf("action" to "getTrip", "tripId" to tripId))
            assertEquals(reference.count, (trip["places"] as List<*>).size)
            if(reference.title == "부산여행") {
                for ((index, item) in (trip["places"] as List<*>).take(5).withIndex()) {
                    val place = item as Map<*, *>
                    val result = call(mapOf("action" to "setSchedule", "requestId" to UUID.randomUUID().toString(),
                        "tripId" to tripId, "expectedVersion" to version, "placeId" to place["id"],
                        "visitDate" to if(index < 3) "2026-09-01" else "2026-09-02"))
                    version = (result["version"] as Number).toLong()
                }
            }
        }
        val list = call(mapOf("action" to "listTrips"))["trips"] as List<*>
        assertEquals(3, list.size)
        // Auth persists only in this emulator installation, so the app can display the fixture after this test.
    }
}
