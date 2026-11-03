package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Animated sound wave indicator providing live visual feedback during recording or playback.
 */
@Composable
fun AudioWaveformVisualizer(
    isActive: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")

    val anim1 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w1"
    )
    val anim2 by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(520, easing = LinearOutSlowInEasing), RepeatMode.Reverse),
        label = "w2"
    )
    val anim3 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(360, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w3"
    )
    val anim4 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(580, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w4"
    )
    val anim5 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(460, easing = LinearOutSlowInEasing), RepeatMode.Reverse),
        label = "w5"
    )
    val anim6 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(390, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w6"
    )

    val factors = listOf(anim1, anim2, anim3, anim4, anim5, anim6)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        factors.forEachIndexed { index, animatedVal ->
            val factor = if (isActive) animatedVal else {
                when (index % 4) {
                    0 -> 0.45f
                    1 -> 0.8f
                    2 -> 0.35f
                    else -> 0.6f
                }
            }
            val barHeight = (22 * factor).coerceIn(4f, 22f).dp

            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

/**
 * Premium, spacious Voice Alarm Slot Card designed for anti-theft audio management.
 * Replaces cramped 34dp circle buttons with generous touch targets, distinct visual hierarchy,
 * animated feedback, and clear Persian labels.
 */
@Composable
fun VoiceSlotCard(
    slot: Int,
    isRecorded: Boolean,
    recordingActive: Boolean,
    playingActive: Boolean,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onStartPlay: () -> Unit,
    onStopPlay: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = when {
        recordingActive -> Color(0xFF281114)
        playingActive -> Color(0xFF0C2415)
        isRecorded -> Color(0xFF191B1F)
        else -> Color(0xFF141518)
    }

    val cardBorder = when {
        recordingActive -> BorderStroke(1.5.dp, Color(0xFFEF5350))
        playingActive -> BorderStroke(1.5.dp, Color(0xFF4CAF50))
        isRecorded -> BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.3f))
        else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.07f))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .testTag("voice_slot_card_$slot"),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = cardBorder,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // ── TOP HEADER: Slot badge + SMS command + Status pill ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Right (RTL start): Number badge and Slot title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(
                                when {
                                    recordingActive -> Color(0xFFEF5350)
                                    playingActive -> Color(0xFF4CAF50)
                                    isRecorded -> Color(0xFFFFB300)
                                    else -> Color.White.copy(alpha = 0.08f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$slot",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = when {
                                recordingActive || playingActive -> Color.White
                                isRecorded -> Color.Black
                                else -> Color.LightGray
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "اسلات صوتی $slot ویژه هشدار ضد سکوت",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "دستور فعال‌سازی:",
                                color = Color.Gray,
                                fontSize = 10.5.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                color = Color.White.copy(alpha = 0.06f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "#SOUND${slot}_[کد]#",
                                    color = Color(0xFFFFB300),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                // Left (RTL end): Status chip
                Surface(
                    color = when {
                        recordingActive -> Color(0xFFEF5350).copy(alpha = 0.2f)
                        playingActive -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                        isRecorded -> Color(0xFF81C784).copy(alpha = 0.15f)
                        else -> Color.White.copy(alpha = 0.05f)
                    },
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        1.dp,
                        when {
                            recordingActive -> Color(0xFFEF5350).copy(alpha = 0.5f)
                            playingActive -> Color(0xFF4CAF50).copy(alpha = 0.5f)
                            isRecorded -> Color(0xFF81C784).copy(alpha = 0.35f)
                            else -> Color.White.copy(alpha = 0.08f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        recordingActive -> Color(0xFFEF5350)
                                        playingActive -> Color(0xFF4CAF50)
                                        isRecorded -> Color(0xFF81C784)
                                        else -> Color.Gray
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = when {
                                recordingActive -> "در حال ضبط"
                                playingActive -> "در حال پخش"
                                isRecorded -> "آماده شلیک"
                                else -> "خالی"
                            },
                            color = when {
                                recordingActive -> Color(0xFFFF8A80)
                                playingActive -> Color(0xFFA5D6A7)
                                isRecorded -> Color(0xFFA5D6A7)
                                else -> Color.Gray
                            },
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── MIDDLE: Waveform indicator & State explanation ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color.Black.copy(alpha = 0.28f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AudioWaveformVisualizer(
                    isActive = recordingActive || playingActive,
                    color = when {
                        recordingActive -> Color(0xFFEF5350)
                        playingActive -> Color(0xFF4CAF50)
                        isRecorded -> Color(0xFFFFB300)
                        else -> Color.White.copy(alpha = 0.2f)
                    }
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = when {
                        recordingActive -> "میکروفون فعال است (حداکثر ۳۰ ثانیه)... پس از اتمام صحبت، کلید ذخیره را لمس کنید."
                        playingActive -> "در حال پخش پیش‌نمایش آوای ضبط‌شده با حداکثر توان بلندگو..."
                        isRecorded -> "فایل صوتی با موفقیت در این اسلات ذخیره شد و آماده اجرای دستور پیامکی است."
                        else -> "برای مواقع سرقت، صدای فریاد، آژیر یا هشدار پلیس را برای این اسلات ضبط کنید."
                    },
                    color = when {
                        recordingActive -> Color(0xFFFFCDD2)
                        playingActive -> Color(0xFFC8E6C9)
                        isRecorded -> Color.White.copy(alpha = 0.85f)
                        else -> Color.Gray
                    },
                    fontSize = 11.sp,
                    lineHeight = 16.5.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // ── BOTTOM: Spacious, Clear Action Buttons (min 46-48dp touch targets) ──
            when {
                recordingActive -> {
                    // Full-width Stop & Save Button
                    Button(
                        onClick = onStopRecord,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("stop_recording_slot_$slot")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "توقف و ذخیره صدای اسلات $slot",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                playingActive -> {
                    // Full-width Stop Playback Button
                    Button(
                        onClick = onStopPlay,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("stop_playback_slot_$slot")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "توقف پخش پیش‌نمایش آوا",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                isRecorded -> {
                    // Recorded Slot: Distinct Play preview, Re-record, and isolated Delete buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Play Preview Button (Primary action for recorded slot)
                        FilledTonalButton(
                            onClick = onStartPlay,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFFFFB300).copy(alpha = 0.18f),
                                contentColor = Color(0xFFFFD54F)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp)
                                .testTag("play_sound_slot_$slot")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color(0xFFFFD54F)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "پخش صدا",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // 2. Re-record Button (Secondary action)
                        OutlinedButton(
                            onClick = onStartRecord,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(46.dp)
                                .testTag("rerecord_sound_slot_$slot")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp),
                                tint = Color(0xFFFFB300)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ضبط مجدد",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // 3. Delete Button (Isolated danger button with 46dp touch target)
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFC62828).copy(alpha = 0.15f))
                                .border(
                                    BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.35f)),
                                    RoundedCornerShape(12.dp)
                                )
                                .testTag("delete_sound_slot_$slot")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "حذف فایل صوتی اسلات $slot",
                                tint = Color(0xFFEF5350),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                else -> {
                    // Empty Slot: Inviting, large full-width Record button
                    Button(
                        onClick = onStartRecord,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB300),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("record_sound_slot_$slot")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "شروع ضبط صدای اسلات $slot",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}
