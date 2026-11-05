package br.com.condovitta.app.feature.portaria

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PersonAdd
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
import br.com.condovitta.app.feature.mais.MaisActions
import br.com.condovitta.app.feature.mais.MaisScreen
import br.com.condovitta.app.ui.ModeShell
import br.com.condovitta.app.ui.Tab
import br.com.condovitta.app.ui.navigation.PortEncomendas
import br.com.condovitta.app.ui.navigation.PortMais
import br.com.condovitta.app.ui.navigation.PortPainel
import br.com.condovitta.app.ui.navigation.PortVisitantes
import br.com.condovitta.app.ui.navigation.SoonCatalog
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
        composable<PortPainel> { PainelTurno(me, perfil, padding, onSoon = nav::soon) }
        composable<PortVisitantes> { SoonTab("port/visitantes", padding) }
        composable<PortEncomendas> { SoonTab("port/encomendas", padding) }
        composable<PortMais> { MaisScreen(me, perfil, mais, padding, onSoon = nav::soon) }
    }
}

@Composable
private fun PainelTurno(me: Me, perfil: Perfil, padding: PaddingValues, onSoon: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding()).verticalScroll(rememberScrollState())) {
        GreetingHero(me.iniciais, "Portaria", me.primeiroNome, perfil.condominioNome, onBell = { onSoon("port/notifs") })
        Column(Modifier.padding(horizontal = 16.dp)) {
            SectionHeader("Ações rápidas")
            QuickActions(
                listOf(
                    QuickAction("Registrar encomenda", Icons.Rounded.Inventory2, Color(0xFFD97706)) { onSoon("port/registrar-encomenda") },
                    QuickAction("Nova visita", Icons.Rounded.PersonAdd, Color(0xFF1F4FE0)) { onSoon("port/nova-visita") },
                    QuickAction("Reservas de hoje", Icons.Rounded.Event, Color(0xFF7C3AED)) { onSoon("port/reservas") },
                    QuickAction("Bloqueados", Icons.Rounded.Block, Color(0xFFDC2626)) { onSoon("port/bloqueados") },
                    QuickAction("Livro", Icons.AutoMirrored.Rounded.MenuBook, Color(0xFF64748B)) { onSoon("port/livro") },
                    QuickAction("Veículos", Icons.Rounded.DirectionsCar, Color(0xFF0891B2)) { onSoon("port/veiculos") }
                )
            )
            Spacer(Modifier.height(18.dp))
            PhaseNote("O painel do turno (busca por morador, unidade ou placa e os números de esperados, no condomínio e encomendas) chega na próxima fase, ligado à portaria do condomínio.")
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
internal fun SoonTab(key: String, padding: PaddingValues) {
    val info = SoonCatalog[key]
    SoonContent(info.title, info.icon, info.color, info.description, contentPadding = padding)
}
