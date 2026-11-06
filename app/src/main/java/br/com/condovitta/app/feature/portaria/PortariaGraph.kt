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
import br.com.condovitta.app.ui.navigation.PortReservasRoute
import br.com.condovitta.app.feature.reservas.PortariaReservasScreen
import br.com.condovitta.app.ui.navigation.PortVisitantes
import br.com.condovitta.app.ui.navigation.VisitanteDetalhe
import br.com.condovitta.app.ui.soon

/** Modo Portaria (rápido): Painel, Visitantes, Encomendas e Mais, no acento âmbar. */
@Composable
fun PortariaApp(me: Me, perfil: Perfil, mais: MaisActions, messenger: br.com.condovitta.app.ui.Messenger, extras: br.com.condovitta.app.ui.ModoExtras = br.com.condovitta.app.ui.ModoExtras()) {
    ModeShell(
        tag = "modo_portaria",
        efeito = { nav ->
            androidx.compose.runtime.LaunchedEffect(extras.destino) {
                val d = extras.destino ?: return@LaunchedEffect
                extras.consumirDestino()
                abrirPortaria(d, null, nav)
            }
        },
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
            soon = { key ->
                when (key) {
                    "port/bloqueados" -> nav.navigate(BloqueadosRoute)
                    "port/reservas" -> nav.navigate(PortReservasRoute)
                    "port/livro" -> nav.navigate(br.com.condovitta.app.ui.navigation.LivroRoute)
                    "port/veiculos" -> nav.navigate(br.com.condovitta.app.ui.navigation.VeiculosRoute)
                    "port/gas" -> nav.navigate(br.com.condovitta.app.ui.navigation.GasRoute)
                    "port/notifs" -> nav.navigate(br.com.condovitta.app.ui.navigation.NotificacoesRoute)
                    else -> nav.soon(key)
                }
            },
            back = { nav.popBackStack() },
            notificacoes = { nav.navigate(br.com.condovitta.app.ui.navigation.NotificacoesRoute) }
        )
        composable<PortPainel> { PainelScreen(me, perfil, padding, pnav, messenger, extras.naoLidas) }
        composable<PortVisitantes> { VisitantesScreen(padding, pnav) }
        composable<PortEncomendas> { EncomendasScreen(padding, pnav) }
        composable<PortMais> { MaisScreen(me, perfil, mais, padding, onSoon = pnav.soon) }
        composable<VisitanteDetalhe> { VisitanteScreen(pnav) }
        composable<NovaVisitaRoute> { NovaVisitaScreen(pnav) }
        composable<BloqueadosRoute> { BloqueadosScreen(pnav) }
        composable<EncomendaDetalhe> { EncomendaScreen(pnav) }
        composable<NovaEncomendaRoute> { NovaEncomendaScreen(pnav) }
        composable<PortReservasRoute> { PortariaReservasScreen(onBack = pnav.back) }
        composable<br.com.condovitta.app.ui.navigation.LivroRoute> { br.com.condovitta.app.feature.rotina.LivroScreen(onBack = pnav.back) }
        composable<br.com.condovitta.app.ui.navigation.VeiculosRoute> { br.com.condovitta.app.feature.rotina.VeiculosScreen(onBack = pnav.back) }
        composable<br.com.condovitta.app.ui.navigation.GasRoute> { br.com.condovitta.app.feature.rotina.GasScreen(onBack = pnav.back) }
        composable<br.com.condovitta.app.ui.navigation.NotificacoesRoute> {
            br.com.condovitta.app.feature.notificacoes.NotificacoesScreen(onBack = pnav.back, onAbrir = { d -> abrirPortaria(d, pnav, nav) })
        }
    }
}

/** Tela da portaria para cada destino de notificação. */
internal fun abrirPortaria(d: br.com.condovitta.app.domain.Destino, pnav: PortariaNav?, nav: androidx.navigation.NavHostController) {
    fun tab(route: Any) = nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
    when (d) {
        br.com.condovitta.app.domain.Destino.PORTARIA_ACESSO, br.com.condovitta.app.domain.Destino.VISITANTES -> tab(PortVisitantes)
        br.com.condovitta.app.domain.Destino.PORTARIA_ENCOMENDAS, br.com.condovitta.app.domain.Destino.ENCOMENDAS -> tab(PortEncomendas)
        br.com.condovitta.app.domain.Destino.RESERVAS -> nav.navigate(PortReservasRoute)
        br.com.condovitta.app.domain.Destino.PORTARIA_GAS -> nav.navigate(br.com.condovitta.app.ui.navigation.GasRoute)
        else -> nav.navigate(br.com.condovitta.app.ui.navigation.NotificacoesRoute)
    }
}
