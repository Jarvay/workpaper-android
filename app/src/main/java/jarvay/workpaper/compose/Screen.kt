package jarvay.workpaper.compose

import androidx.navigation.NamedNavArgument

sealed class Screen(
    val route: String,
    val navArguments: List<NamedNavArgument> = emptyList()
) {
    data object Home : Screen("home")

    data object RuleCreate : Screen(route = "rule/create")
    data object RuleUpdate : Screen(route = "rule/update/{ruleId}") {
        fun createRoute(ruleId: Long) = "rule/update/${ruleId}"
    }

    data object AlbumDetail : Screen(route = "album/detail/{albumId}") {
        fun createRoute(albumId: Long) = "album/detail/${albumId}"
    }

    data object Settings : Screen(route = "settings")

    data object Sponsor : Screen(route = "sponsor")
}