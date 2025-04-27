package com.ppam.eyemovementbellapp.analyzer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ppam.eyemovementbellapp.gesture.EyeDirection
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.*

@Composable
fun EyeGestureFloatingLabel(eyeGestureAnalyzer: EyeGestureAnalyzer) {
    val eyeDirection = eyeGestureAnalyzer.currentDirection.collectAsState().value

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 32.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            shape = MaterialTheme.shapes.medium,
            shadowElevation = 4.dp
        ) {
            Text(
                text = "Direction: ${eyeDirection.name}",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}