package com.example.harudam.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

private val ScheduleMint = Color(0xFFA0E8CB)
private val ScheduleMintLight = Color(0xFFC7F9E5)
private val ScheduleIvory = Color(0xFFFBF9F4)
private val ScheduleText = Color(0xFF554239)
private val ScheduleSubtext = Color(0xFF7D6A60)
private val ScheduleBorder = Color(0xFFECE5DC)
private val FoodOrange = Color(0xFFEE8A5F)
private val TourBlue = Color(0xFF4C9BC4)

private data class ScheduleDayUi(
    val dayNumber: Int,
    val dateLabel: String,
    val places: List<SchedulePlaceUi>,
)

private data class SchedulePlaceUi(
    val id: String,
    val name: String,
    val category: String,
)

/** Figma screen 3: 핸들을 길게 눌러 같은 날짜 안에서 장소 순서를 바꿉니다. */
@Composable
fun TripScheduleScreen() {
    val days = listOf(
        ScheduleDayUi(
            1,
            "09.01 (화)",
            listOf(
                SchedulePlaceUi("day1-place1", "초량밀면", "음식·카페"),
                SchedulePlaceUi("day1-place2", "코이", "음식·카페"),
                SchedulePlaceUi("day1-place3", "광안리해수욕장", "관광"),
            ),
        ),
        ScheduleDayUi(
            2,
            "09.02 (수)",
            listOf(
                SchedulePlaceUi("day2-place1", "AREA6", "관광"),
                SchedulePlaceUi("day2-place2", "자매국밥", "음식·카페"),
            ),
        ),
        ScheduleDayUi(3, "09.03 (목)", emptyList()),
    )

    Surface(modifier = Modifier.fillMaxSize(), color = ScheduleIvory) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            ScheduleHeader()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 21.dp)
                    .offset(y = (-40).dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                days.forEach { day -> ScheduleDayCard(day) }
            }
        }
    }
}

@Composable
private fun ScheduleHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(161.dp)
            .background(ScheduleMint, RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
            .padding(start = 16.dp, end = 20.dp, top = 32.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = ScheduleText, fontSize = 27.sp, lineHeight = 27.sp)
            Text(
                "일정 정리",
                modifier = Modifier.padding(start = 9.dp),
                color = ScheduleText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = "부산여행 | 2026.09.01 ~ 09.05",
            modifier = Modifier.padding(start = 25.dp, top = 7.dp),
            color = ScheduleText,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun ScheduleDayCard(day: ScheduleDayUi) {
    val places = remember(day.dayNumber) {
        mutableStateListOf<SchedulePlaceUi>().apply { addAll(day.places) }
    }
    var draggedIndex by remember(day.dayNumber) { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember(day.dayNumber) { mutableStateOf(0f) }
    val density = LocalDensity.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ScheduleBorder),
        shadowElevation = 3.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 21.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${day.dayNumber}일차",
                    color = ScheduleText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    day.dateLabel,
                    modifier = Modifier.padding(start = 10.dp).weight(1f),
                    color = ScheduleSubtext,
                    fontSize = 12.sp,
                )
                Box(
                    modifier = Modifier
                        .background(ScheduleMintLight, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text("+ 추가", color = ScheduleText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(9.dp))
            if (places.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(45.dp)
                        .background(Color.White, RoundedCornerShape(10.dp))
                        .padding(1.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.White,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ScheduleBorder),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("아직 장소가 없어요", color = ScheduleSubtext, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    places.forEachIndexed { index, place ->
                        key(place.id) {
                            SchedulePlaceRow(
                                number = index + 1,
                                place = place,
                                isDragging = draggedIndex == index,
                                dragOffsetY = dragOffsetY,
                                onDragStart = {
                                    draggedIndex = index
                                    dragOffsetY = 0f
                                },
                                onDragDelta = { deltaY ->
                                    val currentIndex = draggedIndex
                                    if (currentIndex != null) {
                                        val rowStepPx = with(density) { (45.dp + 12.dp).toPx() }
                                        var remainingOffset = dragOffsetY + deltaY
                                        var targetIndex = currentIndex

                                        while (
                                            remainingOffset > rowStepPx / 2f &&
                                            targetIndex < places.lastIndex
                                        ) {
                                            targetIndex += 1
                                            remainingOffset -= rowStepPx
                                        }
                                        while (remainingOffset < -rowStepPx / 2f && targetIndex > 0) {
                                            targetIndex -= 1
                                            remainingOffset += rowStepPx
                                        }

                                        if (targetIndex != currentIndex) {
                                            val movedPlace = places.removeAt(currentIndex)
                                            places.add(targetIndex, movedPlace)
                                            draggedIndex = targetIndex
                                        }

                                        dragOffsetY = remainingOffset
                                    }
                                },
                                onDragFinished = {
                                    draggedIndex = null
                                    dragOffsetY = 0f
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SchedulePlaceRow(
    number: Int,
    place: SchedulePlaceUi,
    isDragging: Boolean,
    dragOffsetY: Float,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragFinished: () -> Unit,
) {
    Row(
        modifier = Modifier
            .pointerInput(place.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        onDragStart()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDragDelta(dragAmount.y)
                    },
                    onDragEnd = {
                        onDragFinished()
                    },
                    onDragCancel = {
                        onDragFinished()
                    },
                )
            }
            .graphicsLayer {
                translationY = if (isDragging) dragOffsetY else 0f
                shadowElevation = if (isDragging) 8.dp.toPx() else 0f
            }
            .zIndex(if (isDragging) 1f else 0f)
            .fillMaxWidth()
            .height(45.dp)
            .background(ScheduleIvory, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaceOrderBadge(number)
        Text(
            place.name,
            modifier = Modifier.padding(start = 12.dp).weight(1f),
            color = ScheduleText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        ScheduleCategoryChip(place.category)
        Box(
            modifier = Modifier
                .size(width = 28.dp, height = 40.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "≡",
                color = ScheduleSubtext,
                fontSize = 19.sp,
                lineHeight = 19.sp,
            )
        }
    }
}

@Composable
private fun PlaceOrderBadge(number: Int) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .background(ScheduleMintLight, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(number.toString(), color = Color(0xFF4A8C76), fontSize = 12.sp)
    }
}

@Composable
private fun ScheduleCategoryChip(category: String) {
    val dotColor = if (category == "관광") TourBlue else FoodOrange
    Surface(
        color = Color.White,
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, ScheduleBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(6.dp).background(dotColor, CircleShape))
            Text(category, modifier = Modifier.padding(start = 5.dp), color = ScheduleText, fontSize = 11.sp)
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun TripScheduleScreenPreview() {
    MaterialTheme { TripScheduleScreen() }
}
