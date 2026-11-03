package com.jsf.app.pravin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.jsf.app.pravin.data.db.FitnessDatabase
import com.jsf.app.pravin.data.preferences.UserPreferences
import com.jsf.app.pravin.data.repository.FitnessRepository
import com.jsf.app.pravin.ui.navigation.Screen
import com.jsf.app.pravin.ui.screens.auth.AuthScreen
import com.jsf.app.pravin.ui.screens.avatar.AvatarScreen
import com.jsf.app.pravin.ui.screens.home.HomeScreen
import com.jsf.app.pravin.ui.screens.onboarding.OnboardingScreen
import com.jsf.app.pravin.ui.screens.onboarding.OnboardingGoals
import com.jsf.app.pravin.ui.screens.progress.ProgressScreen
import com.jsf.app.pravin.ui.screens.community.ChannelChatScreen
import com.jsf.app.pravin.ui.screens.community.CommunityFeedScreen
import com.jsf.app.pravin.ui.screens.community.CreatePostScreen
import com.jsf.app.pravin.ui.screens.routine.CreateRoutineScreen
import com.jsf.app.pravin.ui.screens.routine.RoutineDetailScreen
import com.jsf.app.pravin.ui.screens.routine.ServerRoutinesScreen
import com.jsf.app.pravin.ui.screens.servers.CreateServerScreen
import com.jsf.app.pravin.ui.screens.servers.ServerHomeScreen
import com.jsf.app.pravin.ui.screens.servers.ServerMembersScreen
import com.jsf.app.pravin.ui.screens.servers.ServersScreen
import com.jsf.app.pravin.ui.screens.workout.WorkoutScreen
import com.jsf.app.pravin.ui.screens.profile.ProfileScreen
import com.jsf.app.pravin.ui.screens.livestream.LiveWorkoutScreen
import com.jsf.app.pravin.ui.screens.friends.FriendsScreen
import com.jsf.app.pravin.ui.screens.challenge.RepBattleScreen
import com.jsf.app.pravin.ui.screens.achievements.AchievementsScreen
import com.jsf.app.pravin.data.repository.CommunityRepository
import com.jsf.app.pravin.ui.theme.*
import kotlinx.coroutines.launch

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val screen: Screen
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PravinTheme {
                FitAIApp()
            }
        }
    }
}

@Composable
fun FitAIApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { UserPreferences(context) }

    val currentUserId by prefs.currentUserId.collectAsStateWithLifecycle(initialValue = null)
    val isOnboardingDone by prefs.isOnboardingDone.collectAsStateWithLifecycle(initialValue = null)

    if (currentUserId == null || isOnboardingDone == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CyanPrimary)
        }
        return
    }

    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val bottomNavItems = listOf(
        BottomNavItem("Home", Icons.Default.Home, Screen.Home),
        BottomNavItem("Community", Icons.Default.Groups, Screen.Servers),
        BottomNavItem("Workout", Icons.Default.SportsMartialArts, Screen.Workout),
        BottomNavItem("Progress", Icons.Default.ShowChart, Screen.Progress),
        BottomNavItem("Profile", Icons.Default.Person, Screen.Profile)
    )

    val startDestination = when {
        currentUserId!!.isEmpty() -> Screen.Auth.route
        !isOnboardingDone!! -> Screen.Onboarding.route
        else -> Screen.Home.route
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute != Screen.Auth.route &&
            currentRoute != Screen.Onboarding.route &&
            currentRoute != Screen.CreateServer.route &&
            currentRoute != Screen.Avatar.route &&
            !currentRoute.orEmpty().startsWith("server_home/") &&
            !currentRoute.orEmpty().startsWith("channel/") &&
            !currentRoute.orEmpty().startsWith("create_post/") &&
            !currentRoute.orEmpty().startsWith("server_routines/") &&
            !currentRoute.orEmpty().startsWith("create_routine/") &&
            !currentRoute.orEmpty().startsWith("routine/") &&
            !currentRoute.orEmpty().startsWith("server_members/") &&
            !currentRoute.orEmpty().startsWith("live_workout/") &&
            currentRoute != Screen.Friends.route &&
            currentRoute != Screen.Achievements.route &&
            !currentRoute.orEmpty().startsWith("rep_battle/")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = LightBg,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = LightCard

                ) {
                    bottomNavItems.forEach { item ->
                        val selected = navBackStackEntry?.destination?.hierarchy?.any {
                            it.route == item.screen.route
                        } == true
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyanPrimary,
                                selectedTextColor = CyanPrimary,
                                unselectedIconColor = TextGray,
                                unselectedTextColor = TextGray,
                                indicatorColor = CyanPrimary.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ── Auth ─────────────────────────────────────────────────────────
            composable(Screen.Auth.route) {
                AuthScreen(
                    onAuthSuccess = { uid, isNewUser ->
                        if (isNewUser) {
                            navController.navigate(Screen.Onboarding.route) {
                                popUpTo(Screen.Auth.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Auth.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

            // ── Onboarding ────────────────────────────────────────────────────
            composable(Screen.Onboarding.route) {
                val db = FitnessDatabase.getInstance(context)
                val repository = FitnessRepository(db)
                OnboardingScreen(
                    uid = currentUserId!!,
                    onComplete = { profile, goals ->
                        scope.launch {
                            repository.saveProfile(profile)
                            prefs.setFitnessGoal(goals.fitnessGoal)
                            prefs.setWeeklyGoalSessions(goals.weeklyGoalSessions)
                            prefs.setOnboardingDone(true)
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

            // ── Home ─────────────────────────────────────────────────────────
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToWorkout = { exercise ->
                        navController.navigate("${Screen.Workout.route}?exercise=$exercise")
                    }
                )
            }

            // ── Workout ───────────────────────────────────────────────────────
            composable(
                route = "${Screen.Workout.route}?exercise={exercise}",
                arguments = listOf(
                    androidx.navigation.navArgument("exercise") { defaultValue = "Squats" }
                )
            ) { backStackEntry ->
                val exercise = backStackEntry.arguments?.getString("exercise") ?: "Squats"
                WorkoutScreen(initialExercise = exercise)
            }
            composable(Screen.Workout.route) { WorkoutScreen() }

            // ── Progress, Avatar, Profile ─────────────────────────────────────
            composable(Screen.Progress.route) { ProgressScreen() }
            composable(Screen.Avatar.route) { AvatarScreen() }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onSignOut = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToFriends = {
                        navController.navigate(Screen.Friends.route)
                    },
                    onNavigateToAchievements = {
                        navController.navigate(Screen.Achievements.route)
                    }
                )
            }

            // ── Friends ────────────────────────────────────────────────────────
            composable(Screen.Friends.route) {
                FriendsScreen(onNavigateBack = { navController.popBackStack() })
            }

            // ── Achievements ────────────────────────────────────────────────────
            composable(Screen.Achievements.route) {
                AchievementsScreen(onNavigateBack = { navController.popBackStack() })
            }

            // ── Rep Battle ─────────────────────────────────────────────────────
            composable(
                route = Screen.RepBattle.route,
                arguments = listOf(
                    androidx.navigation.navArgument("serverId") { type = androidx.navigation.NavType.StringType }
                )
            ) { backStackEntry ->
                val serverId = backStackEntry.arguments?.getString("serverId") ?: return@composable
                RepBattleScreen(
                    serverId = serverId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ── Community — Servers ───────────────────────────────────────────
            composable(Screen.Servers.route) {
                ServersScreen(
                    onNavigateToServer = { serverId ->
                        navController.navigate(Screen.ServerHome.createRoute(serverId))
                    },
                    onNavigateToCreateServer = {
                        navController.navigate(Screen.CreateServer.route)
                    }
                )
            }

            // ── Create Server ─────────────────────────────────────────────────
            composable(Screen.CreateServer.route) {
                CreateServerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onServerCreated = { serverId ->
                        navController.navigate(Screen.ServerHome.createRoute(serverId)) {
                            popUpTo(Screen.CreateServer.route) { inclusive = true }
                        }
                    }
                )
            }

            // ── Server Home ───────────────────────────────────────────────────
            composable(
                route = Screen.ServerHome.route,
                arguments = listOf(
                    androidx.navigation.navArgument("serverId") { type = androidx.navigation.NavType.StringType }
                )
            ) { backStackEntry ->
                val serverId = backStackEntry.arguments?.getString("serverId") ?: return@composable
                ServerHomeScreen(
                    serverId = serverId,
                    onNavigateBack = { navController.popBackStack() },
                    onStartWorkout = { navController.navigate(Screen.Workout.route) },
                    onNavigateToChannel = { channelId, _ ->
                        navController.navigate(Screen.ChannelChat.createRoute(channelId))
                    },
                    onNavigateToRoutines = { sid ->
                        navController.navigate(Screen.ServerRoutines.createRoute(sid))
                    },
                    onNavigateToMembers = { sid ->
                        navController.navigate(Screen.ServerMembers.createRoute(sid))
                    },
                    onNavigateToLive = { sid ->
                        navController.navigate(Screen.LiveWorkout.createRoute(sid))
                    },
                    onNavigateToRepBattle = { sid ->
                        navController.navigate(Screen.RepBattle.createRoute(sid))
                    }
                )
            }

            // ── Channel Chat / Feed ───────────────────────────────────────────
            composable(
                route = Screen.ChannelChat.route,
                arguments = listOf(
                    androidx.navigation.navArgument("channelId") { type = androidx.navigation.NavType.StringType }
                )
            ) { backStackEntry ->
                val channelId = backStackEntry.arguments?.getString("channelId") ?: return@composable
                val db = FitnessDatabase.getInstance(context)
                val communityRepo = remember { CommunityRepository(db) }
                val channelType = remember(channelId) { androidx.compose.runtime.mutableStateOf("") }
                val channelName = remember(channelId) { mutableStateOf("") }
                LaunchedEffect(channelId) {
                    communityRepo.getChannelById(channelId).collect { ch ->
                        if (ch != null) {
                            channelType.value = ch.type
                            channelName.value = ch.name
                        }
                    }
                }
                when (channelType.value) {
                    com.jsf.app.pravin.data.model.Channel.ChannelType.TEXT,
                    com.jsf.app.pravin.data.model.Channel.ChannelType.ANNOUNCEMENTS -> {
                        ChannelChatScreen(
                            channelId = channelId,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    "" -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = CyanPrimary)
                        }
                    }
                    else -> {
                        CommunityFeedScreen(
                            channelId = channelId,
                            channelName = channelName.value,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToCreatePost = { cid ->
                                navController.navigate(Screen.CreatePost.createRoute(cid))
                            }
                        )
                    }
                }
            }

            // ── Create Post ───────────────────────────────────────────────────
            composable(
                route = Screen.CreatePost.route,
                arguments = listOf(
                    androidx.navigation.navArgument("channelId") { type = androidx.navigation.NavType.StringType }
                )
            ) { backStackEntry ->
                val channelId = backStackEntry.arguments?.getString("channelId") ?: return@composable
                CreatePostScreen(
                    channelId = channelId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ── Server Routines ───────────────────────────────────────────────
            composable(
                route = Screen.ServerRoutines.route,
                arguments = listOf(
                    androidx.navigation.navArgument("serverId") { type = androidx.navigation.NavType.StringType }
                )
            ) { backStackEntry ->
                val serverId = backStackEntry.arguments?.getString("serverId") ?: return@composable
                ServerRoutinesScreen(
                    serverId = serverId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCreateRoutine = {
                        navController.navigate(Screen.CreateRoutine.createRoute(serverId))
                    },
                    onNavigateToRoutine = { routineId ->
                        navController.navigate(Screen.RoutineDetail.createRoute(routineId))
                    }
                )
            }

            // ── Create Routine ─────────────────────────────────────────────────
            composable(
                route = Screen.CreateRoutine.route,
                arguments = listOf(
                    androidx.navigation.navArgument("serverId") { type = androidx.navigation.NavType.StringType }
                )
            ) { backStackEntry ->
                val serverId = backStackEntry.arguments?.getString("serverId") ?: return@composable
                CreateRoutineScreen(
                    serverId = serverId,
                    onNavigateBack = { navController.popBackStack() },
                    onRoutineCreated = { routineId ->
                        navController.navigate(Screen.RoutineDetail.createRoute(routineId)) {
                            popUpTo(Screen.CreateRoutine.createRoute(serverId)) { inclusive = true }
                        }
                    }
                )
            }

            // ── Routine Detail ─────────────────────────────────────────────────
            composable(
                route = Screen.RoutineDetail.route,
                arguments = listOf(
                    androidx.navigation.navArgument("routineId") { type = androidx.navigation.NavType.StringType }
                )
            ) { backStackEntry ->
                val routineId = backStackEntry.arguments?.getString("routineId") ?: return@composable
                RoutineDetailScreen(
                    routineId = routineId,
                    onNavigateBack = { navController.popBackStack() },
                    onStartWorkout = { navController.navigate(Screen.Workout.route) }
                )
            }

            // ── Server Members ─────────────────────────────────────────────────
            composable(
                route = Screen.ServerMembers.route,
                arguments = listOf(
                    androidx.navigation.navArgument("serverId") { type = androidx.navigation.NavType.StringType }
                )
            ) { backStackEntry ->
                val serverId = backStackEntry.arguments?.getString("serverId") ?: return@composable
                ServerMembersScreen(
                    serverId = serverId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ── Live Workout ───────────────────────────────────────────────────
            composable(
                route = Screen.LiveWorkout.route,
                arguments = listOf(
                    androidx.navigation.navArgument("serverId") { type = androidx.navigation.NavType.StringType }
                )
            ) { backStackEntry ->
                val serverId = backStackEntry.arguments?.getString("serverId") ?: return@composable
                LiveWorkoutScreen(
                    serverId = serverId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
