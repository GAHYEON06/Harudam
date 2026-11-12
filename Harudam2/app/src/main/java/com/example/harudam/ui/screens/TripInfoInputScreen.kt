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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


// 여행 일정 추가
private val InputNavy = Color(0xFF294577)
private val InputBlue = Color(0xFF496AA5)
private val InputText = Color(0xFF252B36)
private val InputSubtext = Color(0xFF788397)
private val InputBorder = Color(0xFFE1E6EE)
private val InputBackground = Color(0xFFF7F8FA)

@Composable
fun TripInfoInputScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = InputBackground,
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
                    Text("‹", color = InputNavy, fontSize = 30.sp)
                }
                Text(
                    text = "여행일정 추가",
                    modifier = Modifier.padding(start = 12.dp),
                    color = InputText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(28.dp))
            InputFormLabel("여행명")
            Spacer(Modifier.height(8.dp))
            InputStaticField("부산여행")

            Spacer(Modifier.height(22.dp))
            InputFormLabel("날짜")
            Spacer(Modifier.height(8.dp))
            InputStaticField("2026.09.01 ~ 2026.09.17")

            Spacer(Modifier.height(22.dp))
            InputFormLabel("장소링크")
            Spacer(Modifier.height(8.dp))
            InputStaticField(
                text = "네이버 지도 장소 링크를 붙여넣으세요",
                textColor = InputSubtext,
                height = 58.dp,
            )

            Spacer(Modifier.height(18.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                color = Color.White,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, InputBorder),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "가져온 장소 미리보기",
                        color = InputSubtext,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    InputPlaceRow("봉어당")
                    InputPlaceRow("면식당")
                    InputPlaceRow("슬로우브랜드")
                }
            }

            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(InputNavy, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "장소 확정하기",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun InputFormLabel(text: String) {
    Text(
        text = text,
        color = InputText,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun InputStaticField(
    text: String,
    textColor: Color = InputText,
    height: Dp = 54.dp,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        color = Color.White,
        shape = RoundedCornerShape(11.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, InputBorder),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 15.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(text = text, color = textColor, fontSize = 15.sp)
        }
    }
}

@Composable
private fun InputPlaceRow(name: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9FAFC), RoundedCornerShape(9.dp))
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("⌖", color = InputBlue, fontSize = 17.sp)
        Spacer(Modifier.width(9.dp))
        Text(name, color = InputText, fontSize = 14.sp)
    }
}

@Preview(showBackground = true)
@Composable
private fun TripInfoInputScreenPreview() {
    MaterialTheme {
        TripInfoInputScreen()
    }
}