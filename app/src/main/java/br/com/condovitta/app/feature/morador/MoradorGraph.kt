package br.com.condovitta.app.feature.morador

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.composable
import br.com.condovitta.app.domain.Me
import br.com.condovitta.app.domain.Perfil
import br.com.condovitta.app.feature.mais.MaisActions
import br.com.condovitta.app.feature.mais.MaisScreen
import br.com.condovitta.app.ui.Messenger
import br.com.condovitta.app.ui.ModeShell
import br.com.condovitta.app.ui.Tab
import br.com.condovitta.app.ui.navigation.AppAvisos
import br.com.condovitta.app.ui.navigation.AppBoletos
import br.com.condovitta.app.ui.navigation.AppEncomendas
import br.com.condovitta.app.ui.navigation.AppInicio
import br.com.condovitta.app.ui.navigation.AppMais
import br.com.condovitta.app.ui.navigation.AppReservas
import br.com.condovitta.app.ui.navigation.AppVisitantes
import br.com.condovitta.app.ui.navigation.AutorizacaoDetalhe
import br.com.condovitta.app.ui.navigation.NovaAutorizacaoRoute
import br.com.condovitta.app.ui.navigation.NovaReservaRoute
import br.com.condovitta.app.feature.reservas.NovaReservaScreen
import br.com.condovitta.app.feature.reservas.ReservasScreen
import br.com.condovitta.app.ui.soon

/** App do morador (completo): Início, Reservas, Boletos e Mais, nas cores da marca. */
@Composable
fun MoradorApp(me: Me, perfil: Perfil, mais: MaisActions, messenger: Messenger) {
    ModeShell(
        tag = "modo_morador",
        tabs = listOf(
            Tab(AppInicio, "Início", Icons.Outlined.Home, Icons.Rounded.Home),
            Tab(AppReservas, "Reservas", Icons.Outlined.CalendarMonth, Icons.Rounded.CalendarMonth),
            Tab(AppBoletos, "Boletos", Icons.Outlined.AccountBalanceWallet, Icons.Rounded.AccountBalanceWallet),
            Tab(AppMais, "Mais", Icons.Rounded.Menu)
        )
    ) { nav, padding ->
        fun tab(route: Any) = nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        val mnav = MoradorNav(
            visitantes = { nav.navigate(AppVisitantes) },
            novaAutorizacao = { nav.navigate(NovaAutorizacaoRoute) },
            autorizacao = { nav.navigate(AutorizacaoDetalhe(it)) },
            encomendas = { nav.navigate(AppEncomendas) },
            avisos = { nav.navigate(AppAvisos) },
            abaBoletos = { tab(AppBoletos) },
            abaReservas = { tab(AppReservas) },
            soon = { key ->
                when (key) {
                    "app/visitantes" -> nav.navigate(AppVisitantes)
                    "app/encomendas" -> nav.navigate(AppEncomendas)
                    "app/avisos" -> nav.navigate(AppAvisos)
                    else -> nav.soon(key)
                }
            },
            back = { nav.popBackStack() }
        )
        composable<AppInicio> { InicioScreen(me, perfil, padding, mnav, messenger) }
        composable<AppReservas> { ReservasScreen(padding, onReservar = { nav.navigate(NovaReservaRoute(it)) }) }
        composable<AppBoletos> { BoletosScreen(padding, messenger) }
        composable<AppMais> { MaisScreen(me, perfil, mais, padding, onSoon = mnav.soon) }
        composable<AppVisitantes> { VisitasScreen(mnav) }
        composable<NovaAutorizacaoRoute> { NovaAutorizacaoScreen(mnav, perfil.condominioNome, perfil.unidade) }
        composable<AutorizacaoDetalhe> { AutorizacaoScreen(mnav, perfil.condominioNome, perfil.unidade) }
        composable<AppEncomendas> { EncomendasMoradorScreen(mnav) }
        composable<AppAvisos> { AvisosScreen(mnav) }
        composable<NovaReservaRoute> { NovaReservaScreen(onBack = { nav.popBackStack() }, onConcluir = { nav.popBackStack() }) }
    }
}
