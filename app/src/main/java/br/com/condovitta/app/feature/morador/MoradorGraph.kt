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
import br.com.condovitta.app.feature.servicos.AssembleiasScreen
import br.com.condovitta.app.feature.servicos.CalendarioScreen
import br.com.condovitta.app.feature.servicos.ChamadoScreen
import br.com.condovitta.app.feature.servicos.ChamadosScreen
import br.com.condovitta.app.feature.servicos.DocumentoScreen
import br.com.condovitta.app.feature.servicos.DocumentosScreen
import br.com.condovitta.app.feature.servicos.GasMoradorScreen
import br.com.condovitta.app.feature.servicos.NovaOcorrenciaScreen
import br.com.condovitta.app.feature.servicos.NovoChamadoScreen
import br.com.condovitta.app.feature.servicos.OcorrenciasScreen
import br.com.condovitta.app.feature.servicos.PlenarioScreen
import br.com.condovitta.app.feature.servicos.PrestacaoScreen
import br.com.condovitta.app.ui.navigation.AssembleiasRoute
import br.com.condovitta.app.ui.navigation.CalendarioRoute
import br.com.condovitta.app.ui.navigation.ChamadoRoute
import br.com.condovitta.app.ui.navigation.ChamadosRoute
import br.com.condovitta.app.ui.navigation.DocumentoRoute
import br.com.condovitta.app.ui.navigation.DocumentosRoute
import br.com.condovitta.app.ui.navigation.GasMoradorRoute
import br.com.condovitta.app.ui.navigation.NovaOcorrenciaRoute
import br.com.condovitta.app.ui.navigation.NovoChamadoRoute
import br.com.condovitta.app.ui.navigation.OcorrenciasRoute
import br.com.condovitta.app.ui.navigation.PlenarioRoute
import br.com.condovitta.app.ui.navigation.PrestacaoRoute

/** App do morador (completo): Início, Reservas, Boletos e Mais, nas cores da marca. */
@Composable
fun MoradorApp(me: Me, perfil: Perfil, mais: MaisActions, messenger: Messenger, extras: br.com.condovitta.app.ui.ModoExtras = br.com.condovitta.app.ui.ModoExtras()) {
    ModeShell(
        tag = "modo_morador",
        efeito = { nav ->
            androidx.compose.runtime.LaunchedEffect(extras.destino) {
                val d = extras.destino ?: return@LaunchedEffect
                extras.consumirDestino()
                abrirMorador(d, nav)
            }
        },
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
                    "app/unidade" -> nav.navigate(br.com.condovitta.app.ui.navigation.MinhaUnidadeRoute)
                    "app/notifs" -> nav.navigate(br.com.condovitta.app.ui.navigation.NotificacoesRoute)
                    "app/chamados" -> nav.navigate(ChamadosRoute)
                    "app/ocorrencias" -> nav.navigate(OcorrenciasRoute)
                    "app/documentos" -> nav.navigate(DocumentosRoute)
                    "app/assembleias" -> nav.navigate(AssembleiasRoute)
                    "app/prestacao" -> nav.navigate(PrestacaoRoute)
                    "app/gas" -> nav.navigate(GasMoradorRoute)
                    "app/calendario" -> nav.navigate(CalendarioRoute)
                    else -> nav.soon(key)
                }
            },
            back = { nav.popBackStack() },
            notificacoes = { nav.navigate(br.com.condovitta.app.ui.navigation.NotificacoesRoute) }
        )
        composable<AppInicio> { InicioScreen(me, perfil, padding, mnav, messenger, extras.naoLidas) }
        composable<AppReservas> { ReservasScreen(padding, onReservar = { nav.navigate(NovaReservaRoute(it)) }) }
        composable<AppBoletos> { BoletosScreen(padding, messenger) }
        composable<AppMais> { MaisScreen(me, perfil, mais, padding, onSoon = mnav.soon) }
        composable<AppVisitantes> { VisitasScreen(mnav) }
        composable<NovaAutorizacaoRoute> { NovaAutorizacaoScreen(mnav, perfil.condominioNome, perfil.unidade) }
        composable<AutorizacaoDetalhe> { AutorizacaoScreen(mnav, perfil.condominioNome, perfil.unidade) }
        composable<AppEncomendas> { EncomendasMoradorScreen(mnav) }
        composable<AppAvisos> { AvisosScreen(mnav) }
        composable<br.com.condovitta.app.ui.navigation.MinhaUnidadeRoute> { br.com.condovitta.app.feature.rotina.MinhaUnidadeScreen(onBack = mnav.back) }
        composable<br.com.condovitta.app.ui.navigation.NotificacoesRoute> {
            br.com.condovitta.app.feature.notificacoes.NotificacoesScreen(onBack = mnav.back, onAbrir = { d -> abrirMorador(d, nav) })
        }
        composable<NovaReservaRoute> { NovaReservaScreen(onBack = { nav.popBackStack() }, onConcluir = { nav.popBackStack() }) }

        // serviços do morador
        composable<ChamadosRoute> { ChamadosScreen(onBack = mnav.back, onAbrir = { nav.navigate(ChamadoRoute(it)) }, onNovo = { nav.navigate(NovoChamadoRoute) }) }
        composable<NovoChamadoRoute> {
            NovoChamadoScreen(onBack = mnav.back, onCriado = { id -> nav.navigate(ChamadoRoute(id)) { popUpTo<NovoChamadoRoute> { inclusive = true } } })
        }
        composable<ChamadoRoute> { ChamadoScreen(onBack = mnav.back) }
        composable<OcorrenciasRoute> { OcorrenciasScreen(onBack = mnav.back, onNova = { nav.navigate(NovaOcorrenciaRoute) }) }
        composable<NovaOcorrenciaRoute> { NovaOcorrenciaScreen(onBack = mnav.back) }
        composable<DocumentosRoute> { DocumentosScreen(onBack = mnav.back, onAbrir = { nav.navigate(DocumentoRoute(it)) }) }
        composable<DocumentoRoute> { DocumentoScreen(onBack = mnav.back) }
        composable<AssembleiasRoute> { AssembleiasScreen(onBack = mnav.back, onPlenario = { nav.navigate(PlenarioRoute(it)) }) }
        composable<PlenarioRoute> { PlenarioScreen(onBack = mnav.back) }
        composable<PrestacaoRoute> { PrestacaoScreen(onBack = mnav.back) }
        composable<GasMoradorRoute> { GasMoradorScreen(onBack = mnav.back) }
        composable<CalendarioRoute> { CalendarioScreen(onBack = mnav.back) }
    }
}

/** Tela do morador para cada destino de notificação. */
internal fun abrirMorador(d: br.com.condovitta.app.domain.Destino, nav: androidx.navigation.NavHostController) {
    fun tab(route: Any) = nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
    when (d) {
        br.com.condovitta.app.domain.Destino.ENCOMENDAS -> nav.navigate(AppEncomendas)
        br.com.condovitta.app.domain.Destino.VISITANTES -> nav.navigate(AppVisitantes)
        br.com.condovitta.app.domain.Destino.BOLETOS -> tab(AppBoletos)
        br.com.condovitta.app.domain.Destino.RESERVAS -> tab(AppReservas)
        br.com.condovitta.app.domain.Destino.AVISOS -> nav.navigate(AppAvisos)
        else -> nav.navigate(br.com.condovitta.app.ui.navigation.NotificacoesRoute)
    }
}
