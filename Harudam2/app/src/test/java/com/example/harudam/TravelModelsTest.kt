package com.example.harudam

import com.example.harudam.model.toTravelTrip
import org.junit.Assert.*
import org.junit.Test

class TravelModelsTest {
    private fun data(places: List<Map<String, Any?>> = emptyList()) = mapOf(
        "id" to "trip-1", "title" to "부산 여행", "startDate" to "2026-12-01", "endDate" to "2026-12-03",
        "version" to 3.0, "placeCount" to places.size, "placesConfirmed" to true, "places" to places
    )
    @Test fun callableNumbersAndAllTripDatesAreParsed() {
        val trip = data().toTravelTrip()
        assertEquals(3L, trip.version)
        assertEquals(listOf("2026-12-01", "2026-12-02", "2026-12-03"), trip.dates)
    }
    @Test fun assignedPlacesAreSortedAndUnassignedExcluded() {
        fun place(id:String, order:Int?, date:String?) = mapOf("id" to id, "name" to id,
            "visitDate" to date, "visitOrder" to order, "visitTime" to "12:30", "note" to "점심")
        val trip = data(listOf(place("second",2,"2026-12-01"),place("unassigned",null,null),place("first",1,"2026-12-01"))).toTravelTrip()
        assertEquals(listOf("first","second"),trip.placesOn("2026-12-01").map{it.id})
        assertEquals("12:30",trip.placesOn("2026-12-01").first().visitTime)
        assertTrue(trip.placesOn("2026-12-02").isEmpty())
    }
    @Test fun summaryWithoutPlaceArrayCanStillBeShown() {
        val trip = (data() - "places").toTravelTrip()
        assertTrue(trip.places.isEmpty())
        assertEquals("부산 여행", trip.title)
    }
}
