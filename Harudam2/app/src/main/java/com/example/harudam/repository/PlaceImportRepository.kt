package com.example.harudam.repository

import com.example.harudam.model.TravelPlace

data class PlaceImportPreview(
    val sourceUrl: String,
    val status: String,
    val place: TravelPlace?,
    val providerPlaceId: String?,
    val warnings: List<String>,
    val errorCode: String?,
    val errorMessage: String?,
    val requiresConfirmation: Boolean = true
)

data class PlaceImportBatch(val results: List<PlaceImportPreview>, val successCount: Int, val errorCount: Int)

class PlaceImportRepository(private val trips: TripRepository = TripRepository()) {
    suspend fun preview(urls: List<String>): PlaceImportBatch {
        require(urls.size in 1..10 && urls.all { it.isNotBlank() && it.trim().length <= 500 })
        val response = trips.call(mapOf("action" to "parsePlaces", "urls" to urls.map { it.trim() }))
        val results = (response["results"] as List<*>).map { raw ->
            val item = raw as Map<*, *>
            val source = item["sourceUrl"] as String
            val status = item["status"] as String
            val p = item["place"] as? Map<*, *>
            val place = if (status == "success" && p != null) TravelPlace(
                id = "", name = p["name"] as String, address = p["address"] as? String ?: "",
                category = p["category"] as? String ?: "OTHER",
                sourceUrl = p["sourceUrl"] as? String ?: source,
                latitude = (p["latitude"] as? Number)?.toDouble(),
                longitude = (p["longitude"] as? Number)?.toDouble()
            ) else null
            val err = item["error"] as? Map<*, *>
            PlaceImportPreview(source, status, place, item["providerPlaceId"] as? String,
                (item["warnings"] as? List<*>)?.filterIsInstance<String>().orEmpty(),
                err?.get("code") as? String, err?.get("message") as? String)
        }
        return PlaceImportBatch(results, (response["successCount"] as Number).toInt(), (response["errorCount"] as Number).toInt())
    }
}

fun TravelPlace.toAddPlacesInput(): Map<String, Any?> = mapOf(
    "name" to name, "address" to address, "category" to category,
    "sourceUrl" to sourceUrl, "latitude" to latitude, "longitude" to longitude
)
