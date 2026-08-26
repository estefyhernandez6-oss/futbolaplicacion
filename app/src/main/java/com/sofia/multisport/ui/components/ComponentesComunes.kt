package com.sofia.multisport.ui.components

import com.sofia.multisport.R
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sofia.multisport.data.models.Publicidad
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SeccionPublicidad(publicidades: List<Publicidad>) {
    if (publicidades.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2CC))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📢 ¡ANUNCIA AQUÍ TU NEGOCIO!", fontWeight = FontWeight.Bold, color = Color(0xFF7F6000), fontSize = 14.sp)
                    Text("Llega a cientos de deportistas locales cada fin de semana.", color = Color.DarkGray, fontSize = 11.sp)
                }
            }
        }
        return
    }

    // Corregido con llaves {} para satisfacer el tipo '() -> Int'
    val pagerState = rememberPagerState(pageCount = { publicidades.size })

    LaunchedEffect(key1 = Unit) {
        while (true) {
            delay(timeMillis = 4000)
            val nextPage = (pagerState.currentPage + 1) % publicidades.size
            pagerState.animateScrollToPage(nextPage)
        }
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(vertical = 8.dp)
        ) { page ->
            val publi = publicidades[page]
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                AsyncImage(
                    model = publi.imagenUrl,
                    contentDescription = "Banner publicitario",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.banner_placeholder),
                    error = painterResource(id = R.drawable.banner_placeholder)
                )
            }
        }

        Row(
            Modifier
                .height(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(publicidades.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) Color(0xFFE94560) else Color.LightGray
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(8.dp)
                )
            }
        }
    }
}