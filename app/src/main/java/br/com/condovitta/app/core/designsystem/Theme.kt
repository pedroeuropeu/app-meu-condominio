package br.com.condovitta.app.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** Cores da marca Condovitta (logo: "Condo" azul-marinho, "vitta" verde-água, arco azul→verde). */
object Brand {
    val Navy = Color(0xFF0A2C55)
    val Blue = Color(0xFF0A62B8)
    val Teal = Color(0xFF01B6A3)
    val ArcStart = Color(0xFF1062C4)
    val ArcMid = Color(0xFF05A8AA)
    val ArcEnd = Color(0xFF1BE79A)

    /** Acento de função da Portaria (não é cor da marca). */
    val Amber = Color(0xFFE59008)
}

/** Cores que o Material 3 não tem: estados, acento, topo em degradê e as cores de apoio do protótipo. */
@Immutable
data class ExtendedColors(
    val success: Color,
    val successContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val warn: Color,
    val warnContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val accent: Color,
    val accentContainer: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val card: Color,
    val cardBorder: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    val navIndicator: Color
)

private fun lightExtended(portaria: Boolean) = ExtendedColors(
    success = Color(0xFF15803D), successContainer = Color(0xFFDCFCE7),
    danger = Color(0xFFDC2626), dangerContainer = Color(0xFFFEE2E2),
    warn = Color(0xFFB45309), warnContainer = Color(0xFFFEF3C7),
    info = Color(0xFF1D4ED8), infoContainer = Color(0xFFDBEAFE),
    accent = Brand.Teal, accentContainer = Color(0xFFD3F5F0),
    heroStart = if (portaria) Color(0xFF7A4E06) else Brand.Navy,
    heroEnd = if (portaria) Brand.Amber else Color(0xFF0A7FA0),
    card = Color.White, cardBorder = Color(0xFFE2EAEF),
    ink = Color(0xFF0A2C4E), muted = Color(0xFF5B6B82), faint = Color(0xFF93A1B6),
    navIndicator = if (portaria) Color(0xFFFDF0D6) else Color(0xFFD6EDF0)
)

private fun darkExtended(portaria: Boolean) = ExtendedColors(
    success = Color(0xFF4ADE80), successContainer = Color(0x214ADE80),
    danger = Color(0xFFF87171), dangerContainer = Color(0x21F87171),
    warn = Color(0xFFFBBF24), warnContainer = Color(0x21FBBF24),
    info = Color(0xFF93B4FF), infoContainer = Color(0x246096FF),
    accent = Color(0xFF2DD4BF), accentContainer = Color(0x242DD4BF),
    heroStart = if (portaria) Color(0xFF4A2F05) else Color(0xFF0A2440),
    heroEnd = if (portaria) Color(0xFFB2730A) else Color(0xFF0A5E80),
    card = Color(0xFF0E1F2E), cardBorder = Color(0xFF1B3043),
    ink = Color(0xFFEEF3F9), muted = Color(0xFF90A1B9), faint = Color(0xFF607089),
    navIndicator = if (portaria) Color(0x24FBBF24) else Color(0xFF143A4A)
)

private fun lightScheme(portaria: Boolean): ColorScheme = lightColorScheme(
    primary = if (portaria) Brand.Amber else Brand.Blue,
    onPrimary = if (portaria) Color(0xFF3A2606) else Color.White,
    primaryContainer = if (portaria) Color(0xFFFDF0D6) else Color(0xFFE0EDF8),
    onPrimaryContainer = if (portaria) Color(0xFFB2730A) else Brand.Navy,
    secondary = Color(0xFF0B7C70), onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3F5F0), onSecondaryContainer = Color(0xFF064E46),
    tertiary = Brand.Navy, onTertiary = Color.White,
    background = Color(0xFFF2F6F8), onBackground = Color(0xFF0A2C4E),
    surface = Color(0xFFF2F6F8), onSurface = Color(0xFF0A2C4E),
    surfaceVariant = Color(0xFFEBF2F5), onSurfaceVariant = Color(0xFF5B6B82),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFEBF2F5), surfaceContainerHigh = Color(0xFFDFE9EE), surfaceContainerHighest = Color(0xFFD6E2E8),
    outline = Color(0xFFCEDBE3), outlineVariant = Color(0xFFE2EAEF),
    error = Color(0xFFDC2626), onError = Color.White, errorContainer = Color(0xFFFEE2E2), onErrorContainer = Color(0xFF7F1D1D),
    inverseSurface = Color(0xFF0E2440), inverseOnSurface = Color(0xFFEEF3F9), inversePrimary = Color(0xFF5BB4EE)
)

private fun darkScheme(portaria: Boolean): ColorScheme = darkColorScheme(
    primary = if (portaria) Color(0xFFFBBF24) else Color(0xFF5BB4EE),
    onPrimary = if (portaria) Color(0xFF2A1A03) else Color(0xFF04192C),
    primaryContainer = if (portaria) Color(0x24FBBF24) else Color(0xFF133552),
    onPrimaryContainer = if (portaria) Color(0xFFFBBF24) else Color(0xFFCDE7F8),
    secondary = Color(0xFF5EEAD4), onSecondary = Color(0xFF032622),
    secondaryContainer = Color(0x242DD4BF), onSecondaryContainer = Color(0xFF99F6E4),
    tertiary = Color(0xFFCDE7F8), onTertiary = Brand.Navy,
    background = Color(0xFF07141F), onBackground = Color(0xFFEEF3F9),
    surface = Color(0xFF07141F), onSurface = Color(0xFFEEF3F9),
    surfaceVariant = Color(0xFF13283A), onSurfaceVariant = Color(0xFF90A1B9),
    surfaceContainerLowest = Color(0xFF0B1A27), surfaceContainerLow = Color(0xFF0E1F2E),
    surfaceContainer = Color(0xFF13283A), surfaceContainerHigh = Color(0xFF1B3449), surfaceContainerHighest = Color(0xFF223D54),
    outline = Color(0xFF284259), outlineVariant = Color(0xFF1B3043),
    error = Color(0xFFF87171), onError = Color(0xFF3B0A0A), errorContainer = Color(0x21F87171), onErrorContainer = Color(0xFFFECACA),
    inverseSurface = Color(0xFFE7EEF8), inverseOnSurface = Color(0xFF0C1A33), inversePrimary = Brand.Blue
)

/**
 * Títulos: Lexend na marca (por ora a sans-serif do sistema em peso forte, até as fontes entrarem no pacote).
 * Texto: Inter na marca (por ora a sans-serif do sistema).
 */
val DisplayFamily: FontFamily = FontFamily.SansSerif
val BodyFamily: FontFamily = FontFamily.SansSerif

private val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.02).em),
    headlineMedium = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.02).em),
    headlineSmall = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.01).em),
    titleLarge = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = BodyFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = BodyFamily, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = BodyFamily, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = BodyFamily, fontSize = 12.5.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = BodyFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = BodyFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = BodyFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.06.em)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val LocalExtendedColors = staticCompositionLocalOf { lightExtended(false) }

/** Atalho: `MaterialTheme.ext.success` etc. */
val MaterialTheme.ext: ExtendedColors
    @Composable get() = LocalExtendedColors.current

/** Tema do app. [portaria] troca o azul da marca pelo âmbar da função (como no protótipo). */
@Composable
fun CondovittaTheme(dark: Boolean, portaria: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalExtendedColors provides if (dark) darkExtended(portaria) else lightExtended(portaria)) {
        MaterialTheme(
            colorScheme = if (dark) darkScheme(portaria) else lightScheme(portaria),
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
