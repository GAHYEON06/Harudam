package com.example.harudam.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.harudam.repository.PlaceImportBatch
import com.example.harudam.repository.PlaceImportRepository
import com.example.harudam.repository.toAddPlacesInput
import com.example.harudam.model.TravelTrip
import com.example.harudam.model.TravelPlace
import com.example.harudam.repository.TripRepository
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.UUID

class TravelViewModel : ViewModel() {
    private val repository = TripRepository()
    private val importer = PlaceImportRepository(repository)
    var trips by mutableStateOf<List<TravelTrip>>(emptyList()); private set
    var selected by mutableStateOf<TravelTrip?>(null); private set
    var busy by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null); private set
    var retryAvailable by mutableStateOf(false); private set
    private var pending: Map<String, Any?>? = null
    private var pendingSuccess: (() -> Unit)? = null

    fun clearMessage() { message = null }
    fun discardRetry() { pending = null; pendingSuccess = null; retryAvailable = false; message = null }
    fun reset() { trips = emptyList(); selected = null; discardRetry() }
    fun loadTrips() = runRead { trips = repository.listTrips() }
    suspend fun parsePlaceUrl(url: String): TravelPlace {
        try {
            val response = repository.call(mapOf("action" to "parsePlaces", "url" to url))
            val result = (response["results"] as? List<*>)?.firstOrNull() as? Map<*, *>
                ?: throw IllegalStateException("장소 정보를 확인하지 못했어요.")
            if(result["status"] != "success") {
                val error = result["error"] as? Map<*, *>
                throw IllegalStateException(error?.get("message") as? String ?: "장소 이름과 주소를 직접 입력해 주세요.")
            }
            val place = result["place"] as? Map<*, *> ?: throw IllegalStateException("장소 정보를 확인하지 못했어요.")
            return TravelPlace(UUID.randomUUID().toString(), place["name"] as String,
                address = place["address"] as? String ?: "", category = place["category"] as? String ?: "OTHER",
                sourceUrl = url, latitude = (place["latitude"] as? Number)?.toDouble(),
                longitude = (place["longitude"] as? Number)?.toDouble())
        } catch(e: CancellationException) { throw e }
        catch(e: FirebaseFunctionsException) { throw IllegalStateException(errorText(e), e) }
    }
    fun openTrip(id: String, onSuccess: () -> Unit) = runRead { selected = repository.getTrip(id); onSuccess() }
    fun previewPlaces(urls: List<String>, onResult: (PlaceImportBatch) -> Unit) {
        if (busy || retryAvailable) return
        if (urls.size !in 1..10 || urls.any { it.isBlank() || it.trim().length > 500 }) {
            message = "지도 링크는 1~10개, 각각 500자 이하로 입력해 주세요."
            return
        }
        runRead { onResult(importer.preview(urls)) }
    }
    fun saveImportedPlaces(places: List<TravelPlace>, onSuccess: () -> Unit = {}) {
        if (busy || retryAvailable) return
        if (selected == null || places.size !in 1..10) {
            message = "여행을 선택하고 장소 1~10개를 확인해 주세요."
            return
        }
        change("addPlaces", mapOf("places" to places.map { it.toAddPlacesInput() }), onSuccess)
    }
    fun refresh() = runRead {
        selected?.let { selected = repository.getTrip(it.id) }
        trips = repository.listTrips()
    }
    private fun runRead(work: suspend () -> Unit) {
        if (busy || retryAvailable) return
        busy = true; message = null
        viewModelScope.launch {
            try { work() } catch(e: CancellationException) { throw e }
            catch(e: Exception) { message = errorText(e) } finally { busy = false }
        }
    }
    fun createTrip(title: String, start: String, end: String, onSuccess: () -> Unit) = write(
        "createTrip", mapOf("title" to title.trim(), "startDate" to start, "endDate" to end), false, onSuccess)
    fun createPlan(title: String, start: String, end: String, places: List<Map<String, Any?>>, onSuccess: () -> Unit) = write(
        "createTrip", mapOf("title" to title.trim(), "startDate" to start, "endDate" to end, "places" to places), false, onSuccess)
    fun change(action: String, data: Map<String, Any?> = emptyMap(), onSuccess: () -> Unit = {}) = write(action, data, true, onSuccess)
    private fun write(action: String, extra: Map<String, Any?>, existing: Boolean, onSuccess: () -> Unit) {
        if(busy || retryAvailable) return
        val trip = selected
        if(existing && trip == null) return
        val data = mutableMapOf<String, Any?>("action" to action, "requestId" to UUID.randomUUID().toString())
        if(existing) { data["tripId"] = trip!!.id; data["expectedVersion"] = trip.version }
        data.putAll(extra)
        pending = data.toMap(); pendingSuccess = onSuccess
        executePending()
    }
    fun retry() { if(!busy && pending != null) executePending() }
    private fun executePending() {
        val data = pending ?: return
        busy = true; message = null; retryAvailable = false
        viewModelScope.launch {
            var saved = false
            val success = pendingSuccess
            try {
                val response = repository.call(data)
                saved = true
                pending = null; pendingSuccess = null
                if(data["action"] == "deleteTrip") selected = null
                else selected = repository.getTrip(response["tripId"] as String)
                trips = repository.listTrips()
                success?.invoke()
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) {
                val code = (e as? FirebaseFunctionsException)?.code
                if(!saved && (code == FirebaseFunctionsException.Code.UNAVAILABLE || code == FirebaseFunctionsException.Code.DEADLINE_EXCEEDED || code == FirebaseFunctionsException.Code.INTERNAL)) {
                    retryAvailable = true
                    message = "저장 결과를 확인하지 못했어요. 같은 요청으로 재시도하거나 최신 내용을 다시 확인해 주세요."
                } else {
                    pending = null; pendingSuccess = null
                    message = if(saved) "저장은 완료됐지만 화면 갱신에 실패했어요. 새로고침해 주세요." else errorText(e)
                    if(saved) success?.invoke()
                    if(code == FirebaseFunctionsException.Code.ABORTED) {
                        try { selected = selected?.let { repository.getTrip(it.id) } } catch(_: Exception) { }
                    }
                }
            } finally { busy = false }
        }
    }
    private fun errorText(error: Exception): String {
        val e = error as? FirebaseFunctionsException ?: return "연결을 확인하고 다시 시도해 주세요."
        return when(e.code) {
            FirebaseFunctionsException.Code.UNAUTHENTICATED -> "로그인이 만료됐어요. 로그아웃 후 다시 로그인해 주세요."
            FirebaseFunctionsException.Code.PERMISSION_DENIED -> "이 여행에 접근할 권한이 없어요."
            FirebaseFunctionsException.Code.ABORTED -> "다른 수정이 반영됐어요. 최신 내용을 불러왔으니 다시 확인해 주세요."
            FirebaseFunctionsException.Code.NOT_FOUND -> "여행 또는 장소가 없어요. 배포와 목록을 확인하고 새로고침해 주세요."
            FirebaseFunctionsException.Code.ALREADY_EXISTS, FirebaseFunctionsException.Code.INVALID_ARGUMENT, FirebaseFunctionsException.Code.FAILED_PRECONDITION,
            FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED -> e.message ?: "입력 내용을 확인해 주세요."
            else -> "서버에 연결하지 못했어요. 인터넷과 함수 배포 상태를 확인해 주세요."
        }
    }
}
