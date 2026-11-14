package com.example.harudam.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    val name: String,
    val category: String,
)

/** Figma screen 3: 여행의 날짜별 일정을 보여주는 정적 UI입니다. */
@Composable
fun TripScheduleScreen() {
    val days = listOf(
        ScheduleDayUi(
            1,
            "09.01 (화)",
            listOf(
                SchedulePlaceUi("초량밀면", "음식·카페"),
                SchedulePlaceUi("코이", "음식·카페"),
                SchedulePlaceUi("광안리해수욕장", "관광"),
            ),
        ),
        ScheduleDayUi(
            2,
            "09.02 (수)",
            listOf(
                SchedulePlaceUi("AREA6", "관광"),
                SchedulePlaceUi("자매국밥", "음식·카페"),
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
            if (day.places.isEmpty()) {
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
                    day.places.forEachIndexed { index, place ->
                        SchedulePlaceRow(index + 1, place)
                    }
                }
            }
        }
    }
}

@Composable
private fun SchedulePlaceRow(number: Int, place: SchedulePlaceUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp)
            .background(ScheduleIvory, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(ScheduleMintLight, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(number.toString(), color = Color(0xFF4A8C76), fontSize = 12.sp)
        }
        Text(
            place.name,
            modifier = Modifier.padding(start = 12.dp).weight(1f),
            color = ScheduleText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        ScheduleCategoryChip(place.category)
        Text(
            "≡",
            modifier = Modifier.padding(start = 10.dp),
            color = ScheduleSubtext,
            fontSize = 19.sp,
            lineHeight = 19.sp,
        )
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
