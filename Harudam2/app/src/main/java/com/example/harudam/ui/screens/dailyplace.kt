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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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

private val DayScreenNavy = Color(0xFF294577)
private val DayScreenBlue = Color(0xFF496AA5)
private val DayScreenPaleBlue = Color(0xFFEEF3FB)
private val DayScreenText = Color(0xFF252B36)
private val DayScreenSubtext = Color(0xFF788397)
private val DayScreenBorder = Color(0xFFE1E6EE)
private val DayScreenBackground = Color(0xFFF7F8FA)

private data class DayPlacesPreview(
    val day: Int,
    val places: List<String>,
)

/** 날짜별로 배정된 장소 목록을 보여주는 정적 UI 초안입니다. */
@Composable
fun DatePlaceSelectionScreen() {
    val dayGroups = listOf(
        DayPlacesPreview(1, listOf("봉어당", "면식당")),
        DayPlacesPreview(2, listOf("슬로우브랜드", "광안리 해변")),
        DayPlacesPreview(3, emptyList()),
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DayScreenBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("‹", color = DayScreenNavy, fontSize = 30.sp)
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = "부산여행",
                        color = DayScreenText,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "2026.09.01 ~ 2026.09.03",
                        color = DayScreenSubtext,
                        fontSize = 13.sp,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            dayGroups.forEach { group ->
                DayPlaceGroup(group)
                Spacer(Modifier.height(18.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(DayScreenNavy, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "확정하기",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun DayPlaceGroup(group: DayPlacesPreview) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${group.day}일차",
                color = DayScreenText,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Surface(
                color = DayScreenPaleBlue,
                shape = RoundedCornerShape(9.dp),
            ) {
                Text(
                    text = "추가하기",
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                    color = DayScreenBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(Modifier.height(9.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DayScreenBorder),
        ) {
            if (group.places.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(78.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "아직 담은 장소가 없습니다",
                        color = DayScreenSubtext,
                        fontSize = 14.sp,
                    )
                }
            } else {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    group.places.forEachIndexed { index, place ->
                        DayPlaceRow(number = index + 1, name = place)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayPlaceRow(number: Int, name: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFAFBFD), RoundedCornerShape(9.dp))
            .padding(start = 10.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = number.toString(),
            color = DayScreenBlue,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = name,
            modifier = Modifier.weight(1f),
            color = DayScreenText,
            fontSize = 14.sp,
        )
        Text("×", color = DayScreenSubtext, fontSize = 20.sp)
    }
}

@Preview(showBackground = true)
@Composable
private fun DatePlaceSelectionScreenPreview() {
    MaterialTheme {
        DatePlaceSelectionScreen()
    }
}
