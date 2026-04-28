package com.okamilang.mysteria.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.okamilang.mysteria.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val imFellSc = FontFamily(
    Font(googleFont = GoogleFont("IM Fell English SC"), fontProvider = provider)
)

private val specialElite = FontFamily(
    Font(googleFont = GoogleFont("Special Elite"), fontProvider = provider)
)

private val cinzel = FontFamily(
    Font(googleFont = GoogleFont("Cinzel"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Cinzel"), fontProvider = provider, weight = FontWeight.Bold)
)

val FontTitre: FontFamily = imFellSc
val FontTexte: FontFamily = specialElite
val FontOrnement: FontFamily = cinzel

val MysteriaTypography = Typography(
    displayLarge = TextStyle(fontFamily = imFellSc, fontSize = 40.sp, letterSpacing = 0.18.em),
    displayMedium = TextStyle(fontFamily = imFellSc, fontSize = 28.sp, letterSpacing = 0.16.em),
    headlineLarge = TextStyle(fontFamily = imFellSc, fontSize = 24.sp, letterSpacing = 0.14.em),
    headlineMedium = TextStyle(fontFamily = imFellSc, fontSize = 20.sp, letterSpacing = 0.12.em),
    headlineSmall = TextStyle(fontFamily = imFellSc, fontSize = 18.sp, letterSpacing = 0.10.em),
    titleLarge = TextStyle(fontFamily = imFellSc, fontSize = 18.sp, letterSpacing = 0.10.em),
    titleMedium = TextStyle(fontFamily = imFellSc, fontSize = 15.sp, letterSpacing = 0.08.em),
    titleSmall = TextStyle(fontFamily = cinzel, fontSize = 11.sp, letterSpacing = 0.28.em, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontFamily = specialElite, fontSize = 15.sp),
    bodyMedium = TextStyle(fontFamily = specialElite, fontSize = 13.sp),
    bodySmall = TextStyle(fontFamily = specialElite, fontSize = 11.sp),
    labelLarge = TextStyle(fontFamily = cinzel, fontSize = 12.sp, letterSpacing = 0.22.em, fontWeight = FontWeight.Bold),
    labelMedium = TextStyle(fontFamily = cinzel, fontSize = 10.sp, letterSpacing = 0.28.em),
    labelSmall = TextStyle(fontFamily = cinzel, fontSize = 9.sp, letterSpacing = 0.32.em)
)
