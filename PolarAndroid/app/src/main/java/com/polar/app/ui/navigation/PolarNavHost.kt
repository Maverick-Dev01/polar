package com.polar.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.polar.app.AppContainer
import com.polar.app.BuildConfig
import com.polar.app.data.AppSettings
import com.polar.app.data.BitmapLoader
import com.polar.app.ui.catalog.CatalogScreen
import com.polar.app.ui.editor.*
import com.polar.app.ui.home.*
import com.polar.app.ui.onboarding.OnboardingScreen
import com.polar.app.ui.rememberReduceMotion
import com.polar.app.ui.settings.SettingsScreen
import com.polar.app.ui.settings.UpdateViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PolarNavHost(container: AppContainer, startOnboarding: Boolean) {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val reduce = rememberReduceMotion()
    val enter: EnterTransition = if (reduce) EnterTransition.None else fadeIn() + slideInHorizontally { it / 8 }
    val exit: ExitTransition = if (reduce) ExitTransition.None else fadeOut()

    NavHost(
        navController = nav,
        startDestination = if (startOnboarding) OnboardingRoute else HomeRoute,
        enterTransition = { enter }, exitTransition = { exit },
        popEnterTransition = { if (reduce) EnterTransition.None else fadeIn() }, popExitTransition = { exit }
    ) {
        composable<OnboardingRoute> {
            OnboardingScreen(onDone = {
                scope.launch { container.settings.setOnboardingSeen(true) }
                if (nav.previousBackStackEntry != null) {
                    nav.popBackStack() // vino de Ajustes: regresa ahí en vez de apilar otro Inicio
                } else {
                    nav.navigate(HomeRoute) { popUpTo<OnboardingRoute> { inclusive = true } }
                }
            })
        }
        composable<HomeRoute> {
            val context = LocalContext.current
            val vm: HomeViewModel = viewModel(factory = viewModelFactory {
                initializer { HomeViewModel(container.store, context.cacheDir) }
            })
            HomeScreen(
                vm = vm,
                thumbnailFile = container.store::thumbnailFile,
                onOpen = { nav.navigate(EditorRoute(it)) { launchSingleTop = true } },
                onNew = { nav.navigate(CatalogRoute) },
                onSettings = { nav.navigate(SettingsRoute) { launchSingleTop = true } }
            )
        }
        composable<CatalogRoute> {
            CatalogScreen(
                container = container,
                onCreated = { id, notice -> nav.navigate(EditorRoute(id, notice)) { popUpTo<HomeRoute>() } },
                onBack = { nav.popBackStack() }
            )
        }
        composable<EditorRoute> { entry ->
            val route = entry.toRoute<EditorRoute>()
            val vm: EditorViewModel = viewModel(key = route.projectId, factory = viewModelFactory {
                initializer {
                    EditorViewModel(
                        route.projectId,
                        EditorDeps(
                            store = container.store,
                            photos = container.photos,
                            exports = container.exports,
                            thumbnail = { p, t ->
                                withContext(Dispatchers.Default) {
                                    container.thumbnails.projectPng(p, { a -> container.bitmaps.load(a.path, BitmapLoader.PREVIEW_MAX) }, t)
                                }
                            },
                            loadTemplate = { path -> container.bitmaps.load(path, BitmapLoader.PREVIEW_MAX) },
                            removeBackground = container.backgrounds::mask,
                            templates = container.templates
                        )
                    )
                }
            })
            var leaving by remember { mutableStateOf(false) }
            EditorScreen(vm, container, route.notice, onBack = {
                if (!leaving) {
                    leaving = true
                    scope.launch {
                        try { if (vm.flush()) nav.popBackStack() }
                        finally { leaving = false }
                    }
                }
            })
        }
        composable<SettingsRoute> {
            val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            val updates: UpdateViewModel? = if (BuildConfig.GITHUB_UPDATES_ENABLED) viewModel(factory = viewModelFactory {
                initializer { UpdateViewModel(container.updates) }
            }) else null
            var trashCount by remember { mutableIntStateOf(0) }
            LaunchedEffect(Unit) { trashCount = withContext(Dispatchers.IO) { container.store.trashCount() } }
            SettingsScreen(
                settings = settings,
                trashCount = trashCount,
                onEmptyTrash = { scope.launch { trashCount = withContext(Dispatchers.IO) { container.store.emptyTrash(); container.store.trashCount() } } },
                onTheme = { scope.launch { container.settings.setTheme(it) } },
                onUnits = { scope.launch { container.settings.setUnits(it) } },
                onPaper = { scope.launch { container.settings.setDefaultPaper(it) } },
                onShowOnboarding = { nav.navigate(OnboardingRoute) },
                onBack = { nav.popBackStack() },
                updates = updates
            )
        }
    }
}
