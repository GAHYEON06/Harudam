package com.example.harudam.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.harudam.model.TravelPlace
import com.example.harudam.model.TravelTrip
import java.time.LocalDate
import java.util.Locale

@Composable
fun GreenScheduleEditor(place: TravelPlace, trip: TravelTrip, enabled: Boolean,
    onDismiss: () -> Unit, onSave: (String, String?, String) -> Unit, onRemove: () -> Unit) {
    var date by rememberSaveable(place.id) { mutableStateOf(place.visitDate ?: trip.startDate) }
    var time by rememberSaveable(place.id) { mutableStateOf(place.visitTime) }
    var note by rememberSaveable(place.id) { mutableStateOf(place.note) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    AlertDialog(onDismissRequest = { if(enabled) onDismiss() }, title = { Text(place.name) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            TextButton(enabled=enabled,onClick={
                val day=LocalDate.parse(date)
                DatePickerDialog(context,{_,y,m,d->date=LocalDate.of(y,m+1,d).toString()},day.year,day.monthValue-1,day.dayOfMonth).show()
            }) { Text("방문 날짜: $date") }
            TextButton(enabled=enabled,onClick={
                val parts=(time ?: "09:00").split(":")
                TimePickerDialog(context,{_,h,m->time=String.format(Locale.US,"%02d:%02d",h,m)},parts[0].toInt(),parts[1].toInt(),true).show()
            }) { Text("방문 시간: ${time ?: "미정"}") }
            if(time!=null) TextButton(enabled=enabled,onClick={time=null}) { Text("시간 지우기") }
            OutlinedTextField(value=note,onValueChange={if(it.length<=1000)note=it},enabled=enabled,label={Text("메모")},maxLines=5)
            Row {
                TextButton(enabled=enabled,onClick={
                    val query=place.address.ifBlank{place.name}
                    val uri=place.sourceUrl.takeIf{it.isNotBlank()} ?: "https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}"
                    runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(uri)))}
                }) { Text("지도 열기") }
                if(place.visitDate!=null) TextButton(enabled=enabled,onClick=onRemove){Text("배치 취소")}
            }
            error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        } },
        confirmButton={TextButton(enabled=enabled,onClick={
            if(date<trip.startDate || date>trip.endDate) error="여행 기간 안의 날짜를 선택해 주세요." else onSave(date,time,note.trim())
        }){Text("저장")}},dismissButton={TextButton(enabled=enabled,onClick=onDismiss){Text("취소")}})
}
