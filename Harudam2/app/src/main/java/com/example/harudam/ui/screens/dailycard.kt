package com.example.harudam.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ItineraryNavy = Color(0xFF294577)
private val ItineraryBlue = Color(0xFF496AA5)
private val ItineraryText = Color(0xFF252B36)
private val ItinerarySubtext = Color(0xFF788397)
private val ItineraryBorder = Color(0xFFE1E6EE)
private val ItineraryBackground = Color(0xFFF7F8FA)
private val InputNavy = Color(0xFF294577)

private data class ItineraryPlacePreview(
    val name: String,
    val memo: String,
)

/** 장소 확정 뒤 표시되는 날짜별 일정 화면의 정적 UI 초안입니다. */
@Composable
fun TripItineraryScreen() {
    val places = listOf(
        ItineraryPlacePreview("봉어당", "점심 식사"),
        ItineraryPlacePreview("면식당", "웨이팅 확인"),
        ItineraryPlacePreview("슬로우브랜드", "기념품 구경"),
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ItineraryBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 18.dp, bottom = 24.dp),
        ) {
            Text(
                text = "2026.09.01 ~ 2026.09.03",
                color = ItinerarySubtext,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(4.dp))

            Row()
            {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("‹", color = InputNavy, fontSize = 30.sp)
                }

                Spacer(Modifier.width(18.dp))

                Text(
                text = "부산여행",
                color = ItineraryText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold, )


            }


            Spacer(Modifier.height(50.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DayArrow("‹")
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(9.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ItineraryBorder),
                ) {
                    Text(
                        text = "1일차",
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 9.dp),
                        color = ItineraryNavy,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                DayArrow("›")
            }

            Spacer(Modifier.height(12.dp))
            MapPlaceholder(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            )

            Spacer(Modifier.height(14.dp))
            places.forEachIndexed { index, place ->
                ItineraryPlaceCard(index + 1, place)
                Spacer(Modifier.height(9.dp))
            }
        }
    }
}

@Composable
private fun DayArrow(symbol: String) {
    Box(
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .size(36.dp)
            .background(Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = ItineraryBlue, fontSize = 25.sp)
    }
}

/** 지도 API 없이 도로·공원·수변·장소 마커만 표현한 목업입니다. */
@Composable
private fun MapPlaceholder(modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFEAF0F4), RoundedCornerShape(16.dp)),
        ) {
            val w = size.width
            val h = size.height

            // 공원과 수변 구역
            drawRoundRect(
                color = Color(0xFFD9E8D8),
                topLeft = Offset(w * 0.68f, h * 0.06f),
                size = Size(w * 0.25f, h * 0.25f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f),
            )
            val water = Path().apply {
                moveTo(w * 0.00f, h * 0.76f)
                cubicTo(w * 0.19f, h * 0.68f, w * 0.28f, h * 0.86f, w * 0.47f, h * 0.78f)
                cubicTo(w * 0.62f, h * 0.72f, w * 0.77f, h * 0.85f, w, h * 0.73f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(water, color = Color(0xFFD8E8F1))

            // 건물 블록
            val blocks = listOf(
                floatArrayOf(.08f, .10f, .18f, .16f),
                floatArrayOf(.34f, .08f, .20f, .19f),
                floatArrayOf(.11f, .39f, .20f, .18f),
                floatArrayOf(.39f, .38f, .19f, .20f),
                floatArrayOf(.73f, .40f, .17f, .18f),
                floatArrayOf(.60f, .13f, .08f, .17f),
            )
            blocks.forEach { block ->
                drawRoundRect(
                    color = Color(0xFFF9FAF8),
                    topLeft = Offset(w * block[0], h * block[1]),
                    size = Size(w * block[2], h * block[3]),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
                )
            }

            // 지도 도로망: 경로선 하나가 아니라 구역을 나누는 여러 도로
            val roadColor = Color(0xFFFFFFFF)
            val roadWidth = 13f
            listOf(.30f, .64f).forEach { x ->
                drawLine(roadColor, Offset(w * x, 0f), Offset(w * (x - .08f), h * .72f), roadWidth)
            }
            listOf(.31f, .61f).forEach { y ->
                drawLine(roadColor, Offset(0f, h * y), Offset(w, h * (y - .04f)), roadWidth)
            }
            drawLine(roadColor, Offset(w * .02f, h * .58f), Offset(w * .95f, h * .20f), 9f)
            drawLine(Color(0xFFC4D2DC), Offset(w * .02f, h * .58f), Offset(w * .95f, h * .20f), 2f)

            // 주요 도로 테두리
            drawLine(Color(0xFFD4DDE3), Offset(w * .30f, 0f), Offset(w * .22f, h * .72f), 1.5f)
            drawLine(Color(0xFFD4DDE3), Offset(w * .64f, 0f), Offset(w * .56f, h * .72f), 1.5f)
            drawLine(Color(0xFFD4DDE3), Offset(0f, h * .31f), Offset(w, h * .27f), 1.5f)
            drawLine(Color(0xFFD4DDE3), Offset(0f, h * .61f), Offset(w, h * .57f), 1.5f)
        }

        MapMarker("1", Modifier.offset(x = maxWidth * .19f, y = maxHeight * .37f))
        MapMarker("2", Modifier.offset(x = maxWidth * .48f, y = maxHeight * .48f))
        MapMarker("3", Modifier.offset(x = maxWidth * .72f, y = maxHeight * .24f))
    }
}

@Composable
private fun MapMarker(number: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(31.dp)
            .background(ItineraryNavy, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(number, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ItineraryPlaceCard(number: Int, place: ItineraryPlacePreview) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(13.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ItineraryBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(31.dp)
                    .background(Color(0xFFEEF3FB), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(number.toString(), color = ItineraryBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(place.name, color = ItineraryText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(place.memo, color = ItinerarySubtext, fontSize = 12.sp)
            }
            Surface(
                color = Color(0xFFF4F6F9),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = "메모",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    color = ItineraryBlue,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TripItineraryScreenPreview() {
    MaterialTheme {
        TripItineraryScreen()
    }
}
