package br.com.condovitta.app.feature.portaria

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.runtime.Composable
import androidx.navigation.compose.composable
import br.com.condovitta.app.domain.Me
import br.com.condovitta.app.domain.Perfil
import br.com.condovitta.app.feature.mais.MaisActions
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

/**
 * Modo Portaria: simples e de toque fácil. Sem barra inferior — o Início (busca e 6 botões grandes) é o ponto de partida
 * e o menu do cabeçalho guarda o resto da rotina. Cabeçalho escuro e ícones azuis, separado do app do morador.
 */
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
        tabs = listOf(Tab(PortPainel, "Início", Icons.Rounded.GridView))
    ) { nav, padding ->
        val pnav = PortariaNav(
            visitante = { nav.navigate(VisitanteDetalhe(it)) },
            novaVisita = { nav.navigate(NovaVisitaRoute) },
            bloqueados = { nav.navigate(BloqueadosRoute) },
            encomenda = { nav.navigate(EncomendaDetalhe(it)) },
            novaEncomenda = { nav.navigate(NovaEncomendaRoute) },
            abaVisitantes = { nav.navigate(PortVisitantes()) { launchSingleTop = true } },
            abaEncomendas = { nav.navigate(PortEncomendas) { launchSingleTop = true } },
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
            notificacoes = { nav.navigate(br.com.condovitta.app.ui.navigation.NotificacoesRoute) },
            dentro = { nav.navigate(PortVisitantes(dentro = true)) { launchSingleTop = true } },
            menu = { nav.navigate(PortMais) { launchSingleTop = true } }
        )
        composable<PortPainel> { PainelScreen(me, perfil, padding, pnav, messenger, extras.naoLidas) }
        composable<PortVisitantes> { VisitantesScreen(pnav) }
        composable<PortEncomendas> { EncomendasScreen(pnav) }
        composable<PortMais> { PortariaMenuScreen(me, perfil, mais, pnav) }
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
    when (d) {
        br.com.condovitta.app.domain.Destino.PORTARIA_ACESSO, br.com.condovitta.app.domain.Destino.VISITANTES -> nav.navigate(PortVisitantes()) { launchSingleTop = true }
        br.com.condovitta.app.domain.Destino.PORTARIA_ENCOMENDAS, br.com.condovitta.app.domain.Destino.ENCOMENDAS -> nav.navigate(PortEncomendas) { launchSingleTop = true }
        br.com.condovitta.app.domain.Destino.RESERVAS -> nav.navigate(PortReservasRoute)
        br.com.condovitta.app.domain.Destino.PORTARIA_GAS -> nav.navigate(br.com.condovitta.app.ui.navigation.GasRoute)
        else -> nav.navigate(br.com.condovitta.app.ui.navigation.NotificacoesRoute)
    }
}
