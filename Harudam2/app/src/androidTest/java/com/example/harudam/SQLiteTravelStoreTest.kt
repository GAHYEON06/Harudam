package com.example.harudam

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.harudam.repository.LocalTravelStore
import com.example.harudam.repository.LocalTravelException
import com.google.firebase.functions.FirebaseFunctionsException.Code
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Exercises Android's real SQLite engine, not a mocked persistence implementation. */
@RunWith(AndroidJUnit4::class)
class SQLiteTravelStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dbName = "test-${UUID.randomUUID()}.db"
    private lateinit var store: LocalTravelStore
    private val owner = "owner-a"
    @Before fun open() { store = LocalTravelStore(context, dbName) }
    @After fun cleanup() { store.close(); context.deleteDatabase(dbName) }
    private fun create(extra: Map<String, Any?> = emptyMap()) = mapOf<String, Any?>(
        "action" to "createTrip", "requestId" to UUID.randomUUID().toString(), "title" to "SQLite 여행",
        "startDate" to "2026-12-01", "endDate" to "2026-12-03") + extra
    private fun write(action: String, id: String, version: Long, extra: Map<String, Any?> = emptyMap()) =
        mapOf<String, Any?>("action" to action, "requestId" to UUID.randomUUID().toString(), "tripId" to id, "expectedVersion" to version) + extra
    private fun get(id: String) = store.call(owner, mapOf("action" to "getTrip", "tripId" to id))
    private fun rejected(code: Code, block: () -> Unit) {
        try { block(); fail("Expected $code") } catch (e: LocalTravelException) { assertEquals(code, e.code) }
    }
    @Suppress("UNCHECKED_CAST")
    private fun places(trip: Map<String, Any?>) = trip["places"] as List<Map<String, Any?>>

    @Test fun persistenceCanonicalReplayUpdateAndDelete() {
        val input = create(mapOf("places" to listOf(mapOf("name" to "첫 장소", "latitude" to null, "longitude" to null))))
        val created = store.call(owner, input)
        val id = created["tripId"] as String
        store.close(); store = LocalTravelStore(context, dbName)
        assertEquals("첫 장소", places(get(id))[0]["name"])
        assertNull(places(get(id))[0]["latitude"])
        val replay = store.call(owner, input.entries.reversed().associate { it.toPair() })
        assertEquals(id, replay["tripId"])
        assertEquals(1, (replay["version"] as Number).toInt())
        rejected(Code.ALREADY_EXISTS) { store.call(owner, input + ("title" to "다른 내용")) }
        store.call(owner, write("updateTrip", id, 1, mapOf("title" to "수정한 여행")))
        assertEquals("수정한 여행", get(id)["title"])
        val deletion = write("deleteTrip", id, 2)
        store.call(owner, deletion)
        assertEquals(true, store.call(owner, deletion)["deleted"])
        store.call(owner, input) // Original create receipt must not resurrect the deleted aggregate.
        rejected(Code.NOT_FOUND) { get(id) }
    }

    @Test fun ownersAndRequestNamespacesAreIsolated() {
        rejected(Code.UNAUTHENTICATED) { store.call(null, create()) }
        val input = create(); val id = store.call(owner, input)["tripId"] as String
        rejected(Code.PERMISSION_DENIED) { store.call("owner-b", mapOf("action" to "getTrip", "tripId" to id)) }
        rejected(Code.PERMISSION_DENIED) { store.call("owner-b", write("deleteTrip", id, 1)) }
        assertTrue((store.call("owner-b", mapOf("action" to "listTrips"))["trips"] as List<*>).isEmpty())
        assertNotEquals(id, store.call("owner-b", input)["tripId"])
    }

    @Test fun invalidBatchRollsBackAndFailedRequestCanBeCorrected() {
        val id = store.call(owner, create())["tripId"] as String
        val request = write("addPlaces", id, 1, mapOf("places" to listOf(mapOf("name" to "좋은 장소"), mapOf("name" to ""))))
        rejected(Code.INVALID_ARGUMENT) { store.call(owner, request) }
        assertEquals(0, places(get(id)).size)
        assertEquals(1, (get(id)["version"] as Number).toInt())
        store.call(owner, request + ("places" to listOf(mapOf("name" to "좋은 장소", "sourceUrl" to "https://naver.me/one"))))
        rejected(Code.ALREADY_EXISTS) { store.call(owner, write("addPlaces", id, 2, mapOf("places" to listOf(mapOf("name" to "중복", "sourceUrl" to "https://naver.me/one"))))) }
        assertEquals(1, places(get(id)).size)
        assertEquals(2, (get(id)["version"] as Number).toInt())
    }

    @Test fun schedulesReorderMoveNotesRemoveAndPlaceCrud() {
        val id = store.call(owner, create(mapOf("places" to listOf(mapOf("name" to "장소 A"), mapOf("name" to "장소 B")))))["tripId"] as String
        val ids = places(get(id)).map { it["id"] as String }
        var version = 1L
        fun change(action: String, extra: Map<String, Any?>) {
            store.call(owner, write(action, id, version, extra)); version++
        }
        ids.forEach { change("setSchedule", mapOf("placeId" to it, "visitDate" to "2026-12-01")) }
        change("reorderSchedule", mapOf("visitDate" to "2026-12-01", "placeIds" to ids.reversed()))
        change("setSchedule", mapOf("placeId" to ids[1], "visitDate" to "2026-12-01", "visitTime" to "10:30", "note" to "저장된 메모"))
        var p = places(get(id)).first { it["id"] == ids[1] }
        assertEquals(1, (p["visitOrder"] as Number).toInt()); assertEquals("10:30", p["visitTime"])
        assertEquals("저장된 메모", p["note"])
        rejected(Code.INVALID_ARGUMENT) { store.call(owner, write("setSchedule", id, version, mapOf("placeId" to ids[0], "visitDate" to "2027-01-01"))) }
        rejected(Code.INVALID_ARGUMENT) { store.call(owner, write("reorderSchedule", id, version, mapOf("visitDate" to "2026-12-01", "placeIds" to listOf(ids[0], ids[0])))) }
        rejected(Code.FAILED_PRECONDITION) { store.call(owner, write("updateTrip", id, version, mapOf("startDate" to "2026-12-02"))) }
        change("setSchedule", mapOf("placeId" to ids[1], "visitDate" to "2026-12-02"))
        change("removeSchedule", mapOf("placeId" to ids[1]))
        p = places(get(id)).first { it["id"] == ids[1] }; assertNull(p["visitDate"]); assertNull(p["visitOrder"])
        change("updatePlace", mapOf("placeId" to ids[1], "name" to "수정된 장소"))
        assertEquals("수정된 장소", places(get(id)).first { it["id"] == ids[1] }["name"])
        change("deletePlace", mapOf("placeId" to ids[0]))
        change("confirmPlaces", emptyMap())
        assertEquals(true, get(id)["placesConfirmed"]); assertEquals(1, places(get(id)).size)
    }

    @Test fun concurrentWritersWithSameVersionOnlyOneSucceeds() {
        val id = store.call(owner, create())["tripId"] as String
        val other = LocalTravelStore(context, dbName)
        val executor = Executors.newFixedThreadPool(2)
        val gate = CountDownLatch(1)
        try {
            val futures = listOf(store, other).mapIndexed { index, database -> executor.submit(Callable {
                gate.await()
                try { database.call(owner, write("updateTrip", id, 1, mapOf("title" to "writer-$index"))); "OK" }
                catch (e: LocalTravelException) { e.code.name }
            }) }
            gate.countDown()
            assertEquals(listOf("ABORTED", "OK"), futures.map { it.get(15, TimeUnit.SECONDS) }.sorted())
            assertEquals(2, (get(id)["version"] as Number).toInt())
        } finally { executor.shutdownNow(); other.close() }
    }

    @Test fun paginationAndInputValidationAndManualUrlPreview() {
        rejected(Code.INVALID_ARGUMENT) { store.call(owner, create(mapOf("startDate" to "2026-02-30"))) }
        rejected(Code.INVALID_ARGUMENT) { store.call(owner, create(mapOf("places" to listOf(mapOf("name" to "잘못된 좌표", "latitude" to 0.0))))) }
        repeat(3) { store.call(owner, create()) }
        val page = store.call(owner, mapOf("action" to "listTrips", "limit" to 2))
        assertEquals(2, (page["trips"] as List<*>).size)
        val next = store.call(owner, mapOf("action" to "listTrips", "limit" to 2, "cursor" to page["nextCursor"]))
        assertEquals(1, (next["trips"] as List<*>).size); assertNull(next["nextCursor"])
        val preview = store.call(owner, mapOf("action" to "parsePlaces", "url" to "https://naver.me/test"))
        assertEquals(0, preview["successCount"])
        assertEquals(3, (store.call(owner, mapOf("action" to "listTrips"))["trips"] as List<*>).size)
    }
}
