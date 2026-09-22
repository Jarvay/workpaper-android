package jarvay.workpaper.compose

import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavKey

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Home : Route

    @Serializable
    data object RuleCreate : Route

    @Serializable
    data class RuleUpdate(val ruleId: Long) : Route

    @Serializable
    data object StyleCreate : Route

    @Serializable
    data class StyleUpdate(val styleId: Long) : Route

    @Serializable
    data class AlbumDetail(val albumId: Long) : Route

    @Serializable
    data class DirsRelation(val albumId: Long) : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object LiveWallpaperSettings : Route

    @Serializable
    data object Sponsor : Route
}