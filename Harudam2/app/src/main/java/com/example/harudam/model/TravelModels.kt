package com.example.harudam.model

import java.time.LocalDate

 data class TravelPlace(
    val id: String, val name: String, val address: String = "", val category: String = "OTHER",
    val sourceUrl: String = "", val latitude: Double? = null, val longitude: Double? = null,
    val visitDate: String? = null, val visitOrder: Int? = null, val note: String = "", val visitTime: String? = null
)

data class TravelTrip(
    val id: String, val title: String, val startDate: String, val endDate: String,
    val version: Long, val placeCount: Int, val placesConfirmed: Boolean,
    val places: List<TravelPlace> = emptyList()
) {
    val dates: List<String> get() = generateSequence(LocalDate.parse(startDate)) { it.plusDays(1) }
        .takeWhile { !it.isAfter(LocalDate.parse(endDate)) }.map { it.toString() }.toList()
    fun placesOn(date: String) = places.filter { it.visitDate == date }.sortedBy { it.visitOrder }
}

internal fun Map<*, *>.toTravelTrip() = TravelTrip(
    id = this["id"] as String, title = this["title"] as String,
    startDate = this["startDate"] as String, endDate = this["endDate"] as String,
    version = (this["version"] as Number).toLong(), placeCount = (this["placeCount"] as Number).toInt(),
    placesConfirmed = this["placesConfirmed"] as? Boolean ?: false,
    places = (this["places"] as? List<*>)?.map { raw ->
        val p = raw as Map<*, *>
        TravelPlace(p["id"] as String, p["name"] as String,
            p["address"] as? String ?: "", p["category"] as? String ?: "OTHER",
            p["sourceUrl"] as? String ?: "", (p["latitude"] as? Number)?.toDouble(),
            (p["longitude"] as? Number)?.toDouble(), p["visitDate"] as? String,
            (p["visitOrder"] as? Number)?.toInt(), p["note"] as? String ?: "", p["visitTime"] as? String)
    } ?: emptyList()
)
