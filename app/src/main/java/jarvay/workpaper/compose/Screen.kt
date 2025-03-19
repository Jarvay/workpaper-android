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

    data object StyleCreate : Screen(route = "style/create")
    data object StyleUpdate : Screen(route = "style/update/{styleId}") {
        fun createRoute(styleId: Long) = "style/update/${styleId}"
    }

    data object AlbumDetail : Screen(route = "album/detail/{albumId}") {
        fun createRoute(albumId: Long) = "album/detail/${albumId}"
    }

    data object DirsRelation : Screen(route = "album/relation/{albumId}") {
        fun createRoute(albumId: Long) = "album/relation/${albumId}"
    }

    data object Settings : Screen(route = "settings")

    data object Sponsor : Screen(route = "sponsor")
}