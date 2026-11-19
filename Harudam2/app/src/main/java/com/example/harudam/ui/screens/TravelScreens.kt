package com.example.harudam.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.harudam.model.TravelPlace
import com.example.harudam.model.TravelTrip
import com.example.harudam.viewmodel.TravelViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val Navy = Color(0xFF294577)
private val Pale = Color(0xFFEEF3FB)
private val Background = Color(0xFFF7F8FA)

@Composable
fun TravelFlow(vm: TravelViewModel, onLogout: () -> Unit) {
    var page by rememberSaveable { mutableStateOf("list") }
    var formOpen by rememberSaveable { mutableStateOf(false) }
    var editTrip by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<TravelTrip?>(null) }
    LaunchedEffect(Unit) { vm.loadTrips() }
    BackHandler(enabled = page != "list" && !vm.busy && !formOpen) { page = "list" }
    Surface(color = Background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(enabled = !vm.busy, onClick = {
                    if(page == "list") { vm.reset(); onLogout() } else page = "list"
                }) { Text(if(page == "list") "로그아웃" else "‹ 여행 목록", color = Navy) }
                TextButton(enabled = !vm.busy && !vm.retryAvailable, onClick = { if(page=="list") vm.loadTrips() else vm.refresh() }) { Text("새로고침", color = Navy) }
            }
            if(vm.busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Navy)
            vm.message?.let { message ->
                Card(colors = CardDefaults.cardColors(containerColor = Pale), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(message, style = MaterialTheme.typography.bodyMedium)
                        if(vm.retryAvailable) Row {
                            TextButton(onClick = { vm.retry() }) { Text("같은 요청 재시도") }
                            TextButton(onClick = { vm.discardRetry(); vm.refresh() }) { Text("최신 내용 확인") }
                        } else TextButton(onClick = { vm.clearMessage() }) { Text("닫기") }
                    }
                }
            }
            val enabled = !vm.busy && !vm.retryAvailable
            if(page == "list" || vm.selected == null) {
                Row(Modifier.fillMaxWidth().padding(vertical=16.dp), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
                    Text("여행 목록", fontSize=27.sp, fontWeight=FontWeight.Bold)
                    Button(enabled=enabled, onClick={ editTrip=false; formOpen=true }, colors=ButtonDefaults.buttonColors(containerColor=Navy)) { Text("+ 새 여행") }
                }
                if(vm.trips.isEmpty() && !vm.busy) Text("아직 만든 여행이 없어요. + 새 여행을 눌러 시작하세요.", modifier=Modifier.padding(vertical=24.dp))
                LazyColumn(verticalArrangement=Arrangement.spacedBy(14.dp), contentPadding=PaddingValues(bottom=24.dp)) {
                    items(vm.trips, key={it.id}) { trip ->
                        Card(shape=RoundedCornerShape(18.dp), colors=CardDefaults.cardColors(containerColor=Color.White), modifier=Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(16.dp), verticalAlignment=Alignment.CenterVertically) {
                                Column(Modifier.weight(1f).clickable(enabled=enabled) {
                                    vm.openTrip(trip.id) { page=if(vm.selected?.placesConfirmed==true) "itinerary" else "places" }
                                }) {
                                    Text("${trip.startDate} ~ ${trip.endDate}", color=Navy, fontSize=12.sp)
                                    Text(trip.title, fontSize=20.sp, fontWeight=FontWeight.SemiBold, modifier=Modifier.padding(vertical=8.dp))
                                    Text("장소 ${trip.placeCount}개 · ${if(trip.placesConfirmed) "장소 확정" else "계획 중"}", fontSize=13.sp)
                                    TextButton(enabled=enabled,onClick={vm.openTrip(trip.id){page=if(vm.selected?.placesConfirmed==true) "itinerary" else "places"}}) { Text("여행 열기") }
                                }
                                TextButton(enabled=enabled,onClick={vm.openTrip(trip.id){deleting=vm.selected}}) { Text("삭제") }
                            }
                        }
                    }
                }
            } else {
                val trip=vm.selected!!
                Text("${trip.startDate} ~ ${trip.endDate}", fontSize=13.sp, color=Navy)
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
                    Text(trip.title, fontSize=24.sp, fontWeight=FontWeight.Bold, modifier=Modifier.weight(1f))
                    TextButton(enabled=enabled,onClick={editTrip=true;formOpen=true}) { Text("여행 수정") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    listOf("places" to "장소", "assign" to "날짜 배치", "itinerary" to "일정 보기").forEach { (value,label) ->
                        TextButton(enabled=enabled,onClick={page=value}) { Text(label, color=if(page==value) Navy else Color.Gray, fontWeight=if(page==value) FontWeight.Bold else FontWeight.Normal) }
                    }
                }
                when(page) {
                    "places" -> PlaceCollection(trip,enabled,vm) { page="assign" }
                    "assign" -> DateAssignments(trip,enabled,vm) { page="itinerary" }
                    else -> Itinerary(trip,enabled,vm)
                }
            }
        }
    }
    if(formOpen) TripForm(if(editTrip) vm.selected else null, enabled=!vm.busy && !vm.retryAvailable, onDismiss={formOpen=false}) { title,start,end ->
        if(editTrip) vm.change("updateTrip",mapOf("title" to title,"startDate" to start,"endDate" to end)) {formOpen=false}
        else vm.createTrip(title,start,end) {formOpen=false;page="places"}
    }
    deleting?.let { trip ->
        ConfirmDelete("‘${trip.title}’ 여행과 장소·일정을 삭제할까요?",!vm.busy && !vm.retryAvailable,{deleting=null}) {
            vm.change("deleteTrip") {deleting=null;page="list"}
        }
    }
    // Server failures must remain actionable even while an edit dialog is open.
    vm.message?.let { message ->
        AlertDialog(onDismissRequest = { if(!vm.retryAvailable) vm.clearMessage() },
            title = { Text(if(vm.retryAvailable) "저장 결과 확인" else "안내") },
            text = { Text(message) },
            confirmButton = { TextButton(enabled = !vm.busy, onClick = {
                if(vm.retryAvailable) vm.retry() else vm.clearMessage()
            }) { Text(if(vm.retryAvailable) "같은 요청 재시도" else "확인") } },
            dismissButton = { if(vm.retryAvailable) TextButton(enabled = !vm.busy, onClick = {
                vm.discardRetry(); vm.refresh()
            }) { Text("최신 내용 확인") } })
    }
}

@Composable
private fun PlaceCollection(trip: TravelTrip, enabled: Boolean, vm: TravelViewModel, onConfirmed: () -> Unit) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TravelPlace?>(null) }
    var deleting by remember { mutableStateOf<TravelPlace?>(null) }
    Column {
        Text("가고 싶은 곳을 먼저 모아 보세요", fontWeight=FontWeight.SemiBold, modifier=Modifier.padding(vertical=8.dp))
        Primary("+ 장소 추가", enabled) {adding=true}
        if(trip.places.isEmpty()) Text("지도 링크와 장소 이름을 직접 입력할 수 있어요.",modifier=Modifier.padding(vertical=16.dp))
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(vertical=12.dp)) {
            items(trip.places,key={it.id}) { place ->
                PlaceCard(place) {
                    Row {
                        TextButton(enabled=enabled,onClick={editing=place}) {Text("수정")}
                        TextButton(enabled=enabled,onClick={deleting=place}) {Text("삭제")}
                    }
                }
            }
        }
        Primary("장소 확정하고 날짜 배치", enabled && trip.places.isNotEmpty()) { vm.change("confirmPlaces",onSuccess=onConfirmed) }
        Spacer(Modifier.height(16.dp))
    }
    if(adding || editing!=null) PlaceForm(editing,enabled,{adding=false;editing=null}) { fields ->
        val extra=fields.toMutableMap(); editing?.let { extra["placeId"]=it.id }
        vm.change(if(editing==null) "addPlace" else "updatePlace",extra) {adding=false;editing=null}
    }
    deleting?.let { p -> ConfirmDelete("‘${p.name}’ 장소와 연결된 일정 배치를 삭제할까요?",enabled,{deleting=null}) {
        vm.change("deletePlace",mapOf("placeId" to p.id)) {deleting=null}
    } }
}

@Composable
private fun DateAssignments(trip: TravelTrip, enabled: Boolean, vm: TravelViewModel, onNext: () -> Unit) {
    var targetDate by remember { mutableStateOf<String?>(null) }
    var moving by remember { mutableStateOf<TravelPlace?>(null) }
    Column {
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(vertical=12.dp)) {
            item { Text("장소를 날짜별로 배치하세요. 배치 변경은 바로 저장됩니다.", fontSize=14.sp) }
            items(trip.dates,key={it}) { date ->
                Column {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                        Text("${ChronoUnit.DAYS.between(LocalDate.parse(trip.startDate),LocalDate.parse(date))+1}일차 · $date",fontWeight=FontWeight.Bold)
                        TextButton(enabled=enabled,onClick={targetDate=date}) {Text("추가하기")}
                    }
                    if(trip.placesOn(date).isEmpty()) Text("아직 담은 장소가 없습니다",modifier=Modifier.padding(12.dp),color=Color.Gray)
                    trip.placesOn(date).forEach { place ->
                        PlaceCard(place) {
                            Row {
                                TextButton(enabled=enabled,onClick={moving=place}) {Text("날짜 이동")}
                                TextButton(enabled=enabled,onClick={vm.change("removeSchedule",mapOf("placeId" to place.id))}) {Text("배치 취소")}
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            item {Text("미배치 장소 ${trip.places.count{it.visitDate==null}}개",color=Navy)}
        }
        Primary("확정하고 일정 보기",enabled && trip.places.isNotEmpty()) {
            if(trip.placesConfirmed) onNext() else vm.change("confirmPlaces",onSuccess=onNext)
        }
        Spacer(Modifier.height(16.dp))
    }
    targetDate?.let { date ->
        AlertDialog(onDismissRequest={if(enabled) targetDate=null},title={Text("$date 에 추가")},text={
            LazyColumn { val available=trip.places.filter {it.visitDate==null}
                if(available.isEmpty()) item{Text("미배치 장소가 없어요. 장소 탭에서 추가하거나 기존 장소의 날짜를 이동하세요.")}
                items(available,key={it.id}) {p -> TextButton(enabled=enabled,onClick={vm.change("setSchedule",mapOf("placeId" to p.id,"visitDate" to date)){targetDate=null}}){Text(p.name)} }
            }
        },confirmButton={TextButton(enabled=enabled,onClick={targetDate=null}){Text("닫기")}})
    }
    moving?.let { p -> ScheduleForm(p,trip,enabled,{moving=null}) {date,time,note ->
        vm.change("setSchedule",mapOf("placeId" to p.id,"visitDate" to date,"visitTime" to time,"note" to note)){moving=null}
    } }
}

@Composable
private fun Itinerary(trip: TravelTrip, enabled: Boolean, vm: TravelViewModel) {
    var day by rememberSaveable(trip.id) {mutableStateOf(0)}
    var editing by remember {mutableStateOf<TravelPlace?>(null)}
    var removing by remember {mutableStateOf<TravelPlace?>(null)}
    val index=day.coerceIn(0,trip.dates.lastIndex)
    val date=trip.dates[index]
    val places=trip.placesOn(date)
    val context=LocalContext.current
    Column {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically) {
            TextButton(enabled=enabled && index>0,onClick={day=index-1}) {Text("‹ 이전")}
            Text("${index+1}일차 · $date",fontWeight=FontWeight.Bold)
            TextButton(enabled=enabled && index<trip.dates.lastIndex,onClick={day=index+1}) {Text("다음 ›")}
        }
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=24.dp)) {
            item {
                Card(colors=CardDefaults.cardColors(containerColor=Pale),modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("오늘의 방문 장소 ${places.size}곳",fontWeight=FontWeight.SemiBold)
                        Text("장소의 ‘지도 열기’를 누르면 지도 앱에서 위치를 확인할 수 있어요.",fontSize=13.sp)
                    }
                }
            }
            if(places.isEmpty()) item {Text("이 날짜에 배치한 장소가 없어요. 날짜 배치 탭에서 추가하세요.",modifier=Modifier.padding(vertical=20.dp))}
            items(places,key={it.id}) {p ->
                PlaceCard(p) {
                    Row(horizontalArrangement=Arrangement.spacedBy(2.dp)) {
                        TextButton(enabled=enabled,onClick={editing=p}) {Text("시간·메모")}
                        TextButton(enabled=enabled,onClick={
                            val query=if(p.latitude!=null && p.longitude!=null) "${p.latitude},${p.longitude}" else p.address.ifBlank{p.name}
                            val uri=if(p.sourceUrl.isNotBlank()) Uri.parse(p.sourceUrl) else Uri.parse("geo:0,0?q=${Uri.encode(query)}")
                            try {context.startActivity(Intent(Intent.ACTION_VIEW,uri))} catch(_: Exception) {
                                try {context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}")))} catch(_: Exception) {android.widget.Toast.makeText(context,"지도를 열 앱을 찾지 못했어요.",android.widget.Toast.LENGTH_SHORT).show()}
                            }
                        }) {Text("지도 열기")}
                        TextButton(enabled=enabled,onClick={removing=p}) {Text("빼기")}
                    }
                    Row {
                        val position=places.indexOf(p)
                        TextButton(enabled=enabled && position>0,onClick={
                            val ids=places.map{it.id}.toMutableList(); java.util.Collections.swap(ids,position,position-1)
                            vm.change("reorderSchedule",mapOf("visitDate" to date,"placeIds" to ids))
                        }) {Text("↑ 위로")}
                        TextButton(enabled=enabled && position<places.lastIndex,onClick={
                            val ids=places.map{it.id}.toMutableList(); java.util.Collections.swap(ids,position,position+1)
                            vm.change("reorderSchedule",mapOf("visitDate" to date,"placeIds" to ids))
                        }) {Text("↓ 아래로")}
                    }
                }
            }
            item {Primary("이 날짜 일정 공유",enabled && places.isNotEmpty()) {
                val content=buildString{append("${trip.title}\n$date\n");places.forEachIndexed{i,p->append("\n${i+1}. ${p.visitTime.orEmpty()} ${p.name}\n${p.note}\n${p.address}\n")}}
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,content)},"일정 공유"))
            }}
        }
    }
    editing?.let{p->ScheduleForm(p,trip,enabled,{editing=null}){d,t,n->vm.change("setSchedule",mapOf("placeId" to p.id,"visitDate" to d,"visitTime" to t,"note" to n)){editing=null}}}
    removing?.let{p->ConfirmDelete("‘${p.name}’을 날짜 일정에서 뺄까요? 장소는 보관됩니다.",enabled,{removing=null}){vm.change("removeSchedule",mapOf("placeId" to p.id)){removing=null}}}
}

@Composable
private fun PlaceCard(place: TravelPlace, content: @Composable ColumnScope.() -> Unit) {
    Card(shape=RoundedCornerShape(14.dp),colors=CardDefaults.cardColors(containerColor=Color.White),modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text("${place.visitOrder?.let{"$it. "}.orEmpty()}${place.name}",fontWeight=FontWeight.SemiBold,fontSize=17.sp)
            Text(when(place.category){"FOOD"->"음식·카페";"TOUR"->"관광";"STAY"->"숙소";else->"기타"},fontSize=12.sp,color=Navy)
            if(place.address.isNotBlank()) Text(place.address,fontSize=13.sp)
            if(!place.visitTime.isNullOrBlank()) Text("방문 시간 ${place.visitTime}",fontSize=13.sp)
            if(place.note.isNotBlank()) Text(place.note,fontSize=14.sp,modifier=Modifier.padding(top=6.dp))
            content()
        }
    }
}

@Composable
private fun Primary(label:String,enabled:Boolean,onClick:()->Unit) {
    Button(onClick=onClick,enabled=enabled,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp),shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.buttonColors(containerColor=Navy)){Text(label)}
}

@Composable
private fun ConfirmDelete(message:String,enabled:Boolean,onDismiss:()->Unit,onConfirm:()->Unit) {
    AlertDialog(onDismissRequest={if(enabled)onDismiss()},title={Text("삭제 확인")},text={Text(message)},
        confirmButton={TextButton(enabled=enabled,onClick=onConfirm){Text("삭제")}},dismissButton={TextButton(enabled=enabled,onClick=onDismiss){Text("취소")}})
}

@Composable
private fun DateInput(label:String,value:String,enabled:Boolean,onChange:(String)->Unit) {
    val context=LocalContext.current
    OutlinedButton(enabled=enabled,onClick={
        val d=runCatching{LocalDate.parse(value)}.getOrDefault(LocalDate.now())
        DatePickerDialog(context,{_,y,m,day->onChange(LocalDate.of(y,m+1,day).toString())},d.year,d.monthValue-1,d.dayOfMonth).show()
    },modifier=Modifier.fillMaxWidth()){Text("$label  $value")}
}

@Composable
private fun TripForm(trip:TravelTrip?,enabled:Boolean,onDismiss:()->Unit,onSave:(String,String,String)->Unit) {
    var title by rememberSaveable(trip?.id){mutableStateOf(trip?.title.orEmpty())}
    var start by rememberSaveable(trip?.id){mutableStateOf(trip?.startDate?:LocalDate.now().toString())}
    var end by rememberSaveable(trip?.id){mutableStateOf(trip?.endDate?:LocalDate.now().toString())}
    var error by remember{mutableStateOf<String?>(null)}
    AlertDialog(onDismissRequest={if(enabled)onDismiss()},title={Text(if(trip==null)"여행일정 추가" else "여행 수정")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
        OutlinedTextField(value=title,onValueChange={if(it.length<=60)title=it},enabled=enabled,label={Text("여행명")},singleLine=true)
        DateInput("시작일",start,enabled){start=it;if(end<start)end=start}
        DateInput("종료일",end,enabled){end=it}
        Text("장소는 여행을 만든 다음 화면에서 추가합니다.",fontSize=13.sp)
        error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
    }},confirmButton={TextButton(enabled=enabled,onClick={
        error=when{title.isBlank()->"여행명을 입력해 주세요.";end<start->"종료일은 시작일 이후여야 해요.";ChronoUnit.DAYS.between(LocalDate.parse(start),LocalDate.parse(end))>365->"여행 기간은 최대 366일이에요.";else->null}
        if(error==null)onSave(title.trim(),start,end)
    }){Text("저장")}},dismissButton={TextButton(enabled=enabled,onClick=onDismiss){Text("취소")}})
}

@Composable
private fun PlaceForm(place:TravelPlace?,enabled:Boolean,onDismiss:()->Unit,onSave:(Map<String,Any?>)->Unit) {
    var name by rememberSaveable(place?.id){mutableStateOf(place?.name.orEmpty())}
    var address by rememberSaveable(place?.id){mutableStateOf(place?.address.orEmpty())}
    var url by rememberSaveable(place?.id){mutableStateOf(place?.sourceUrl.orEmpty())}
    var category by rememberSaveable(place?.id){mutableStateOf(place?.category?:"OTHER")}
    var error by remember{mutableStateOf<String?>(null)}
    AlertDialog(onDismissRequest={if(enabled)onDismiss()},title={Text(if(place==null)"장소 추가" else "장소 수정")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
        OutlinedTextField(value=name,onValueChange={if(it.length<=100)name=it},enabled=enabled,label={Text("장소 이름 (필수)")},singleLine=true)
        OutlinedTextField(value=url,onValueChange={if(it.length<=500)url=it},enabled=enabled,label={Text("장소 링크 (선택)")},singleLine=true)
        Text("링크만으로 정보를 자동 추출하지 않아요. 장소 이름을 확인해 주세요.",fontSize=12.sp)
        OutlinedTextField(value=address,onValueChange={if(it.length<=300)address=it},enabled=enabled,label={Text("주소 (선택)")})
        listOf("FOOD" to "음식·카페","TOUR" to "관광","STAY" to "숙소","OTHER" to "기타").chunked(2).forEach{row->Row{row.forEach{(key,label)->FilterChip(selected=category==key,enabled=enabled,onClick={category=key},label={Text(label)},modifier=Modifier.padding(end=6.dp))}}}
        error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
    }},confirmButton={TextButton(enabled=enabled,onClick={
        val validUrl=url.isBlank() || runCatching{val u=java.net.URI(url.trim());u.scheme in listOf("http","https") && !u.host.isNullOrBlank()}.getOrDefault(false)
        error=if(name.isBlank())"장소 이름을 입력해 주세요." else if(!validUrl)"http 또는 https 장소 링크를 입력해 주세요." else null
        if(error==null)onSave(mapOf("name" to name.trim(),"address" to address.trim(),"sourceUrl" to url.trim(),"category" to category))
    }){Text("저장")}},dismissButton={TextButton(enabled=enabled,onClick=onDismiss){Text("취소")}})
}

@Composable
private fun ScheduleForm(place:TravelPlace,trip:TravelTrip,enabled:Boolean,onDismiss:()->Unit,onSave:(String,String?,String)->Unit) {
    var date by rememberSaveable(place.id){mutableStateOf(place.visitDate?:trip.startDate)}
    var time by rememberSaveable(place.id){mutableStateOf(place.visitTime)}
    var note by rememberSaveable(place.id){mutableStateOf(place.note)}
    var error by remember{mutableStateOf<String?>(null)}
    val context=LocalContext.current
    AlertDialog(onDismissRequest={if(enabled)onDismiss()},title={Text(place.name)},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
        DateInput("방문 날짜",date,enabled){date=it}
        OutlinedButton(enabled=enabled,onClick={val p=(time?:"09:00").split(":");TimePickerDialog(context,{_,h,m->time=String.format(java.util.Locale.US,"%02d:%02d",h,m)},p[0].toInt(),p[1].toInt(),true).show()}){Text("방문 시간: ${time?:"미정"}")}
        if(time!=null)TextButton(enabled=enabled,onClick={time=null}){Text("시간 미정으로 변경")}
        OutlinedTextField(value=note,onValueChange={if(it.length<=1000)note=it},enabled=enabled,label={Text("메모")},minLines=2,maxLines=5)
        error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
    }},confirmButton={TextButton(enabled=enabled,onClick={
        if(date<trip.startDate || date>trip.endDate)error="여행 기간 안의 날짜를 선택해 주세요." else onSave(date,time,note.trim())
    }){Text("저장")}},dismissButton={TextButton(enabled=enabled,onClick=onDismiss){Text("취소")}})
}
