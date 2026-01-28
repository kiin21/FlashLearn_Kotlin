package com.kotlin.flashlearn.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.kotlin.flashlearn.MainActivity
import com.kotlin.flashlearn.utils.DateKey
import dagger.hilt.android.EntryPointAccessors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.action.clickable
import com.kotlin.flashlearn.R

class DailyWordWidget : GlanceAppWidget() {

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val ep = EntryPointAccessors.fromApplication(
            context,
            DailyWordWidgetEntryPoint::class.java
        )

        val lastUserId = ep.lastUserPrefs().getLastUserId()
        val today = DateKey.today()

        val dailyWord = if (lastUserId != null) {
            ep.getTodayDailyWordUseCase().invoke(lastUserId, today)
        } else null

        val word = dailyWord?.word ?: "Open FlashLearn"
        val meaning = dailyWord?.meaning ?: "Sign in to see Daily Word"

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(ColorProvider(R.color.white))
                        .cornerRadius(16.dp)
                        .padding(16.dp)
                        .clickable(
                            actionStartActivity(
                                Intent(context, MainActivity::class.java)
                            )
                        ),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {

                    // Title
                    Text(
                        text = "Daily Word",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF888888))
                        )
                    )

                    Spacer(GlanceModifier.height(10.dp))

                    // Word
                    Text(
                        text = word,
                        style = TextStyle(
                            color = ColorProvider(Color.Black),
                            fontSize = 22.sp,
                            fontWeight = androidx.glance.text.FontWeight.Bold
                        )
                    )

                    // IPA
                    if (!dailyWord?.ipa.isNullOrBlank()) {
                        Spacer(GlanceModifier.height(4.dp))
                        Text(
                            text = "${dailyWord?.ipa}",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF666666)),
                                fontSize = 14.sp
                            )
                        )
                    }

                    Spacer(GlanceModifier.height(8.dp))

                    // Meaning
                    Text(
                        text = meaning,
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF444444)),
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }
    }
}
