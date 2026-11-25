
package com.example.harudam.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex


// ========================================
// 색상 정의
// ========================================

private val ScheduleMint = Color(0xFFA0E8CB)
private val ScheduleMintLight = Color(0xFFC7F9E5)
private val ScheduleIvory = Color(0xFFFBF9F4)
private val ScheduleText = Color(0xFF554239)
private val ScheduleSubtext = Color(0xFF7D6A60)
private val ScheduleBorder = Color(0xFFECE5DC)

private val ScheduleGreen = Color(0xFF4A8C76)
private val SelectorBackground = Color(0xFFF1F8F4)

private val FoodOrange = Color(0xFFEE8A5F)
private val TourBlue = Color(0xFF4C9BC4)
private val StayPurple = Color(0xFF9C7EC1)


// ========================================
// UI 데이터 클래스
// ========================================

private data class TripScheduleDayUi(
    val dayNumber: Int,
    val dateLabel: String
)

private data class TripSchedulePlaceUi(
    val id: String,
    val name: String,
    val category: String
)


// ========================================
// 일정 정리 화면
// ========================================

@Composable
fun TripInfoInputScreen(
    onBackClick: () -> Unit = {},
    onConfirmClick: () -> Unit = {}
) {

    // 테스트용 여행 날짜
    val days = remember {
        listOf(
            TripScheduleDayUi(1, "09.01 (화)"),
            TripScheduleDayUi(2, "09.02 (수)"),
            TripScheduleDayUi(3, "09.03 (목)"),
            TripScheduleDayUi(4, "09.04 (금)"),
            TripScheduleDayUi(5, "09.05 (토)")
        )
    }

    // 테스트용 URL 파싱 결과
    val importedPlaces = remember {
        listOf(
            TripSchedulePlaceUi(
                "place-1", "초량밀면", "음식·카페"
            ),
            TripSchedulePlaceUi(
                "place-2", "코이", "음식·카페"
            ),
            TripSchedulePlaceUi(
                "place-3", "광안리해수욕장", "관광"
            ),
            TripSchedulePlaceUi(
                "place-4", "AREA6", "관광"
            ),
            TripSchedulePlaceUi(
                "place-5", "자매국밥", "음식·카페"
            )
        )
    }

    // 모든 날짜의 장소 목록을 빈 상태로 초기화
    val placesByDay = remember {
        days.associate { day ->
            day.dayNumber to
                    mutableStateListOf<TripSchedulePlaceUi>()
        }
    }

    // 아직 날짜에 배정되지 않은 장소
    val availablePlaces = importedPlaces.filter { candidate ->
        placesByDay.values.none { places ->
            places.any { it.id == candidate.id }
        }
    }

    // 배정된 장소 개수
    val assignedPlaceCount =
        importedPlaces.size - availablePlaces.size

    // 최초에는 1일차만 공개
    var visibleDayCount by remember {
        mutableIntStateOf(1)
    }

    // 장소 선택 영역이 열려 있는 날짜
    var expandedDay by remember {
        mutableStateOf<Int?>(null)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScheduleIvory
    ) {

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            // 스크롤 가능한 일정 영역
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    ScheduleHeader(
                        onBackClick = onBackClick
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 21.dp,
                                end = 21.dp,
                                top = 121.dp,
                                bottom = 20.dp
                            ),
                        verticalArrangement =
                            Arrangement.spacedBy(20.dp)
                    ) {

                        // 일정 배정 현황
                        ScheduleAssignmentProgress(
                            assignedCount = assignedPlaceCount,
                            totalCount = importedPlaces.size
                        )

                        // 공개된 날짜만 화면에 출력
                        days.take(visibleDayCount).forEach { day ->

                            key(day.dayNumber) {

                                ScheduleDayCard(
                                    day = day,

                                    places = placesByDay.getValue(
                                        day.dayNumber
                                    ),

                                    availablePlaces = availablePlaces,

                                    isExpanded =
                                        expandedDay == day.dayNumber,

                                    // 장소 추가 영역 열기 및 닫기
                                    onAddClick = {
                                        expandedDay =
                                            if (expandedDay == day.dayNumber) {
                                                null
                                            } else {
                                                day.dayNumber
                                            }
                                    },

                                    // 장소 선택
                                    onPlaceSelected = { place ->

                                        val alreadyAssigned =
                                            placesByDay.values.any { list ->
                                                list.any {
                                                    it.id == place.id
                                                }
                                            }

                                        if (!alreadyAssigned) {

                                            placesByDay
                                                .getValue(day.dayNumber)
                                                .add(place)

                                            // 다음 날짜 자동 공개
                                            if (
                                                day.dayNumber == visibleDayCount &&
                                                visibleDayCount < days.size
                                            ) {
                                                visibleDayCount++
                                            }
                                        }
                                    },

                                    // 장소 배정 취소
                                    onPlaceRemoved = { place ->

                                        placesByDay
                                            .getValue(day.dayNumber)
                                            .remove(place)
                                    },

                                    // 장소 선택 영역 닫기
                                    onCloseSelector = {
                                        expandedDay = null
                                    }
                                )
                            }
                        }

                        // 다음 날짜 수동 공개
                        if (visibleDayCount < days.size) {

                            OpenNextDayButton(
                                nextDayNumber = visibleDayCount + 1,
                                onClick = {
                                    visibleDayCount++
                                    expandedDay = null
                                }
                            )
                        }
                    }
                }
            }

            // 하단 고정 버튼
            ScheduleConfirmButton(
                onClick = onConfirmClick
            )
        }
    }
}


// ========================================
// 상단 헤더
// ========================================

@Composable
private fun ScheduleHeader(
    onBackClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(161.dp)
            .background(
                ScheduleMint,
                RoundedCornerShape(
                    bottomStart = 10.dp,
                    bottomEnd = 10.dp
                )
            )
            .padding(
                start = 16.dp,
                end = 20.dp,
                top = 32.dp
            )
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "‹",
                modifier = Modifier.clickable(
                    onClick = onBackClick
                ),
                color = ScheduleText,
                fontSize = 27.sp,
                lineHeight = 27.sp
            )

            Text(
                text = "일정 정리",
                modifier = Modifier.padding(start = 9.dp),
                color = ScheduleText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "부산여행 | 2026.09.01 ~ 09.05",
            modifier = Modifier.padding(
                start = 25.dp,
                top = 7.dp
            ),
            color = ScheduleText,
            fontSize = 14.sp
        )
    }
}


// ========================================
// 일정 배정 진행률
// ========================================

@Composable
private fun ScheduleAssignmentProgress(
    assignedCount: Int,
    totalCount: Int
) {

    val progress = if (totalCount > 0) {
        (assignedCount.toFloat() / totalCount)
            .coerceIn(0f, 1f)
    } else {
        0f
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, ScheduleBorder),
        shadowElevation = 2.dp
    ) {

        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 14.dp
            )
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "일정 배정 현황",
                    modifier = Modifier.weight(1f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ScheduleText
                )

                Text(
                    text = "$assignedCount / ${totalCount}개 장소",
                    fontSize = 12.sp,
                    color = ScheduleSubtext
                )
            }

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5E4DD))
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(
                            Color(0xFF66B99A),
                            CircleShape
                        )
                )
            }
        }
    }
}


// ========================================
// 날짜별 일정 카드
// ========================================

@Composable
private fun ScheduleDayCard(
    day: TripScheduleDayUi,
    places: SnapshotStateList<TripSchedulePlaceUi>,
    availablePlaces: List<TripSchedulePlaceUi>,
    isExpanded: Boolean,
    onAddClick: () -> Unit,
    onPlaceSelected: (TripSchedulePlaceUi) -> Unit,
    onPlaceRemoved: (TripSchedulePlaceUi) -> Unit,
    onCloseSelector: () -> Unit
) {

    var draggedIndex by remember(day.dayNumber) {
        mutableStateOf<Int?>(null)
    }

    var dragOffsetY by remember(day.dayNumber) {
        mutableFloatStateOf(0f)
    }

    val density = LocalDensity.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, ScheduleBorder),
        shadowElevation = 3.dp
    ) {

        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 12.dp
            )
        ) {

            // 일차 및 날짜 정보
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "${day.dayNumber}일차",
                    color = ScheduleText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = day.dateLabel,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .weight(1f),
                    color = ScheduleSubtext,
                    fontSize = 12.sp
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(ScheduleMintLight)
                        .clickable(onClick = onAddClick)
                        .padding(
                            horizontal = 12.dp,
                            vertical = 7.dp
                        )
                ) {

                    Text(
                        text = if (isExpanded) {
                            "− 닫기"
                        } else {
                            "+ 추가"
                        },
                        color = ScheduleText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 장소가 없는 날짜
            if (places.isEmpty() && !isExpanded) {

                Spacer(Modifier.height(9.dp))

                EmptyDayPlace()
            }

            // 배정된 장소 목록
            if (places.isNotEmpty()) {

                Spacer(Modifier.height(9.dp))

                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    places.forEachIndexed { index, place ->

                        key(place.id) {

                            SchedulePlaceRow(
                                number = index + 1,
                                place = place,

                                isDragging =
                                    draggedIndex == index,

                                dragOffsetY = dragOffsetY,

                                onRemoveClick = {
                                    onPlaceRemoved(place)
                                },

                                onDragStart = {
                                    draggedIndex = index
                                    dragOffsetY = 0f
                                },

                                onDragDelta = { deltaY ->

                                    val currentIndex = draggedIndex

                                    if (
                                        currentIndex != null &&
                                        currentIndex in places.indices
                                    ) {

                                        val rowStepPx = with(density) {
                                            (45.dp + 12.dp).toPx()
                                        }

                                        var remainingOffset =
                                            dragOffsetY + deltaY

                                        var targetIndex =
                                            currentIndex

                                        // 아래 방향 이동
                                        while (
                                            remainingOffset >
                                            rowStepPx / 2f &&
                                            targetIndex <
                                            places.lastIndex
                                        ) {
                                            targetIndex++
                                            remainingOffset -=
                                                rowStepPx
                                        }

                                        // 위 방향 이동
                                        while (
                                            remainingOffset <
                                            -rowStepPx / 2f &&
                                            targetIndex > 0
                                        ) {
                                            targetIndex--
                                            remainingOffset +=
                                                rowStepPx
                                        }

                                        // 방문 순서 변경
                                        if (
                                            targetIndex != currentIndex
                                        ) {

                                            val movedPlace =
                                                places.removeAt(
                                                    currentIndex
                                                )

                                            places.add(
                                                targetIndex,
                                                movedPlace
                                            )

                                            draggedIndex = targetIndex
                                        }

                                        dragOffsetY = remainingOffset
                                            .coerceIn(
                                                -rowStepPx / 2f,
                                                rowStepPx / 2f
                                            )
                                    }
                                },

                                onDragFinished = {
                                    draggedIndex = null
                                    dragOffsetY = 0f
                                }
                            )
                        }
                    }
                }
            }

            // 인라인 장소 선택 영역
            if (isExpanded) {

                Spacer(Modifier.height(12.dp))

                InlinePlaceSelector(
                    dayNumber = day.dayNumber,
                    availablePlaces = availablePlaces,
                    onPlaceSelected = onPlaceSelected,
                    onClose = onCloseSelector
                )
            }
        }
    }
}


// ========================================
// 인라인 장소 선택 영역
// ========================================

@Composable
private fun InlinePlaceSelector(
    dayNumber: Int,
    availablePlaces: List<TripSchedulePlaceUi>,
    onPlaceSelected: (TripSchedulePlaceUi) -> Unit,
    onClose: () -> Unit
) {

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SelectorBackground,
        shape = RoundedCornerShape(10.dp)
    ) {

        Column(
            modifier = Modifier.padding(12.dp)
        ) {

            Text(
                text = "${dayNumber}일차 장소 추가",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = ScheduleText
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "미배정 장소 ${availablePlaces.size}개",
                fontSize = 12.sp,
                color = ScheduleSubtext
            )

            Spacer(Modifier.height(12.dp))

            if (availablePlaces.isEmpty()) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "모든 장소가 배정되었어요.",
                        fontSize = 12.sp,
                        color = ScheduleSubtext
                    )
                }

            } else {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    availablePlaces.forEach { place ->

                        key(place.id) {

                            AvailablePlaceRow(
                                place = place,
                                onClick = {
                                    onPlaceSelected(place)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "장소 선택 완료",
                    color = ScheduleGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}


// ========================================
// 미배정 장소 선택 항목
// ========================================

@Composable
private fun AvailablePlaceRow(
    place: TripSchedulePlaceUi,
    onClick: () -> Unit
) {

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        border = BorderStroke(1.dp, ScheduleBorder)
    ) {

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = place.name,
                modifier = Modifier.weight(1f),
                color = ScheduleText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            ScheduleCategoryChip(
                category = place.category
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = "+",
                fontSize = 20.sp,
                color = ScheduleGreen,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


// ========================================
// 배정된 장소 항목
// ========================================

@Composable
private fun SchedulePlaceRow(
    number: Int,
    place: TripSchedulePlaceUi,
    isDragging: Boolean,
    dragOffsetY: Float,
    onRemoveClick: () -> Unit,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragFinished: () -> Unit
) {

    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragDelta by rememberUpdatedState(onDragDelta)
    val currentOnDragFinished by rememberUpdatedState(onDragFinished)

    Row(
        modifier = Modifier
            .graphicsLayer {

                translationY =
                    if (isDragging) dragOffsetY else 0f

                shadowElevation =
                    if (isDragging) 8.dp.toPx() else 0f

                shape = RoundedCornerShape(10.dp)
            }
            .zIndex(if (isDragging) 1f else 0f)
            .fillMaxWidth()
            .height(45.dp)
            .background(
                ScheduleIvory,
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        PlaceOrderBadge(number)

        Text(
            text = place.name,
            modifier = Modifier
                .padding(start = 9.dp, end = 4.dp)
                .weight(1f),
            color = ScheduleText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        ScheduleCategoryChip(
            category = place.category
        )

        // 장소 배정 취소 버튼
        IconButton(
            onClick = onRemoveClick,
            enabled = !isDragging,
            modifier = Modifier
                .size(32.dp)
                .semantics {
                    contentDescription = "${place.name} 배정 취소"
                }
        ) {

            ScheduleRemoveIcon(
                modifier = Modifier.size(15.dp)
            )
        }

        // 드래그 핸들
        Box(
            modifier = Modifier
                .size(
                    width = 24.dp,
                    height = 40.dp
                )
                .pointerInput(place.id) {

                    detectDragGesturesAfterLongPress(

                        onDragStart = {
                            currentOnDragStart()
                        },

                        onDrag = { change, dragAmount ->
                            change.consume()
                            currentOnDragDelta(dragAmount.y)
                        },

                        onDragEnd = {
                            currentOnDragFinished()
                        },

                        onDragCancel = {
                            currentOnDragFinished()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = "≡",
                color = ScheduleSubtext,
                fontSize = 19.sp,
                lineHeight = 19.sp
            )
        }
    }
}


// ========================================
// 장소 배정 취소 아이콘
// ========================================

@Composable
private fun ScheduleRemoveIcon(
    modifier: Modifier = Modifier
) {

    Canvas(modifier = modifier) {

        val stroke = 1.5.dp.toPx()

        val w = size.width
        val h = size.height

        // 휴지통 몸체
        drawRect(
            color = ScheduleSubtext,
            topLeft = Offset(w * 0.23f, h * 0.3f),
            size = Size(w * 0.54f, h * 0.6f),
            style = Stroke(width = stroke)
        )

        // 휴지통 뚜껑
        drawLine(
            color = ScheduleSubtext,
            start = Offset(w * 0.12f, h * 0.3f),
            end = Offset(w * 0.88f, h * 0.3f),
            strokeWidth = stroke
        )

        // 뚜껑 손잡이
        drawLine(
            color = ScheduleSubtext,
            start = Offset(w * 0.36f, h * 0.13f),
            end = Offset(w * 0.64f, h * 0.13f),
            strokeWidth = stroke
        )

        // 내부 왼쪽 선
        drawLine(
            color = ScheduleSubtext,
            start = Offset(w * 0.42f, h * 0.43f),
            end = Offset(w * 0.42f, h * 0.76f),
            strokeWidth = stroke
        )

        // 내부 오른쪽 선
        drawLine(
            color = ScheduleSubtext,
            start = Offset(w * 0.58f, h * 0.43f),
            end = Offset(w * 0.58f, h * 0.76f),
            strokeWidth = stroke
        )
    }
}


// ========================================
// 방문 순서 번호
// ========================================

@Composable
private fun PlaceOrderBadge(
    number: Int
) {

    Box(
        modifier = Modifier
            .size(20.dp)
            .background(
                ScheduleMintLight,
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = number.toString(),
            color = ScheduleGreen,
            fontSize = 12.sp
        )
    }
}


// ========================================
// 장소 카테고리
// ========================================

@Composable
private fun ScheduleCategoryChip(
    category: String
) {

    val dotColor = when (category) {
        "관광" -> TourBlue
        "음식·카페" -> FoodOrange
        "숙소" -> StayPurple
        else -> Color.Gray
    }

    Surface(
        color = Color.White,
        shape = CircleShape,
        border = BorderStroke(1.dp, ScheduleBorder)
    ) {

        Row(
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 3.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(dotColor, CircleShape)
            )

            Text(
                text = category,
                modifier = Modifier.padding(start = 5.dp),
                color = ScheduleText,
                fontSize = 11.sp
            )
        }
    }
}


// ========================================
// 빈 일정 표시
// ========================================

@Composable
private fun EmptyDayPlace() {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp),
        color = Color.White,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, ScheduleBorder)
    ) {

        Box(
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = "아직 장소가 없어요",
                color = ScheduleSubtext,
                fontSize = 12.sp
            )
        }
    }
}


// ========================================
// 다음 날짜 수동 공개 버튼
// ========================================

@Composable
private fun OpenNextDayButton(
    nextDayNumber: Int,
    onClick: () -> Unit
) {

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        color = ScheduleIvory,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.dp,
            Color(0xFFA0CCB8)
        )
    ) {

        Box(
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = "+ ${nextDayNumber}일차 일정 열기",
                color = ScheduleGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}


// ========================================
// 일정 확정하기 버튼
// ========================================

@Composable
private fun ScheduleConfirmButton(
    onClick: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ScheduleIvory)
            .navigationBarsPadding()
            .padding(
                horizontal = 21.dp,
                vertical = 16.dp
            )
    ) {

        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ScheduleMint,
                contentColor = ScheduleText
            )
        ) {

            Text(
                text = "일정 확정하기",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
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
    device = "spec:width=393dp,height=852dp"
    
)
@Composable
private fun TripInfoInputScreenPreview() {

    MaterialTheme {
        TripInfoInputScreen()
    }
}
