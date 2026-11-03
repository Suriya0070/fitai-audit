package com.jsf.app.pravin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsf.app.pravin.ui.theme.*

@Composable
fun MetricCard(
    label: String,
    value: String,
    unit: String = "",
    color: Color = CyanPrimary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(LightCard, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 11.sp, color = TextGray)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
            if (unit.isNotEmpty()) {
                Spacer(Modifier.width(2.dp))
                Text(
                    unit,
                    fontSize = 12.sp,
                    color = TextGray,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
}
