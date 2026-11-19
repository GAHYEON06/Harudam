package com.example.harudam.repository

import com.example.harudam.model.TravelTrip
import com.example.harudam.model.toTravelTrip
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TripRepository {
    private val functions = FirebaseFunctions.getInstance("asia-northeast3")
    suspend fun call(data: Map<String, Any?>): Map<*, *> = suspendCancellableCoroutine { continuation ->
        functions.getHttpsCallable("travelApi").call(data)
            .addOnSuccessListener { result ->
                if (continuation.isActive) {
                    val map = result.data as? Map<*, *>
                    if (map == null) continuation.resumeWithException(IllegalStateException("서버 응답 형식을 확인해 주세요."))
                    else continuation.resume(map)
                }
            }
            .addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
    }
    suspend fun listTrips(): List<TravelTrip> {
        val trips = mutableListOf<TravelTrip>()
        var cursor: String? = null
        do {
            val data = mutableMapOf<String, Any?>("action" to "listTrips", "limit" to 50)
            cursor?.let { data["cursor"] = it }
            val page = call(data)
            trips += (page["trips"] as List<*>).map { (it as Map<*, *>).toTravelTrip() }
            cursor = page["nextCursor"] as? String
        } while(cursor != null)
        return trips.sortedByDescending { it.startDate }
    }
    suspend fun getTrip(id: String) = call(mapOf("action" to "getTrip", "tripId" to id)).toTravelTrip()
}
