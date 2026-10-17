package com.moneyjournal.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneyjournal.app.R

/* ---------- Palette ---------- */
val Ink = Color(0xFF0D1117)
val InkDeep = Color(0xFF090D12)
val Surface1 = Color(0xFF131A22)
val Surface2 = Color(0xFF18212B)
val Surface3 = Color(0xFF1E2936)
val OutlineSoft = Color(0xFF243040)
val OutlineFaint = Color(0xFF1B2531)

val Accent = Color(0xFF22E39A)
val AccentDeep = Color(0xFF0E9F6E)
val AccentSoft = Color(0xFF7DF5C6)
val AccentDim = Color(0xFF12352A)

val TextPrimary = Color(0xFFF2F6FA)
val TextSecondary = Color(0xFF9AA8B8)
val TextTertiary = Color(0xFF6B7A8C)

val Income = Color(0xFF3BE8A4)
val Expense = Color(0xFFF4F7FB)
val Transfer = Color(0xFF7FB4FF)
val Danger = Color(0xFFFF6B6B)
val Warning = Color(0xFFFFB020)

val CategoryPalette = listOf(
    Color(0xFF22C55E), // green
    Color(0xFFF59E0B), // amber
    Color(0xFF60A5FA), // blue
    Color(0xFFF472B6), // pink
    Color(0xFFA78BFA), // violet
    Color(0xFF2DD4BF), // teal
    Color(0xFFFB7185), // rose
    Color(0xFF94A3B8), // slate
    Color(0xFFFDBA74), // peach
    Color(0xFF38BDF8), // sky
)

/* Account card gradients */
val AccountGradients = listOf(
    listOf(Color(0xFF064E3B), Color(0xFF0B2B25), Color(0xFF10251F)),
    listOf(Color(0xFF1E3A8A), Color(0xFF12203F), Color(0xFF0E1A2E)),
    listOf(Color(0xFF4C1D95), Color(0xFF241542), Color(0xFF171229)),
    listOf(Color(0xFF7C2D12), Color(0xFF3B1A10), Color(0xFF241310)),
    listOf(Color(0xFF0F766E), Color(0xFF0C3330), Color(0xFF0B1F1E)),
    listOf(Color(0xFF334155), Color(0xFF1B2430), Color(0xFF121A23)),
)

/* ---------- Typography ---------- */
val PlusJakarta = FontFamily(
    Font(R.font.pjs_regular, FontWeight.Normal),
    Font(R.font.pjs_medium, FontWeight.Medium),
    Font(R.font.pjs_semibold, FontWeight.SemiBold),
    Font(R.font.pjs_bold, FontWeight.Bold),
    Font(R.font.pjs_extrabold, FontWeight.ExtraBold),
)

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = PlusJakarta, fontWeight = FontWeight.ExtraBold,
        fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-1.2).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = PlusJakarta, fontWeight = FontWeight.Bold,
        fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.8).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = PlusJakarta, fontWeight = FontWeight.Bold,
        fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.4).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = PlusJakarta, fontWeight = FontWeight.Bold,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = PlusJakarta, fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp, lineHeight = 24.sp, letterSpacing = (-0.1).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakarta, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = PlusJakarta, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 19.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = PlusJakarta, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakarta, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = PlusJakarta, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.1.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = PlusJakarta, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = PlusJakarta, fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = PlusJakarta, fontWeight = FontWeight.Bold,
        fontSize = 10.5.sp, lineHeight = 14.sp, letterSpacing = 0.7.sp,
    ),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

private val DarkScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF04120C),
    primaryContainer = AccentDim,
    onPrimaryContainer = AccentSoft,
    secondary = AccentSoft,
    onSecondary = Color(0xFF04120C),
    tertiary = Transfer,
    background = Ink,
    onBackground = TextPrimary,
    surface = Ink,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextSecondary,
    surfaceTint = Accent,
    outline = OutlineSoft,
    outlineVariant = OutlineFaint,
    error = Danger,
    onError = Color(0xFF1A0505),
)

@Composable
fun MoneyJournalTheme(content: @Composable () -> Unit) {
    @Suppress("UNUSED_EXPRESSION")
    isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = DarkScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}

/* ---------- Reusable brushes ---------- */
val GreenGradient = Brush.linearGradient(listOf(Color(0xFF2BF2A6), Color(0xFF0FAE7B)))
val HeroGradient = Brush.linearGradient(
    listOf(Color(0xFF10281F), Color(0xFF0E1C1C), Color(0xFF101A26))
)
val AccentGlow = Brush.radialGradient(listOf(Color(0x3322E39A), Color(0x0022E39A)))
