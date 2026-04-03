package com.omersusin.sealora.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.omersusin.sealora.ui.util.shimmerEffect

@Composable
fun LoadingWeatherCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clip(RoundedCornerShape(24.dp))
                .shimmerEffect()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(5) {
                Box(
                    modifier = Modifier
                        .size(70.dp, 100.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .shimmerEffect()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        repeat(5) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun LoadingChatBubble(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .shimmerEffect()
        )
    }
}
