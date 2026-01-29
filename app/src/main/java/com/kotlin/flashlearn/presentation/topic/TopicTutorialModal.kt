package com.kotlin.flashlearn.presentation.topic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kotlin.flashlearn.ui.theme.FlashRed

@Composable
fun TopicTutorialModal(
    step: Int,
    onCancel: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val total = 5
    val s = step.coerceIn(0, total - 1)

    data class StepData(
        val title: String,
        val desc: String,
        val content: @Composable () -> Unit
    )

    val steps = listOf(
        StepData(
            title = "Topic name",
            desc = "Name your topic. Enter a title to organize your vocabulary set.",
            content = {
                MockLabel("Topic Name *")
                MockOutlinedField(placeholder = "Enter topic name...")
            }
        ),
        StepData(
            title = "Description",
            desc = "Add a description to help you remember what this topic is about.",
            content = {
                Text(
                    "Description (Optional)",
                    color = Color(0xFF6B7280),
                    fontWeight = FontWeight.Medium
                )
                MockTextArea(placeholder = "Add description...")
            }
        ),
        StepData(
            title = "Search for a word",
            desc = "Search for a word to quickly find and add vocabulary.",
            content = {
                MockLabel("Search for a word")
                MockOutlinedField(
                    placeholder = "Type to search...",
                    leading = {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF9CA3AF), RoundedCornerShape(6.dp))
                        )
                    }
                )
            }
        ),
        StepData(
            title = "Get words by topic",
            desc = "Pick a suggested topic to get words instantly from curated lists.",
            content = {
                MockLabel("Get words by topic")
                MockOutlinedField(
                    placeholder = "Choose a topic...",
                    trailing = {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFF9CA3AF), RoundedCornerShape(4.dp))
                        )
                    }
                )
            }
        ),
        StepData(
            title = "Enter manually",
            desc = "Or enter a keyword and tap Get to fetch words by topic.",
            content = {
                Text(
                    "Or enter manually:",
                    color = Color(0xFF9CA3AF),
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        MockOutlinedField(
                            placeholder = "e.g. environment, technology...",
                            height = 52.dp
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .height(52.dp)
                            .width(86.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(FlashRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Get", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    )

    val data = steps[s]

    Dialog(onDismissRequest = onCancel) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White)
                .padding(18.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFF3F4F6))
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Create New Topic",
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF111827)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Step ${s + 1} / $total — ${data.title}",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = data.desc,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF9FAFB))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        data.content()
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(total) { i ->
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (i == s) 20.dp else 8.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (i == s) FlashRed else Color(0xFFE5E7EB))
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Cancel",
                        color = FlashRed,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onCancel() }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF3F4F6))
                                .then(
                                    if (s == 0) Modifier else Modifier.clickable { onBack() }
                                )
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                "Back",
                                color = if (s == 0) Color(0xFF9CA3AF) else Color(0xFF111827),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        val isLast = s == total - 1
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(FlashRed)
                                .clickable { onNext() }
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text(
                                if (isLast) "Done" else "Next",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MockLabel(text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF111827)
    )
}

@Composable
private fun MockOutlinedField(
    placeholder: String,
    height: Dp = 52.dp,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, Color(0xFF9CA3AF), RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(10.dp))
        }

        Text(
            text = placeholder,
            color = Color(0xFF9CA3AF),
            modifier = Modifier.weight(1f)
        )

        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        }
    }
}

@Composable
private fun MockTextArea(
    placeholder: String,
    height: Dp = 86.dp
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, Color(0xFF9CA3AF), RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Text(text = placeholder, color = Color(0xFF9CA3AF))
    }
}
