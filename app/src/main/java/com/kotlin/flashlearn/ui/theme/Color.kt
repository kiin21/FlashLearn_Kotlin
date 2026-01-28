package com.kotlin.flashlearn.ui.theme

import androidx.compose.ui.graphics.Color

// PREMIUN BRAND COLORS
val BrandRed = Color(0xFFE11D48)        // Electric Crimson
val BrandRedDark = Color(0xFFBE123C)    // Darker shade
val BrandRedContainer = Color(0xFF3F1622) // Subtle red tint

// BACKGROUNDS - DARK MODE
val CryptoBlack = Color(0xFF0F1115)     // Deep Space
val Gunmetal = Color(0xFF1C1E24)        // Cool Charcoal Cards
val Charcoal = Color(0xFF2D3039)        // Borders/Input fields

// BACKGROUNDS - LIGHT MODE
val PaperWhite = Color(0xFFFFFFFF)      // Pure White
val MistGrey = Color(0xFFF4F4F5)        // Zinc 100 Cards

// TEXT COLORS
val TextWhitePrimary = Color(0xFFFAFAFA)
val TextBlackPrimary = Color(0xFF09090B) // Zinc 950
val TextGreySecondaryDark = Color(0xFFA1A1AA) // Zinc 400
val TextGreySecondaryLight = Color(0xFF71717A) // Zinc 500

// COMPATIBILITY ALIASES (To satisfy existing code while transitioning)
val FlashRed = BrandRed
val FlashRedLight = BrandRedContainer
val FlashGreen = Color(0xFF10B981) // Emerald 500 - more modern green
val FlashBlack = CryptoBlack
val FlashDarkGrey = Gunmetal
val FlashGrey = TextGreySecondaryDark
val FlashLightGrey = MistGrey
val FlashResultText = TextBlackPrimary
val Purple80 = BrandRed // Fallback for deprecated Material templates
val PurpleGrey80 = TextGreySecondaryDark
val Pink80 = BrandRed
val Purple40 = BrandRed
val PurpleGrey40 = TextGreySecondaryLight
val Pink40 = BrandRed


val FlashRedDarkest = Color(0xFFA00400)
val FlashRedDark = Color(0xFF2897FF)
val FlashRedMed = Color(0xFF6FB9FF)
val FlashRedLightFig = Color(0xFFB3DAFF)
val FlashRedKLightest = Color(0xFFFBC6C6)

val FlashSuccessDark = Color(0xFF0C7A41)
val FlashSuccessMed = Color(0xFF3AC0A0)
val FlashSuccessLight = Color(0xFFE7F4E8)

val FlashErrorMed = Color(0xFFE11D48)  // Using BrandRed for errors
val FlashErrorLight = Color(0xFFFEE2E2)  // Light red background for error states

val FlashInfoMed = Color(0xFF2563EB)   // Medium blue for info states (icons, borders, emphasis)
val FlashInfoLight = Color(0xFFDBEAFE) // Light blue background for info messages
val FlashInfoDark = Color(0xFF1E40AF)  // Dark blue for hover/active states
