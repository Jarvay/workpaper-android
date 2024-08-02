package jarvay.workpaper.compose

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class Screen(
    val route: String,
    val navArguments: List<NamedNavArgument> = emptyList()
) {
    data object Home : Screen("home")

    data object DayCreate : Screen(route = "day/create")

    data object DayUpdate : Screen(
        route = "day/update/{dayId}",
        navArguments = listOf(navArgument("dayId") {
            type = NavType.StringType
        })
    ) {
        fun createRoute(dayId: Long) = "day/update/${dayId}"

    }

    data object DayDetail : Screen(
        route = "day/{dayId}",
    ) {
        fun createRoute(dayId: Long) = "day/${dayId}"
    }

    data object RuleCreate : Screen(route = "rule/create")
    data object RuleUpdate : Screen(route = "rule/update/{ruleId}") {
        fun createRoute(ruleId: Long) = "rule/update/${ruleId}"
    }

    data object AlbumDetail : Screen(route = "album/detail/{albumId}") {
        fun createRoute(albumId: Long) = "album/detail/${albumId}"
    }

    data object Settings : Screen(route = "settings")
}