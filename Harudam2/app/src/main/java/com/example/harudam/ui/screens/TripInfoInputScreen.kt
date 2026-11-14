package com.example.harudam.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val CreateMint = Color(0xFFA0E8CB)
private val CreateMintLight = Color(0xFFC7F9E5)
private val CreateIvory = Color(0xFFFBF9F4)
private val CreateText = Color(0xFF554239)
private val CreateSubtext = Color(0xFF7D6A60)
private val CreateBorder = Color(0xFFECE5DC)

/** Figma screen 2: 여행 이름과 여행 기간을 입력합니다. */
@Composable
fun TripInfoInputScreen(
    onBackClick: () -> Unit = {},
    onConfirmPlacesClick: () -> Unit = {},
) {
    var tripName by remember { mutableStateOf("") }
    var startDateMillis by remember { mutableStateOf<Long?>(null) }
    var endDateMillis by remember { mutableStateOf<Long?>(null) }
    var placeListUrl by remember { mutableStateOf("") }
    val importedPlaces = remember {
        mutableStateListOf<String>()
    }
    var deletePlaceTargetIndex by remember { mutableStateOf<Int?>(null) }

    val tripInfoComplete = tripName.isNotBlank() &&
            startDateMillis != null &&
            endDateMillis != null
    val canConfirmPlaces = tripInfoComplete && placeListUrl.isNotBlank()

    Surface(modifier = Modifier.fillMaxSize(), color = CreateIvory) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            CreateTripHeader(onBackClick = onBackClick)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 21.dp)
                    .offset(y = (-31).dp),
            ) {
                CreateTripForm(
                    tripName = tripName,
                    onTripNameChange = { tripName = it },
                    startDateMillis = startDateMillis,
                    endDateMillis = endDateMillis,
                    onStartDateSelected = { selectedDate ->
                        startDateMillis = selectedDate
                        if (endDateMillis != null && endDateMillis!! < selectedDate) {
                            endDateMillis = null
                        }
                    },
                    onEndDateSelected = { endDateMillis = it },
                    placeListUrl = placeListUrl,
                    onPlaceListUrlChange = { placeListUrl = it },
                    placesEnabled = tripInfoComplete,
                )
                Spacer(Modifier.height(18.dp))
                CreateMapPlaceholder(hasPlaces = importedPlaces.isNotEmpty())
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "여행 장소 ${importedPlaces.size}",
                    color = CreateText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(10.dp))
                if (importedPlaces.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CreateBorder),
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "추가된 장소가 없어요",
                                color = CreateSubtext,
                                fontSize = 12.sp,
                            )
                        }
                    }
                } else {
                    importedPlaces.forEachIndexed { index, name ->
                        ImportedPlaceRow(
                            name = name,
                            onDeleteClick = { deletePlaceTargetIndex = index },
                        )
                        if (index != importedPlaces.lastIndex) {
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
                Spacer(Modifier.height(26.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 34.dp)
                        .height(47.dp)
                        .clickable(
                            enabled = canConfirmPlaces,
                            onClick = onConfirmPlacesClick,
                        ),
                    color = if (canConfirmPlaces) CreateMint else Color(0xFFE7E3DC),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "장소 확정하기",
                            color = if (canConfirmPlaces) CreateText else CreateSubtext,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        deletePlaceTargetIndex?.let { index ->
            val placeName = importedPlaces.getOrNull(index)
            if (placeName != null) {
                AlertDialog(
                    onDismissRequest = { deletePlaceTargetIndex = null },
                    title = { Text("장소를 삭제할까요?") },
                    text = { Text("$placeName 장소를 여행 목록에서 삭제해요.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                importedPlaces.removeAt(index)
                                deletePlaceTargetIndex = null
                            },
                        ) {
                            Text("삭제")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { deletePlaceTargetIndex = null }) {
                            Text("취소")
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun CreateTripHeader(onBackClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(119.dp)
            .background(CreateMint, RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 34.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "‹",
                modifier = Modifier.clickable(onClick = onBackClick),
                color = CreateText,
                fontSize = 27.sp,
                lineHeight = 27.sp,
            )
            Text(
                "새 여행 만들기",
                modifier = Modifier.padding(start = 9.dp),
                color = CreateText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CreateTripForm(
    tripName: String,
    onTripNameChange: (String) -> Unit,
    startDateMillis: Long?,
    endDateMillis: Long?,
    onStartDateSelected: (Long) -> Unit,
    onEndDateSelected: (Long) -> Unit,
    placeListUrl: String,
    onPlaceListUrlChange: (String) -> Unit,
    placesEnabled: Boolean,
) {
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("yyyy.MM.dd", Locale.KOREA) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CreateBorder),
    ) {
        Column(modifier = Modifier.padding(horizontal = 17.dp, vertical = 12.dp)) {
            FormLabel("여행 이름")
            Spacer(Modifier.height(5.dp))
            OutlinedTextField(
                value = tripName,
                onValueChange = onTripNameChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                singleLine = true,
                placeholder = {
                    Text("여행 이름 입력", color = CreateSubtext, fontSize = 14.sp)
                },
                textStyle = TextStyle(
                    color = CreateText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CreateIvory,
                    unfocusedContainerColor = CreateIvory,
                    focusedBorderColor = CreateBorder,
                    unfocusedBorderColor = CreateBorder,
                    cursorColor = CreateText,
                ),
            )

            Spacer(Modifier.height(8.dp))
            FormLabel("날짜", modifier = Modifier.padding(start = 2.dp))
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                DateInputField(
                    text = startDateMillis?.let { dateFormatter.format(Date(it)) }.orEmpty(),
                    placeholder = "시작 날짜",
                    modifier = Modifier.width(114.dp),
                    onClick = {
                        val initial = startDateMillis?.let { Calendar.getInstance().apply { timeInMillis = it } }
                            ?: Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val selected = Calendar.getInstance().apply {
                                    set(year, month, day, 0, 0, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                onStartDateSelected(selected.timeInMillis)
                            },
                            initial.get(Calendar.YEAR),
                            initial.get(Calendar.MONTH),
                            initial.get(Calendar.DAY_OF_MONTH),
                        ).show()
                    },
                )
                Text("~", color = CreateText, fontSize = 15.sp)
                DateInputField(
                    text = endDateMillis?.let { dateFormatter.format(Date(it)) }.orEmpty(),
                    placeholder = "종료 날짜",
                    modifier = Modifier.width(118.dp),
                    onClick = {
                        val initialMillis = endDateMillis ?: startDateMillis ?: System.currentTimeMillis()
                        val initial = Calendar.getInstance().apply { timeInMillis = initialMillis }
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val selected = Calendar.getInstance().apply {
                                    set(year, month, day, 0, 0, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                onEndDateSelected(selected.timeInMillis)
                            },
                            initial.get(Calendar.YEAR),
                            initial.get(Calendar.MONTH),
                            initial.get(Calendar.DAY_OF_MONTH),
                        ).apply {
                            startDateMillis?.let { datePicker.minDate = it }
                        }.show()
                    },
                )
            }

            if (startDateMillis != null && endDateMillis == null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "종료 날짜는 시작 날짜 이후로 선택해 주세요.",
                    color = CreateSubtext,
                    fontSize = 11.sp,
                )
            }

            Spacer(Modifier.height(8.dp))
            FormLabel("장소 목록 URL", modifier = Modifier.padding(start = 2.dp))
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = placeListUrl,
                onValueChange = onPlaceListUrlChange,
                enabled = placesEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                singleLine = true,
                placeholder = {
                    Text(
                        text = if (placesEnabled) {
                            "지도 목록 링크를 붙여넣으세요"
                        } else {
                            "여행 이름과 날짜를 먼저 입력해 주세요"
                        },
                        color = CreateSubtext,
                        fontSize = 12.sp,
                    )
                },
                textStyle = TextStyle(
                    color = CreateText,
                    fontSize = 12.sp,
                ),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color(0xFFF1EEE8),
                    focusedBorderColor = CreateBorder,
                    unfocusedBorderColor = CreateBorder,
                    disabledBorderColor = CreateBorder,
                    focusedTextColor = CreateText,
                    unfocusedTextColor = CreateText,
                    disabledTextColor = CreateSubtext,
                    cursorColor = CreateText,
                ),
            )
        }
    }
}

@Composable
private fun FormLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier.padding(start = 9.dp), color = CreateSubtext, fontSize = 12.sp)
}

@Composable
private fun DateInputField(
    text: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(43.dp)
            .clickable(onClick = onClick),
        color = CreateIvory,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CreateBorder),
    ) {
        Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = text.ifEmpty { placeholder },
                color = if (text.isEmpty()) CreateSubtext else CreateText,
                fontSize = 12.sp,
                fontWeight = if (text.isEmpty()) FontWeight.Normal else FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun CreateMapPlaceholder(hasPlaces: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(Color.White, RoundedCornerShape(10.dp))
            .padding(1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = CreateIvory,
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CreateBorder),
        ) {
            if (hasPlaces) {
                Box(contentAlignment = Alignment.Center) {
                    Text("지도", color = CreateText, fontSize = 36.sp, fontWeight = FontWeight.Light)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {

                    Spacer(Modifier.height(9.dp))
                    Text(
                        text = "지도에 표시할 장소가 없어요",
                        color = CreateText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "장소 목록 URL을 추가하면 여기에 표시돼요.",
                        color = CreateSubtext,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportedPlaceRow(
    name: String,
    onDeleteClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(47.dp),
        color = Color.White,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CreateBorder),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("♧", color = Color(0xFF8AAE9A), fontSize = 20.sp)
            Text(name, modifier = Modifier.padding(start = 9.dp).weight(1f), color = CreateText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            CategoryTag("음식·카페")
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(36.dp),
            ) {
                PlaceDeleteIcon(modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun PlaceDeleteIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 1.6.dp.toPx()
        val left = size.width * 0.24f
        val top = size.height * 0.29f
        val bodyWidth = size.width * 0.52f
        val bodyHeight = size.height * 0.57f

        drawRoundRect(
            color = CreateSubtext,
            topLeft = Offset(left, top),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = CornerRadius(1.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
        )
        drawLine(
            color = CreateSubtext,
            start = Offset(size.width * 0.17f, size.height * 0.23f),
            end = Offset(size.width * 0.83f, size.height * 0.23f),
            strokeWidth = stroke,
        )
        drawLine(
            color = CreateSubtext,
            start = Offset(size.width * 0.39f, size.height * 0.15f),
            end = Offset(size.width * 0.61f, size.height * 0.15f),
            strokeWidth = stroke,
        )
        drawLine(
            color = CreateSubtext,
            start = Offset(size.width * 0.42f, size.height * 0.39f),
            end = Offset(size.width * 0.42f, size.height * 0.73f),
            strokeWidth = stroke,
        )
        drawLine(
            color = CreateSubtext,
            start = Offset(size.width * 0.58f, size.height * 0.39f),
            end = Offset(size.width * 0.58f, size.height * 0.73f),
            strokeWidth = stroke,
        )
    }
}

@Composable
private fun CategoryTag(text: String) {
    Surface(
        color = Color.White,
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, CreateBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(6.dp).background(Color(0xFFEE8A5F), CircleShape))
            Text(text, modifier = Modifier.padding(start = 5.dp), color = CreateText, fontSize = 11.sp)
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun TripInfoInputScreenPreview() {
    MaterialTheme { TripInfoInputScreen() }
}
