package com.example.harudam.ui.screens


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.clickable


// ========================================
// 색상 정의
// ========================================

private val ScreenBackground = Color(0xFFF9F7F3)
private val HeaderMint = Color(0xFFBFEBD9)
private val LightMint = Color(0xFFE2F6ED)
private val MarkerMint = Color(0xFFC2EEDA)

private val DarkGreen = Color(0xFF145B44)
private val DarkBrown = Color(0xFF594437)
private val BorderGray = Color(0xFFE0E0E0)
private val TextDark = Color(0xFF202520)


// ========================================
// 일정 상세 화면
// ========================================

@Composable
fun ResultScreen(
    mapPainter: Painter? = null,
    onBackClick: () -> Unit = {}
) {
    val dates = listOf(
        "09.01 (화)",
        "09.02 (수)",
        "09.03 (목)",
        "09.04 (금)",
        "09.05 (토)"
    )
    // 날짜별 임시 장소 데이터
    val placesByDay = listOf(

        // 1일차
        listOf(
            "초량밀면" to "음식·카페",
            "코이" to "음식·카페",
            "광안리해수욕장" to "관광"
        ),

        // 2일차
        listOf(
            "AREA6" to "관광",
            "자매국밥" to "음식·카페"
        ),

        // 3일차
        emptyList(),

        // 4일차
        listOf(
            "해운대해수욕장" to "관광"
        ),

        // 5일차
        listOf(
            "부산역" to "기타"
        )
    )

    val totalDays = dates.size

    // 현재 선택된 날짜
    var selectedDayIndex by rememberSaveable {
        mutableIntStateOf(0)
    }
    var selectedPlaceIndex by rememberSaveable(selectedDayIndex) {
        mutableIntStateOf(0)
    }

    val dailyPlaces = placesByDay
        .getOrNull(selectedDayIndex)
        .orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {

            // 상단 민트색 배경
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .background(HeaderMint)
            )

            // 전체 화면 콘텐츠
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {

                Spacer(modifier = Modifier.height(16.dp))

                // 여행 제목 및 기간
                TripScheduleHeader(onBackClick = onBackClick)

                Spacer(modifier = Modifier.height(16.dp))

                // 날짜 표시 영역
                DaySelector(
                    dayNumber = selectedDayIndex + 1,
                    dateLabel = dates[selectedDayIndex],

                    canGoPrevious = selectedDayIndex > 0,
                    canGoNext = selectedDayIndex < totalDays - 1,

                    onPreviousClick = {
                        if (selectedDayIndex > 0) {
                            selectedDayIndex--
                        }
                    },

                    onNextClick = {
                        if (selectedDayIndex < totalDays - 1) {
                            selectedDayIndex++
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 지도 영역
                ScheduleMapArea(
                    mapPainter = mapPainter
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 장소 리스트

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    if (dailyPlaces.isEmpty()) {

                        EmptyScheduleCard()

                    } else {

                        dailyPlaces.forEachIndexed { index, place ->

                            SchedulePlaceCard(
                                number = index + 1,
                                placeName = place.first,
                                category = place.second,
                                isSelected = index == selectedPlaceIndex,
                                onClick = {
                                    selectedPlaceIndex = index
                                }
                            )
                        }
                    }
                }


                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ResultBackButton(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(24.dp)
            .height(40.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "‹",
            fontSize = 30.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreen
        )
    }
}
@Composable
private fun EmptyScheduleCard() {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(
            1.dp,
            BorderGray
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "등록된 장소가 없어요.",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}



// ========================================
// 상단 여행 정보
// ========================================

@Composable
private fun TripScheduleHeader( onBackClick: () -> Unit) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        ResultBackButton(onClick = onBackClick)

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = "부산여행",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = DarkGreen
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "2026.09.01 - 09.05",
                fontSize = 13.sp,
                color = DarkGreen
            )
        }
    }
}


// ========================================
// 날짜 선택 영역
// ========================================


@Composable
private fun DaySelector(
    dayNumber: Int,
    dateLabel: String,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // 이전 날짜 버튼
        ScheduleArrowButton(
            symbol = "‹",
            enabled = canGoPrevious,
            onClick = onPreviousClick
        )

        // 현재 날짜
        Column(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(LightMint),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "${dayNumber}일차",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            Text(
                text = dateLabel,
                fontSize = 13.sp,
                color = DarkGreen
            )
        }

        // 다음 날짜 버튼
        ScheduleArrowButton(
            symbol = "›",
            enabled = canGoNext,
            onClick = onNextClick
        )
    }
}



// ========================================
// 공통 화살표 버튼 UI
// ========================================


@Composable
private fun ScheduleArrowButton(
    symbol: String,
    enabled: Boolean,
    onClick: () -> Unit
) {

    Surface(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                onClick = onClick
            ),
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 2.dp
    ) {

        Box(
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = symbol,
                fontSize = 30.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) {
                    DarkGreen
                } else {
                    Color.LightGray
                }
            )
        }
    }
}



// ========================================
// 지도 영역
// ========================================

@Composable
private fun ScheduleMapArea(
    mapPainter: Painter? = null
) {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFE3E8E5),
        shadowElevation = 4.dp,
        border = BorderStroke(3.dp, Color.White)
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {

            if (mapPainter != null) {

                Image(
                    painter = mapPainter,
                    contentDescription = "여행 일정 지도",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

            } else {

                // 추후 GoogleMap으로 교체할 영역
                Text(
                    text = "Google Maps 영역",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}


// ========================================
// 개별 장소 카드
// ========================================

@Composable
private fun SchedulePlaceCard(
    number: Int,
    placeName: String,
    category: String,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {

    val cardBackground =
        if (isSelected) LightMint else Color.White

    val cardBorder =
        if (isSelected) DarkGreen else BorderGray

    val numberBackground =
        if (isSelected) DarkBrown else MarkerMint

    val numberColor =
        if (isSelected) Color.White else DarkGreen

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
        shape = RoundedCornerShape(12.dp),
        color = cardBackground,
        border = BorderStroke(
            width = 1.dp,
            color = cardBorder
        ),
        shadowElevation = if (isSelected) 0.dp else 1.dp
    ) {

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // 방문 순서 번호
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = numberBackground,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = number.toString(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = numberColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 장소 이름
            Text(
                text = placeName,
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 장소 카테고리
            PlaceCategoryTag(
                category = category
            )
        }
    }
}


// ========================================
// 장소 카테고리 태그
// ========================================

@Composable
private fun PlaceCategoryTag(
    category: String
) {

    val dotColor = when (category) {
        "관광" -> Color(0xFF5D99BF)
        "음식·카페" -> Color(0xFFE58F68)
        "숙소" -> Color(0xFF9C7EC1)
        else -> Color.Gray
    }

    Surface(
        shape = CircleShape,
        color = Color.White,
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFE9E2D9)
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 4.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // 카테고리별 원형 표시
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(
                        color = dotColor,
                        shape = CircleShape
                    )
            )

            Spacer(modifier = Modifier.width(6.dp))

            // 카테고리 이름
            Text(
                text = category,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF594A40)
            )
        }
    }
}


// ========================================
// Android Studio Preview
// ========================================

@Preview(
    showBackground = true,
    showSystemUi = true,
)
@Composable
private fun ResultScreenPreview() {
    MaterialTheme {
        ResultScreen()
    }
}
