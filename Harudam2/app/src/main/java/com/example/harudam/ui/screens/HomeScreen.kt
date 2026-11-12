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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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


private val InputTextColor = Color(0xFF252B36)
private val HarudamNavy = Color(0xFF294577)
private val HarudamBlue = Color(0xFF496AA5)
private val HarudamPaleBlue = Color(0xFFEEF3FB)
private val HarudamText = Color(0xFF252B36)
private val HarudamSubtext = Color(0xFF788397)
private val HarudamBorder = Color(0xFFE1E6EE)
private val HarudamBackground = Color(0xFFF7F8FA)

private data class TripCardData(
    val title: String,
    val dateRange: String,
)

@Composable
fun HarudamHomeScreen() {
    val trips = listOf(
        TripCardData("부산여행", "2026.01.01 - 01.03"),
        TripCardData("서울여행", "2026.02.10 - 02.12"),
        TripCardData("대전여행", "2026.03.05 - 03.07"),
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = HarudamBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 22.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "여행 목록",
                        color = HarudamText,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // 화면 모양만 확인하는 초안이므로 버튼 동작은 연결하지 않았습니다.
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(HarudamPaleBlue, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "+",
                        color = HarudamBlue,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Light,
                    )
                }
            }

            Spacer(Modifier.height(26.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(trips) { trip ->
                    TripCard(trip)
                }
            }
        }
    }
}

@Composable
private fun TripCard(trip: TripCardData) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HarudamBorder),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 15.dp, end = 14.dp, bottom = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    color = HarudamPaleBlue,
                    shape = RoundedCornerShape(7.dp),
                ) {
                    Text(
                        text = trip.dateRange,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        color = HarudamBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(Modifier.height(9.dp))
                Text(
                    text = trip.title,
                    color = HarudamText,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.width(12.dp))

            // PDF에 그려진 카드별 삭제 아이콘의 정적 표현
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color(0xFFF5F6F8), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("×", color = HarudamSubtext, fontSize = 23.sp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HarudamHomeScreenPreview() {
    MaterialTheme {
        HarudamHomeScreen()
    }
}