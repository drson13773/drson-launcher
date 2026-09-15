package com.drson.launcher.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.drson.launcher.model.AppItem

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val speed = viewModel.currentSpeed.value

        DrivingRoadBackground(
            speedKmH = speed,
            modifier = Modifier.fillMaxSize()
        )

        CircularLuxurySpeedometer(
            speedKmH = speed,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 28.dp, bottom = 80.dp)
        )
    }
}
