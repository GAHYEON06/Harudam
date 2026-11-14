package com.example.harudam.ui.screens

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable

private val CreateMint = Color(0xFFA0E8CB)
private val CreateMintLight = Color(0xFFC7F9E5)
private val CreateIvory = Color(0xFFFBF9F4)
private val CreateText = Color(0xFF554239)
private val CreateSubtext = Color(0xFF7D6A60)
private val CreateBorder = Color(0xFFECE5DC)

/** Figma screen 2: 새 여행 만들기. 화면 구성만 표시하는 정적 UI입니다. */
@Composable
fun TripInfoInputScreen(onBackClick: () -> Unit = {}) {
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
                CreateTripForm()
                Spacer(Modifier.height(18.dp))
                CreateMapPlaceholder()
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "여행 장소 3",
                    color = CreateText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(10.dp))
                ImportedPlaceRow("초량밀면")
                Spacer(Modifier.height(12.dp))
                ImportedPlaceRow("코이")
                Spacer(Modifier.height(26.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 34.dp)
                        .height(47.dp)
                        .background(CreateMint, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("장소 확정하기", color = CreateText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(24.dp))
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
private fun CreateTripForm() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CreateBorder),
    ) {
        Column(modifier = Modifier.padding(horizontal = 17.dp, vertical = 12.dp)) {
            FormLabel("여행 이름")
            Spacer(Modifier.height(5.dp))
            StaticField("부산여행", modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(8.dp))
            FormLabel("날짜", modifier = Modifier.padding(start = 2.dp))
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StaticField("", modifier = Modifier.width(114.dp))
                Text("~", color = CreateText, fontSize = 15.sp)
                StaticField("", modifier = Modifier.width(118.dp))
            }

            Spacer(Modifier.height(8.dp))
            FormLabel("장소 검색", modifier = Modifier.padding(start = 2.dp))
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(43.dp)
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .padding(start = 13.dp, end = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SearchGlyph()
                Text("광안리", modifier = Modifier.padding(start = 10.dp).weight(1f), color = CreateText, fontSize = 14.sp)
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 28.dp)
                        .background(CreateMintLight, RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("추가", color = CreateText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun FormLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier.padding(start = 9.dp), color = CreateSubtext, fontSize = 12.sp)
}

@Composable
private fun StaticField(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(43.dp),
        color = CreateIvory,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CreateBorder),
    ) {
        Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
            if (text.isNotEmpty()) Text(text, color = CreateText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SearchGlyph() {
    Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.size(12.dp),
            color = Color.Transparent,
            shape = CircleShape,
            border = androidx.compose.foundation.BorderStroke(2.dp, CreateSubtext),
        ) {}
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 1.dp, bottom = 1.dp)
                .width(7.dp)
                .height(2.dp)
                .background(CreateSubtext, RoundedCornerShape(2.dp)),
        )
    }
}

@Composable
private fun CreateMapPlaceholder() {
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
            Box(contentAlignment = Alignment.Center) {
                Text("지도", color = CreateText, fontSize = 36.sp, fontWeight = FontWeight.Light)
            }
        }
    }
}

@Composable
private fun ImportedPlaceRow(name: String) {
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
        }
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
