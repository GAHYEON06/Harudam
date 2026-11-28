package com.example.harudam.repository

import com.example.harudam.model.TravelTrip
import com.example.harudam.model.toTravelTrip
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.example.harudam.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TripRepository {
    companion object {
        @Volatile private var database: LocalTravelStore? = null
        private fun store(): LocalTravelStore = database ?: synchronized(this) {
            database ?: LocalTravelStore(FirebaseApp.getInstance().applicationContext,
                if (BuildConfig.DEBUG && BuildConfig.USE_FIREBASE_EMULATORS) "harudam-demo.db" else "harudam.db")
                .also { database = it }
        }
    }
    suspend fun call(data: Map<String, Any?>): Map<*, *> {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        return withContext(Dispatchers.IO) { store().call(uid, data) }
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
