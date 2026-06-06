package app.aurorasecurity.security.ui.theme

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

private val LightColors = lightColorScheme(
    primary              = Ocean,
    onPrimary            = Color.White,
    primaryContainer     = Mist,
    onPrimaryContainer   = Ink,
    secondary            = Graphite,
    onSecondary          = Color.White,
    secondaryContainer   = Mist,
    onSecondaryContainer = Ink,
    tertiary             = Mint,
    onTertiary           = Color.White,
    tertiaryContainer    = Color(0xFFD7E0D9),
    onTertiaryContainer  = Color(0xFF445148),
    background           = Linen,
    onBackground         = Ink,
    surface              = Paper,
    onSurface            = Ink,
    onSurfaceVariant     = Graphite,
    surfaceVariant       = Mist,
    outline              = Color(0xFFC7C0B7),
    outlineVariant       = Color(0xFFE2DBD2),
    error                = Coral,
    onError              = Color.White,
    errorContainer       = Color(0xFFEAD8D4),
    onErrorContainer     = Color(0xFF6F423C),
)

private val DarkColors = darkColorScheme(
    primary              = Color(0xFF7FB7D2),
    onPrimary            = Color(0xFF03101A),
    primaryContainer     = Color(0xFF183247),
    onPrimaryContainer   = Frost,
    secondary            = MoonMist,
    onSecondary          = Midnight,
    secondaryContainer   = DeepSea,
    onSecondaryContainer = Frost,
    tertiary             = Color(0xFF7FC8BD),
    onTertiary           = Midnight,
    tertiaryContainer    = Color(0xFF15302F),
    onTertiaryContainer  = Color(0xFFC5E7E1),
    background           = Midnight,
    onBackground         = Frost,
    surface              = Island,
    onSurface            = Frost,
    onSurfaceVariant     = MoonMist,
    surfaceVariant       = DeepSea,
    outline              = Color(0xFF45647E),
    outlineVariant       = Color(0xFF22384B),
    error                = Ember,
    onError              = Midnight,
    errorContainer       = Color(0xFF5F352E),
    onErrorContainer     = Ember,
)

@Composable
fun AlarmTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? ComponentActivity ?: return@SideEffect
            val systemBarScrim = colorScheme.background.toArgb()
            activity.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(
                    lightScrim = Color.Transparent.toArgb(),
                    darkScrim = Color.Transparent.toArgb(),
                    detectDarkMode = { darkTheme },
                ),
                navigationBarStyle = SystemBarStyle.auto(
                    lightScrim = systemBarScrim,
                    darkScrim = systemBarScrim,
                    detectDarkMode = { darkTheme },
                ),
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
