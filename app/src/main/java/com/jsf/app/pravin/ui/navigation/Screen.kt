package com.jsf.app.pravin.ui.navigation

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Workout : Screen("workout")
    object Progress : Screen("progress")
    object Avatar : Screen("avatar")
    object Servers : Screen("servers")
    object CreateServer : Screen("create_server")
    object ServerHome : Screen("server_home/{serverId}") {
        fun createRoute(serverId: String) = "server_home/$serverId"
    }
    object ChannelChat : Screen("channel/{channelId}") {
        fun createRoute(channelId: String) = "channel/$channelId"
    }
    object CreatePost : Screen("create_post/{channelId}") {
        fun createRoute(channelId: String) = "create_post/$channelId"
    }
    object ServerRoutines : Screen("server_routines/{serverId}") {
        fun createRoute(serverId: String) = "server_routines/$serverId"
    }
    object CreateRoutine : Screen("create_routine/{serverId}") {
        fun createRoute(serverId: String) = "create_routine/$serverId"
    }
    object RoutineDetail : Screen("routine/{routineId}") {
        fun createRoute(routineId: String) = "routine/$routineId"
    }
    object ServerMembers : Screen("server_members/{serverId}") {
        fun createRoute(serverId: String) = "server_members/$serverId"
    }
    object Profile : Screen("profile")
    object LiveWorkout : Screen("live_workout/{serverId}") {
        fun createRoute(serverId: String) = "live_workout/$serverId"
    }
    object Friends : Screen("friends")
    object Achievements : Screen("achievements")
    object RepBattle : Screen("rep_battle/{serverId}") {
        fun createRoute(serverId: String) = "rep_battle/$serverId"
    }
}
