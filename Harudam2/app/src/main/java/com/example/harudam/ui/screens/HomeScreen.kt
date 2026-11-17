package com.example.harudam.ui.screens

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val HarudamMint = Color(0xFFA0E8CB)
private val HarudamMintChip = Color(0x80C7F9E5)
private val HarudamIvory = Color(0xFFFBF9F4)
private val HarudamText = Color(0xFF554239)
private val HarudamSubText = Color(0xFF7D6A60)
private val HarudamBorder = Color(0xFFECE5DC)
private val HarudamWhite = Color(0xFFFFFFFF)

private data class HomeTripUi(
    val dateText: String,
    val title: String,
    val detail: String,
)

/** 홈 화면. 현재는 예시 일정으로 표시하며 삭제는 화면 내 상태에만 반영됩니다. */
@Composable
fun HomeScreen(
    onAddTripClick: () -> Unit = {},
    onTripClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    val trips = remember {
        mutableStateListOf(
            HomeTripUi("2026.09.01 ~ 09.05", "부산여행", "4박 5일  |  장소 25개"),
            HomeTripUi("2026.03.21 ~ 03.22", "강릉 가족여행", "1박 2일  |  장소 7개"),
            HomeTripUi("2025.12.27", "대전 당일치기", "1일  |  장소 6개"),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HarudamIvory),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            HomeHeader(
                onAddTripClick = onAddTripClick,
                onLogoClick = { showLogoutDialog = true },
                tripCount = trips.size,
            )

            // 헤더 아래에서 카드가 위로 겹쳐 올라오는 Figma 배치입니다.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-74).dp)
                    .padding(horizontal = 23.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                trips.forEach { trip ->
                    HomeTripCard(
                        trip = trip,
                        onClick = onTripClick,
                        onDeleteClick = { trips.remove(trip) },
                    )
                }
            }
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("로그아웃할까요?") },
                text = { Text("로그아웃하면 로그인 화면으로 이동합니다.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutDialog = false
                            onLogoutClick()
                        },
                    ) {
                        Text("로그아웃")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("취소")
                    }
                },
            )
        }
    }
}

@Composable
private fun HomeHeader(
    onAddTripClick: () -> Unit,
    onLogoClick: () -> Unit,
    tripCount: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(219.dp)
            .background(
                color = HarudamMint,
                shape = RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp),
            )
            .padding(start = 28.dp, end = 27.dp, top = 38.dp),
    ) {
        Text(
            text = "HARUDAM",
            modifier = Modifier.clickable(onClick = onLogoClick),
            color = HarudamText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "내 여행",
                modifier = Modifier.weight(1f),
                color = HarudamText,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
            )
            // Figma의 원형 추가 아이콘은 UI 모양만 그립니다.
            AddTripIcon(onClick = onAddTripClick)
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = "만든 여행 ${tripCount}개",
            color = HarudamSubText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
        )
    }
}

@Composable
private fun AddTripIcon(onClick: () -> Unit) {
    Canvas(modifier = Modifier.size(39.dp).clickable(onClick = onClick)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color = HarudamText, radius = size.minDimension / 2f, center = center)
        val inset = size.minDimension * 0.31f
        drawLine(
            color = HarudamMint,
            start = Offset(inset, center.y),
            end = Offset(size.width - inset, center.y),
            strokeWidth = 2.2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = HarudamMint,
            start = Offset(center.x, inset),
            end = Offset(center.x, size.height - inset),
            strokeWidth = 2.2.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun HomeTripCard(
    trip: HomeTripUi,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(111.dp)
            .clickable(onClick = onClick),
        color = HarudamWhite,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HarudamBorder),
        shadowElevation = 0.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp, top = 11.dp, end = 45.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(
                    modifier = Modifier
                        .background(HarudamMintChip, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = trip.dateText,
                        color = HarudamSubText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                    )
                }
                Text(
                    text = trip.title,
                    color = HarudamText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = trip.detail,
                    color = HarudamSubText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                )
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp),
            ) {
                TrashIcon(modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun TrashIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val color = HarudamText
        val stroke = 1.7.dp.toPx()
        val left = size.width * 0.24f
        val top = size.height * 0.29f
        val bodyWidth = size.width * 0.52f
        val bodyHeight = size.height * 0.57f

        drawRoundRect(
            color = color,
            topLeft = Offset(left, top),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx()),
            style = Stroke(width = stroke),
        )
        drawLine(color, Offset(size.width * 0.17f, size.height * 0.23f), Offset(size.width * 0.83f, size.height * 0.23f), stroke)
        drawLine(color, Offset(size.width * 0.39f, size.height * 0.15f), Offset(size.width * 0.61f, size.height * 0.15f), stroke)
        drawLine(color, Offset(size.width * 0.42f, size.height * 0.39f), Offset(size.width * 0.42f, size.height * 0.73f), stroke)
        drawLine(color, Offset(size.width * 0.58f, size.height * 0.39f), Offset(size.width * 0.58f, size.height * 0.73f), stroke)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen()
    }
}
