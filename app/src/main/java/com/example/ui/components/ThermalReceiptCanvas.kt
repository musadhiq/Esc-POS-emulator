package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.barcode.BarcodeEncoder
import com.example.escpos.EscPosElement
import com.example.escpos.HriPosition
import com.example.escpos.ParsedReceipt
import com.example.escpos.PrintAlignment
import com.example.escpos.PrintFont
import com.example.escpos.TextStyle
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.ThermalCutBorder
import com.example.ui.theme.ThermalInk
import com.example.ui.theme.ThermalPaperBg

/**
 * Authentic Thermal Paper Roll Rendering Component.
 * Supports 80mm (~576px/380dp) and 58mm (~384px/280dp) widths.
 */
@Composable
fun ThermalReceiptCanvas(
    receipt: ParsedReceipt,
    paperWidthMm: Int = 80,
    modifier: Modifier = Modifier
) {
    val maxPaperWidth = if (paperWidthMm == 58) 300.dp else 390.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = maxPaperWidth)
                .fillMaxWidth()
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .background(ThermalPaperBg)
                .testTag("thermal_receipt_paper")
        ) {
            // Serrated Perforated Top Edge
            PaperTearEdge(isTop = true)

            // Receipt Content Elements
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                if (receipt.elements.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "(Empty Print Job)",
                            color = Slate400,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    receipt.elements.forEachIndexed { index, element ->
                        RenderEscPosElement(
                            element = element,
                            paperWidthMm = paperWidthMm,
                            isLast = index == receipt.elements.lastIndex
                        )
                    }
                }
            }

            // Serrated Perforated Bottom Edge
            PaperTearEdge(isTop = false)
        }
    }
}

@Composable
private fun RenderEscPosElement(
    element: EscPosElement,
    paperWidthMm: Int,
    isLast: Boolean
) {
    when (element) {
        is EscPosElement.Text -> {
            RenderReceiptText(element)
        }

        is EscPosElement.FeedLines -> {
            Spacer(modifier = Modifier.height((element.count * 14).dp))
        }

        is EscPosElement.FeedDots -> {
            Spacer(modifier = Modifier.height((element.dots / 2).coerceAtLeast(2).dp))
        }

        is EscPosElement.Cut -> {
            PaperCutIndicator(isPartial = element.isPartial)
        }

        is EscPosElement.HorizontalDivider -> {
            RenderDivider(element)
        }

        is EscPosElement.Barcode -> {
            RenderBarcodeElement(element)
        }

        is EscPosElement.QrCode -> {
            RenderQrCodeElement(element)
        }

        is EscPosElement.RasterImage -> {
            RenderRasterImageElement(element)
        }

        is EscPosElement.DrawerKick -> {
            // Visual metadata tag for cash drawer pulse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFEF3C7), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "⚡ CASH DRAWER PULSE (${element.onTimeMs}ms)",
                        color = Color(0xFFB45309),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        is EscPosElement.SoundBuzzer -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE0E7FF), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "\uD83D\uDD14 BUZZER ALARM (${element.count}x)",
                        color = Color(0xFF4338CA),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        is EscPosElement.CommandLog -> {
            // Logs are rendered in inspector view, not directly on thermal paper
        }
    }
}

@Composable
private fun RenderReceiptText(element: EscPosElement.Text) {
    val style = element.style
    val baseFontSize = when (style.font) {
        PrintFont.FONT_A -> 13.sp
        PrintFont.FONT_B -> 11.sp
        PrintFont.FONT_C -> 9.5.sp
    }

    val scaledFontSize = (baseFontSize.value * (1f + (style.heightScale - 1) * 0.45f)).sp
    val textAlign = when (element.alignment) {
        PrintAlignment.LEFT -> TextAlign.Start
        PrintAlignment.CENTER -> TextAlign.Center
        PrintAlignment.RIGHT -> TextAlign.End
    }

    val textDecoration = when (style.underline) {
        1, 2 -> TextDecoration.Underline
        else -> TextDecoration.None
    }

    val fontWeight = if (style.bold || style.doubleStrike) FontWeight.Bold else FontWeight.Normal

    val textColor = if (style.inverted) ThermalPaperBg else ThermalInk
    val bgColor = if (style.inverted) ThermalInk else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = (style.heightScale * 0.7).dp),
        contentAlignment = when (element.alignment) {
            PrintAlignment.LEFT -> Alignment.CenterStart
            PrintAlignment.CENTER -> Alignment.Center
            PrintAlignment.RIGHT -> Alignment.CenterEnd
        }
    ) {
        Text(
            text = element.text,
            color = textColor,
            fontSize = scaledFontSize,
            fontWeight = fontWeight,
            fontFamily = FontFamily.Monospace,
            textAlign = textAlign,
            textDecoration = textDecoration,
            lineHeight = (scaledFontSize.value * 1.25f).sp,
            modifier = Modifier
                .background(bgColor, if (style.inverted) RoundedCornerShape(2.dp) else RoundedCornerShape(0.dp))
                .padding(
                    horizontal = if (style.inverted) 4.dp else 0.dp,
                    vertical = if (style.inverted) 2.dp else 0.dp
                )
        )
    }
}

@Composable
private fun RenderDivider(element: EscPosElement.HorizontalDivider) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        if (element.doubleLine) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                HorizontalDivider(color = ThermalInk.copy(alpha = 0.5f), thickness = 1.dp)
                HorizontalDivider(color = ThermalInk.copy(alpha = 0.5f), thickness = 1.dp)
            }
        } else {
            HorizontalDivider(
                color = ThermalInk.copy(alpha = 0.4f),
                thickness = 1.dp,
                modifier = Modifier.drawBehind {
                    // Custom dashed line effect
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    drawLine(
                        color = ThermalInk.copy(alpha = 0.5f),
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = 2f,
                        pathEffect = pathEffect
                    )
                }
            )
        }
    }
}

@Composable
private fun RenderBarcodeElement(element: EscPosElement.Barcode) {
    val bitmap = remember(element.content, element.type, element.heightDots, element.widthRatio) {
        BarcodeEncoder.encode1D(
            type = element.type,
            data = element.content,
            height = element.heightDots,
            widthRatio = element.widthRatio
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = when (element.alignment) {
            PrintAlignment.LEFT -> Alignment.Start
            PrintAlignment.CENTER -> Alignment.CenterHorizontally
            PrintAlignment.RIGHT -> Alignment.End
        }
    ) {
        if (element.hri == HriPosition.ABOVE || element.hri == HriPosition.BOTH) {
            Text(
                text = element.content,
                color = ThermalInk,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Barcode ${element.type} ${element.content}",
                modifier = Modifier
                    .wrapContentHeight()
                    .padding(vertical = 2.dp)
            )
        } else {
            Text(
                text = "[BARCODE: ${element.content}]",
                color = ThermalInk,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (element.hri == HriPosition.BELOW || element.hri == HriPosition.BOTH) {
            Text(
                text = element.content,
                color = ThermalInk,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun RenderQrCodeElement(element: EscPosElement.QrCode) {
    val bitmap = remember(element.content, element.moduleSize) {
        BarcodeEncoder.encodeQr(
            content = element.content,
            moduleSize = element.moduleSize
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = when (element.alignment) {
            PrintAlignment.LEFT -> Alignment.Start
            PrintAlignment.CENTER -> Alignment.CenterHorizontally
            PrintAlignment.RIGHT -> Alignment.End
        }
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "QR Code ${element.content}",
            modifier = Modifier
                .wrapContentHeight()
                .padding(4.dp)
        )
    }
}

@Composable
private fun RenderRasterImageElement(element: EscPosElement.RasterImage) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = when (element.alignment) {
            PrintAlignment.LEFT -> Alignment.CenterStart
            PrintAlignment.CENTER -> Alignment.Center
            PrintAlignment.RIGHT -> Alignment.CenterEnd
        }
    ) {
        Image(
            bitmap = element.bitmap.asImageBitmap(),
            contentDescription = "Raster Graphic",
            modifier = Modifier.wrapContentHeight()
        )
    }
}

@Composable
private fun PaperCutIndicator(isPartial: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "✂",
            color = Slate600,
            fontSize = 13.sp,
            modifier = Modifier.padding(end = 6.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .drawBehind {
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    drawLine(
                        color = ThermalCutBorder,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 2f,
                        pathEffect = pathEffect
                    )
                }
        )
        Text(
            text = if (isPartial) " [PARTIAL CUT] " else " [FULL CUT] ",
            color = Slate600,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .drawBehind {
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    drawLine(
                        color = ThermalCutBorder,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 2f,
                        pathEffect = pathEffect
                    )
                }
        )
    }
}

@Composable
private fun PaperTearEdge(isTop: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .drawBehind {
                val triangleWidth = 14f
                val triangleHeight = 8f
                val count = (size.width / triangleWidth).toInt() + 1
                val path = androidx.compose.ui.graphics.Path()

                if (isTop) {
                    path.moveTo(0f, triangleHeight)
                    for (i in 0 until count) {
                        val x = i * triangleWidth
                        path.lineTo(x + triangleWidth / 2f, 0f)
                        path.lineTo(x + triangleWidth, triangleHeight)
                    }
                    path.lineTo(size.width, size.height)
                    path.lineTo(0f, size.height)
                    path.close()
                } else {
                    path.moveTo(0f, 0f)
                    path.lineTo(size.width, 0f)
                    for (i in count downTo 0) {
                        val x = i * triangleWidth
                        path.lineTo(x + triangleWidth / 2f, triangleHeight)
                        path.lineTo(x, 0f)
                    }
                    path.close()
                }

                drawPath(path = path, color = ThermalPaperBg)
            }
    )
}
