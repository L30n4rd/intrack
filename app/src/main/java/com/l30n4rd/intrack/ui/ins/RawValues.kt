package com.l30n4rd.intrack.ui.ins

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import java.text.DecimalFormat
import androidx.compose.ui.unit.dp

@Composable
fun RawPositionValues(
    modifier: Modifier,
    position: FloatArray
) {
    val decimalFormat = DecimalFormat("0.00")

    Row(
        modifier = modifier
    ) {
        Text(
            color = MaterialTheme.colorScheme.primary,
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("Translation Vector:")
                }
                append("\n")

                append("X: ${position[12].let { decimalFormat.format(it) } ?: "N/A"}")
                append("\n")

                append("Y: ${position[13].let { decimalFormat.format(it) } ?: "N/A"}")
                append("\n")

                append("Z: ${position[14].let { decimalFormat.format(it) } ?: "N/A"}")
                append("\n")
            }
        )
        Spacer(modifier = Modifier.width(32.dp))
        Text(
            color = MaterialTheme.colorScheme.primary,
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("Rotation Matrix:")
                }
                append("\n")

                // Displaying the rotation matrix
                position.let {
                    for (row in 0..2) {
                        for (column in 0..2) {
                            append(decimalFormat.format(it[column * 4 + row]))
                            append("\t\t")
                        }
                        append("\n")
                    }
                }
            }
        )
    }
}
