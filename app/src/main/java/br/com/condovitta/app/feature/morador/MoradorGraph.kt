package br.com.condovitta.app.feature.morador

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.Description
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.composable
import br.com.condovitta.app.core.designsystem.SectionHeader
import br.com.condovitta.app.core.designsystem.SoonContent
import br.com.condovitta.app.domain.Me
import br.com.condovitta.app.domain.Perfil
import br.com.condovitta.app.feature.home.GreetingHero
import br.com.condovitta.app.feature.home.PhaseNote
import br.com.condovitta.app.feature.home.QuickAction
import br.com.condovitta.app.feature.home.QuickActions
import br.com.condovitta.app.feature.home.saudacao
import br.com.condovitta.app.feature.mais.MaisActions
import br.com.condovitta.app.feature.mais.MaisScreen
import br.com.condovitta.app.ui.ModeShell
import br.com.condovitta.app.ui.Tab
import br.com.condovitta.app.ui.navigation.AppBoletos
import br.com.condovitta.app.ui.navigation.AppInicio
import br.com.condovitta.app.ui.navigation.AppMais
import br.com.condovitta.app.ui.navigation.AppReservas
import br.com.condovitta.app.ui.navigation.SoonCatalog
import br.com.condovitta.app.ui.soon

/** App do morador (completo): Início, Reservas, Boletos e Mais, nas cores da marca. */
@Composable
fun MoradorApp(me: Me, perfil: Perfil, mais: MaisActions) {
    ModeShell(
        tag = "modo_morador",
        tabs = listOf(
            Tab(AppInicio, "Início", Icons.Outlined.Home, Icons.Rounded.Home),
            Tab(AppReservas, "Reservas", Icons.Outlined.CalendarMonth, Icons.Rounded.CalendarMonth),
            Tab(AppBoletos, "Boletos", Icons.Outlined.AccountBalanceWallet, Icons.Rounded.AccountBalanceWallet),
            Tab(AppMais, "Mais", Icons.Rounded.Menu)
        )
    ) { nav, padding ->
        composable<AppInicio> { Inicio(me, perfil, padding, onSoon = nav::soon) }
        composable<AppReservas> { SoonTab("app/reservas", padding) }
        composable<AppBoletos> { SoonTab("app/boletos", padding) }
        composable<AppMais> { MaisScreen(me, perfil, mais, padding, onSoon = nav::soon) }
    }
}

@Composable
private fun Inicio(me: Me, perfil: Perfil, padding: PaddingValues, onSoon: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding()).verticalScroll(rememberScrollState())) {
        GreetingHero(
            me.iniciais, saudacao(java.time.LocalTime.now().hour), me.primeiroNome,
            listOfNotNull(perfil.unidade, perfil.condominioNome).joinToString(" · "),
            onBell = { onSoon("app/notifs") }
        )
        Column(Modifier.padding(horizontal = 16.dp)) {
            SectionHeader("Atalhos")
            QuickActions(
                listOf(
                    QuickAction("Visitantes", Icons.Rounded.Badge, Color(0xFF1F4FE0)) { onSoon("app/visitantes") },
                    QuickAction("Encomendas", Icons.Rounded.Inventory2, Color(0xFFD97706)) { onSoon("app/encomendas") },
                    QuickAction("Avisos", Icons.Rounded.Campaign, Color(0xFF0D9488)) { onSoon("app/avisos") },
                    QuickAction("Chamados", Icons.Rounded.SupportAgent, Color(0xFF0891B2)) { onSoon("app/chamados") },
                    QuickAction("Documentos", Icons.Rounded.Description, Color(0xFF64748B)) { onSoon("app/documentos") }
                )
            )
            Spacer(Modifier.height(18.dp))
            PhaseNote("O boleto do mês em destaque (com Pix), as encomendas para retirar, as próximas reservas e os avisos chegam nas próximas fases.")
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SoonTab(key: String, padding: PaddingValues) {
    val info = SoonCatalog[key]
    SoonContent(info.title, info.icon, info.color, info.description, contentPadding = padding)
}
