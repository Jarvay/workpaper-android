package jarvay.workpaper.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.blankj.utilcode.util.LogUtils
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

@Composable
fun WorkpaperTheme(
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val controller = remember(dynamicColor) {
        LogUtils.d("dynamicColor: $dynamicColor")
        if (dynamicColor) {
            ThemeController(
                colorSchemeMode = ColorSchemeMode.MonetSystem,
                paletteStyle = ThemePaletteStyle.Vibrant,
                colorSpec = ThemeColorSpec.Spec2025,
            )
        } else {
            ThemeController(
                colorSchemeMode = ColorSchemeMode.MonetSystem,
                keyColor = Color(0x9575CDFF),
                paletteStyle = ThemePaletteStyle.Vibrant,
                colorSpec = ThemeColorSpec.Spec2025,
            )
        }
    }

    MiuixTheme(
        controller = controller,
        content = content
    )
}