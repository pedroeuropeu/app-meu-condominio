package br.com.condovitta.app.feature.portaria

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.composable
import br.com.condovitta.app.domain.Me
import br.com.condovitta.app.domain.Perfil
import br.com.condovitta.app.feature.mais.MaisActions
import br.com.condovitta.app.feature.mais.MaisScreen
import br.com.condovitta.app.ui.ModeShell
import br.com.condovitta.app.ui.Tab
import br.com.condovitta.app.ui.navigation.BloqueadosRoute
import br.com.condovitta.app.ui.navigation.EncomendaDetalhe
import br.com.condovitta.app.ui.navigation.NovaEncomendaRoute
import br.com.condovitta.app.ui.navigation.NovaVisitaRoute
import br.com.condovitta.app.ui.navigation.PortEncomendas
import br.com.condovitta.app.ui.navigation.PortMais
import br.com.condovitta.app.ui.navigation.PortPainel
import br.com.condovitta.app.ui.navigation.PortVisitantes
import br.com.condovitta.app.ui.navigation.VisitanteDetalhe
import br.com.condovitta.app.ui.soon

/** Modo Portaria (rápido): Painel, Visitantes, Encomendas e Mais, no acento âmbar. */
@Composable
fun PortariaApp(me: Me, perfil: Perfil, mais: MaisActions) {
    ModeShell(
        tag = "modo_portaria",
        tabs = listOf(
            Tab(PortPainel, "Painel", Icons.Outlined.GridView, Icons.Rounded.GridView),
            Tab(PortVisitantes, "Visitantes", Icons.Outlined.Badge, Icons.Rounded.Badge),
            Tab(PortEncomendas, "Encomendas", Icons.Outlined.Inventory2, Icons.Rounded.Inventory2),
            Tab(PortMais, "Mais", Icons.Rounded.Menu)
        )
    ) { nav, padding ->
        fun tab(route: Any) = nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        val pnav = PortariaNav(
            visitante = { nav.navigate(VisitanteDetalhe(it)) },
            novaVisita = { nav.navigate(NovaVisitaRoute) },
            bloqueados = { nav.navigate(BloqueadosRoute) },
            encomenda = { nav.navigate(EncomendaDetalhe(it)) },
            novaEncomenda = { nav.navigate(NovaEncomendaRoute) },
            abaVisitantes = { tab(PortVisitantes) },
            abaEncomendas = { tab(PortEncomendas) },
            soon = { key -> if (key == "port/bloqueados") nav.navigate(BloqueadosRoute) else nav.soon(key) },
            back = { nav.popBackStack() }
        )
        composable<PortPainel> { PainelScreen(me, perfil, padding, pnav) }
        composable<PortVisitantes> { VisitantesScreen(padding, pnav) }
        composable<PortEncomendas> { EncomendasScreen(padding, pnav) }
        composable<PortMais> { MaisScreen(me, perfil, mais, padding, onSoon = pnav.soon) }
        composable<VisitanteDetalhe> { VisitanteScreen(pnav) }
        composable<NovaVisitaRoute> { NovaVisitaScreen(pnav) }
        composable<BloqueadosRoute> { BloqueadosScreen(pnav) }
        composable<EncomendaDetalhe> { EncomendaScreen(pnav) }
        composable<NovaEncomendaRoute> { NovaEncomendaScreen(pnav) }
    }
}
