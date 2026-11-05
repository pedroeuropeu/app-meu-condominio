package br.com.condovitta.app.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import br.com.condovitta.app.core.designsystem.BackTopBar
import br.com.condovitta.app.core.designsystem.SoonContent
import br.com.condovitta.app.core.designsystem.ext
import br.com.condovitta.app.ui.navigation.Soon
import br.com.condovitta.app.ui.navigation.SoonCatalog
import kotlin.reflect.KClass

/** Uma aba da barra inferior. */
data class Tab(val route: Any, val label: String, val icon: ImageVector, val selectedIcon: ImageVector = icon) {
    val routeClass: KClass<*> get() = route::class
}

/**
 * Casca de um modo: barra inferior com [tabs] (uma pilha por aba, que guarda o lugar ao trocar) e as telas do modo.
 * Telas internas (sem barra) entram por cima com a animação de "empurrar" do Material.
 */
@Composable
fun ModeShell(
    tabs: List<Tab>,
    tag: String,
    graph: NavGraphBuilder.(nav: NavHostController, padding: PaddingValues) -> Unit
) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val dest = entry?.destination
    val onTab = tabs.any { t -> dest?.hierarchy?.any { it.hasRoute(t.routeClass) } == true }

    Scaffold(
        modifier = Modifier.testTag(tag),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (onTab) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
                    tabs.forEach { tab ->
                        val selected = dest?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.ext.navIndicator
                            ),
                            modifier = Modifier.testTag("tab_${tab.label.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = nav,
                startDestination = tabs.first().route,
                enterTransition = { slideInHorizontally(tween(260)) { it / 5 } + fadeIn(tween(260)) },
                exitTransition = { fadeOut(tween(160)) },
                popEnterTransition = { fadeIn(tween(220)) },
                popExitTransition = { slideOutHorizontally(tween(240)) { it / 5 } + fadeOut(tween(200)) }
            ) {
                graph(nav, padding)
                composable<Soon> { e ->
                    val info = SoonCatalog[e.toRoute<Soon>().key]
                    Scaffold(topBar = { BackTopBar(info.title, onBack = { nav.popBackStack() }) }, containerColor = MaterialTheme.colorScheme.background) { p ->
                        SoonContent(info.title, info.icon, info.color, info.description, Modifier.padding(p))
                    }
                }
            }
        }
    }
}

/** Abre uma tela "Em construção" do catálogo. */
fun NavHostController.soon(key: String) = navigate(Soon(key))
