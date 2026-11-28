package com.example.harudam.repository

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.firebase.functions.FirebaseFunctionsException.Code
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

class LocalTravelException(val code: Code, message: String) : Exception(message)

/** App-private SQLite storage. A trip aggregate and its request receipt commit atomically.
 * Ownership here is local account separation, not a replacement for server authorization.
 */
class LocalTravelStore(context: Context, name: String = "harudam.db") :
    SQLiteOpenHelper(context.applicationContext, name, null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE trips (id TEXT PRIMARY KEY, owner_id TEXT NOT NULL, payload TEXT NOT NULL)")
        db.execSQL("CREATE INDEX trips_owner ON trips(owner_id, id)")
        // Keep receipts after deletion too: replay must never resurrect a deleted trip.
        db.execSQL("CREATE TABLE requests (owner_id TEXT NOT NULL, request_id TEXT NOT NULL, hash TEXT NOT NULL, result TEXT NOT NULL, PRIMARY KEY(owner_id, request_id))")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Never drop user trips as an implicit migration.
        error("지원하지 않는 데이터베이스 버전이에요: $oldVersion → $newVersion")
    }

    private fun fail(code: Code, message: String): Nothing = throw LocalTravelException(code, message)
    private fun checkInput(valid: Boolean, message: String) { if (!valid) fail(Code.INVALID_ARGUMENT, message) }
    private fun text(value: Any?, label: String, max: Int, optional: Boolean = false): String {
        if (optional && value == null) return ""
        checkInput(value is String, "$label 형식을 확인해 주세요.")
        val result = (value as String).trim()
        checkInput(result.length <= max && (optional || result.isNotEmpty()), "$label 길이를 확인해 주세요. (최대 ${max}자)")
        return result
    }
    private fun date(value: Any?): String {
        checkInput(value is String && Regex("\\d{4}-\\d{2}-\\d{2}").matches(value), "날짜는 YYYY-MM-DD 형식으로 입력해 주세요.")
        val input = value as String
        checkInput(runCatching { LocalDate.parse(input).toString() == input }.getOrDefault(false), "실제 날짜를 입력해 주세요.")
        return input
    }
    private fun period(start: String, end: String) {
        checkInput(start <= end && ChronoUnit.DAYS.between(LocalDate.parse(start), LocalDate.parse(end)) <= 365, "여행 기간은 시작일부터 최대 366일이에요.")
    }
    private fun coordinate(value: Any?, min: Double, max: Double): Double? {
        if (value == null) return null
        checkInput(value is Number && value.toDouble().isFinite() && value.toDouble() in min..max, "좌표 범위를 확인해 주세요.")
        return (value as Number).toDouble()
    }
    private fun place(data: Map<*, *>): MutableMap<String, Any?> {
        val lat = coordinate(data["latitude"], -90.0, 90.0)
        val lon = coordinate(data["longitude"], -180.0, 180.0)
        checkInput((lat == null) == (lon == null), "위도와 경도는 함께 입력해 주세요.")
        val category = data["category"] ?: "OTHER"
        checkInput(category in listOf("FOOD", "TOUR", "STAY", "OTHER"), "장소 분류를 확인해 주세요.")
        val url = text(data["sourceUrl"], "장소 링크", 500, true)
        checkInput(url.isBlank() || runCatching {
            val uri = java.net.URI(url)
            uri.scheme in listOf("https", "http") && !uri.host.isNullOrBlank() && uri.rawUserInfo == null
        }.getOrDefault(false), "올바른 http 또는 https 장소 링크를 입력해 주세요.")
        return mutableMapOf("name" to text(data["name"], "장소 이름", 100),
            "address" to text(data["address"], "주소", 300, true), "category" to category,
            "sourceUrl" to url, "latitude" to lat, "longitude" to lon)
    }
    private fun newPlace(data: Map<*, *>) = place(data).apply {
        putAll(mapOf("id" to UUID.randomUUID().toString(), "visitDate" to null, "visitOrder" to null, "visitTime" to null, "note" to ""))
    }
    private fun compact(places: List<MutableMap<String, Any?>>) {
        places.filter { it["visitDate"] != null }.groupBy { it["visitDate"] }.values.forEach { group ->
            group.sortedBy { (it["visitOrder"] as? Number)?.toInt() ?: Int.MAX_VALUE }
                .forEachIndexed { index, p -> p["visitOrder"] = index + 1 }
        }
    }
    private fun readTrip(db: SQLiteDatabase, uid: String, id: String): MutableMap<String, Any?> {
        db.rawQuery("SELECT owner_id,payload FROM trips WHERE id=?", arrayOf(id)).use { cursor ->
            if (!cursor.moveToFirst()) fail(Code.NOT_FOUND, "이 기기에 저장된 여행이 없어요.")
            if (cursor.getString(0) != uid) fail(Code.PERMISSION_DENIED, "이 여행에 접근할 권한이 없어요.")
            return decode(JSONObject(cursor.getString(1))) as MutableMap<String, Any?>
        }
    }

    /** Call from a background dispatcher. SQL transactions serialize competing helper instances. */
    @Suppress("UNCHECKED_CAST")
    fun call(uid: String?, data: Map<String, Any?>): Map<String, Any?> {
        if (uid.isNullOrBlank()) fail(Code.UNAUTHENTICATED, "로그인이 필요해요.")
        val action = text(data["action"], "작업 이름", 40)
        val db = writableDatabase
        if (action == "parsePlaces") {
            // SQLite stores URLs, but cannot fetch provider metadata. Never invent a place.
            val urls = if (data["urls"] is List<*>) data["urls"] as List<*> else listOf(data["url"])
            checkInput(urls.size in 1..10 && urls.all { it is String && it.isNotBlank() && it.length <= 500 }, "장소 링크를 1~10개 입력해 주세요.")
            return mapOf("successCount" to 0, "errorCount" to urls.size, "results" to urls.map {
                mapOf("sourceUrl" to it, "status" to "error", "requiresConfirmation" to true,
                    "error" to mapOf("code" to "LOCAL_MANUAL_ENTRY_REQUIRED", "message" to "기기 저장 모드에서는 장소 이름과 주소를 직접 입력해 주세요. 링크는 함께 저장돼요."))
            })
        }
        if (action == "listTrips") {
            val limit = data["limit"] ?: 20
            checkInput(limit is Number && limit.toDouble() == limit.toInt().toDouble() && limit.toInt() in 1..50, "목록 크기는 1~50 사이예요.")
            val count = (limit as Number).toInt()
            val rows = mutableListOf<Map<String, Any?>>()
            db.rawQuery("SELECT payload FROM trips WHERE owner_id=? AND id>? ORDER BY id LIMIT ?",
                arrayOf(uid, data["cursor"] as? String ?: "", (count + 1).toString())).use { c ->
                while (c.moveToNext()) rows += decode(JSONObject(c.getString(0))) as Map<String, Any?>
            }
            return mapOf("trips" to rows.take(count), "nextCursor" to if (rows.size > count) rows[count - 1]["id"] else null)
        }
        if (action == "getTrip") return readTrip(db, uid, text(data["tripId"], "여행 ID", 128))
        checkInput(action in setOf("createTrip", "updateTrip", "deleteTrip", "addPlace", "addPlaces", "updatePlace", "deletePlace", "confirmPlaces", "setSchedule", "removeSchedule", "reorderSchedule"), "지원하지 않는 작업이에요.")
        val requestId = text(data["requestId"], "요청 ID", 100)
        val hash = MessageDigest.getInstance("SHA-256").digest(encode(data).toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        db.beginTransaction()
        try {
            db.rawQuery("SELECT hash,result FROM requests WHERE owner_id=? AND request_id=?", arrayOf(uid, requestId)).use { c ->
                if (c.moveToFirst()) {
                    if (c.getString(0) != hash) fail(Code.ALREADY_EXISTS, "같은 요청 ID를 다른 내용에 사용할 수 없어요.")
                    val result = decode(JSONObject(c.getString(1))) as Map<String, Any?>
                    db.setTransactionSuccessful()
                    return result
                }
            }
            val id = if (action == "createTrip") UUID.randomUUID().toString() else text(data["tripId"], "여행 ID", 128)
            val trip = if (action == "createTrip") {
                val start = date(data["startDate"]); val end = date(data["endDate"]); period(start, end)
                val initial = data["places"] ?: emptyList<Any>()
                checkInput(initial is List<*> && initial.size <= 100 && initial.all { it is Map<*, *> }, "여행 장소는 최대 100개예요.")
                val places = (initial as List<Map<*, *>>).map { newPlace(it) }.toMutableList()
                mutableMapOf<String, Any?>("id" to id, "title" to text(data["title"], "여행 이름", 60), "ownerId" to uid,
                    "startDate" to start, "endDate" to end, "version" to 1L, "createdAt" to System.currentTimeMillis(),
                    "placesConfirmed" to places.isNotEmpty(), "places" to places)
            } else readTrip(db, uid, id)
            val places = trip["places"] as MutableList<MutableMap<String, Any?>>
            var result = mutableMapOf<String, Any?>()
            if (action != "createTrip") {
                val expected = data["expectedVersion"]
                checkInput(expected is Number && expected.toDouble() == expected.toLong().toDouble() && expected.toLong() >= 1, "여행 버전을 확인해 주세요.")
                if ((expected as Number).toLong() != (trip["version"] as Number).toLong()) fail(Code.ABORTED, "다른 수정이 반영됐어요. 최신 여행을 다시 확인해 주세요.")
                fun findPlace(): MutableMap<String, Any?> = places.firstOrNull { it["id"] == data["placeId"] }
                    ?: fail(Code.NOT_FOUND, "장소를 찾지 못했어요.")
                fun visitDate(): String = date(data["visitDate"]).also {
                    checkInput(it >= trip["startDate"] as String && it <= trip["endDate"] as String, "여행 기간 안의 날짜를 선택해 주세요.")
                }
                when (action) {
                    "updateTrip" -> {
                        val start = date(data["startDate"] ?: trip["startDate"]); val end = date(data["endDate"] ?: trip["endDate"]); period(start, end)
                        if (places.any { (it["visitDate"] as? String)?.let { d -> d < start || d > end } == true }) fail(Code.FAILED_PRECONDITION, "새 여행 기간 밖의 일정을 먼저 옮겨 주세요.")
                        trip["title"] = text(data["title"] ?: trip["title"], "여행 이름", 60)
                        trip["startDate"] = start; trip["endDate"] = end
                    }
                    "addPlace", "addPlaces" -> {
                        val entries = if (action == "addPlace") listOf(data) else data["places"]
                        checkInput(entries is List<*> && entries.size in 1..10 && entries.all { it is Map<*, *> }, "장소를 1~10개 입력해 주세요.")
                        if (places.size + (entries as List<*>).size > 100) fail(Code.RESOURCE_EXHAUSTED, "여행당 장소는 최대 100개예요.")
                        val additions = (entries as List<Map<*, *>>).map { newPlace(it) }
                        val seen = places.mapNotNull { (it["sourceUrl"] as? String)?.takeIf(String::isNotBlank) }.toMutableSet()
                        additions.forEach { p -> val url = p["sourceUrl"] as String
                            if (url.isNotBlank() && !seen.add(url)) fail(Code.ALREADY_EXISTS, "이미 담긴 장소 링크가 포함돼 있어요.") }
                        places.addAll(additions); trip["placesConfirmed"] = false
                        if (action == "addPlace") result["placeId"] = additions.first()["id"] else result["placeIds"] = additions.map { it["id"] }
                    }
                    "updatePlace" -> { val p = findPlace(); p.putAll(place(p + data)); trip["placesConfirmed"] = false }
                    "deletePlace" -> { places.remove(findPlace()); compact(places); trip["placesConfirmed"] = false }
                    "confirmPlaces" -> {
                        if (places.isEmpty()) fail(Code.FAILED_PRECONDITION, "장소를 한 개 이상 추가해 주세요.")
                        trip["placesConfirmed"] = true
                    }
                    "setSchedule" -> {
                        val p = findPlace(); val day = visitDate()
                        val time = if (data.containsKey("visitTime")) data["visitTime"] else p["visitTime"]
                        checkInput(time == null || time is String && Regex("([01]\\d|2[0-3]):[0-5]\\d").matches(time), "시간은 HH:mm 형식으로 입력해 주세요.")
                        val note = text(if (data.containsKey("note")) data["note"] else p["note"], "메모", 1000, true)
                        if (p["visitDate"] != day) {
                            p["visitDate"] = null; p["visitOrder"] = null; compact(places)
                            p["visitOrder"] = places.count { it["visitDate"] == day } + 1; p["visitDate"] = day
                        }
                        p["visitTime"] = time; p["note"] = note
                    }
                    "removeSchedule" -> { findPlace().apply { this["visitDate"] = null; this["visitOrder"] = null }; compact(places) }
                    "reorderSchedule" -> {
                        val day = visitDate(); val ids = data["placeIds"]
                        val current = places.filter { it["visitDate"] == day }
                        checkInput(ids is List<*> && ids.size == current.size && ids.toSet().size == ids.size && ids.toSet() == current.map { it["id"] }.toSet(), "해당 날짜의 장소를 중복 없이 모두 지정해 주세요.")
                        (ids as List<*>).forEachIndexed { index, placeId -> current.first { it["id"] == placeId }["visitOrder"] = index + 1 }
                    }
                }
                trip["version"] = (trip["version"] as Number).toLong() + 1
            }
            trip["placeCount"] = places.size; trip["updatedAt"] = System.currentTimeMillis()
            result.putAll(mapOf("tripId" to id, "version" to trip["version"]))
            if (action == "deleteTrip") {
                db.delete("trips", "id=? AND owner_id=?", arrayOf(id, uid)); result["deleted"] = true
            } else {
                val values = ContentValues().apply { put("id", id); put("owner_id", uid); put("payload", encode(trip)) }
                db.insertWithOnConflict("trips", null, values, SQLiteDatabase.CONFLICT_REPLACE).also { if (it == -1L) error("여행 저장 실패") }
            }
            db.insertOrThrow("requests", null, ContentValues().apply {
                put("owner_id", uid); put("request_id", requestId); put("hash", hash); put("result", encode(result))
            })
            db.setTransactionSuccessful()
            return result
        } finally { db.endTransaction() }
    }

    private fun encode(value: Any?): String = when (value) {
        null -> "null"
        is Map<*, *> -> value.entries.sortedBy { it.key.toString() }.joinToString(",", "{", "}") { JSONObject.quote(it.key.toString()) + ":" + encode(it.value) }
        is List<*> -> value.joinToString(",", "[", "]") { encode(it) }
        is String -> JSONObject.quote(value)
        is Number -> { checkInput(value.toDouble().isFinite(), "유효한 숫자를 입력해 주세요."); value.toString() }
        is Boolean -> value.toString()
        else -> fail(Code.INVALID_ARGUMENT, "저장할 수 없는 데이터 형식이에요.")
    }
    private fun decode(value: Any?): Any? = when (value) {
        JSONObject.NULL, null -> null
        is JSONObject -> value.keys().asSequence().associateWith { decode(value.get(it)) }.toMutableMap()
        is JSONArray -> (0 until value.length()).map { decode(value.get(it)) }.toMutableList()
        else -> value
    }
}
