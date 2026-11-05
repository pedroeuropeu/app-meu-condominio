package br.com.condovitta.app.feature.portaria

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SensorOccupied
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import br.com.condovitta.app.core.designsystem.MenuRow
import br.com.condovitta.app.core.designsystem.SectionHeader
import br.com.condovitta.app.core.designsystem.ext
import br.com.condovitta.app.domain.Busca
import br.com.condovitta.app.domain.Me
import br.com.condovitta.app.domain.Perfil
import br.com.condovitta.app.feature.home.GreetingHero
import br.com.condovitta.app.feature.home.QuickAction
import br.com.condovitta.app.feature.home.QuickActions
import kotlinx.coroutines.delay

/** Para onde as telas da portaria levam (o grafo do modo liga cada um à sua rota). */
data class PortariaNav(
    val visitante: (Long) -> Unit,
    val novaVisita: () -> Unit,
    val bloqueados: () -> Unit,
    val encomenda: (Long) -> Unit,
    val novaEncomenda: () -> Unit,
    val abaVisitantes: () -> Unit,
    val abaEncomendas: () -> Unit,
    val soon: (String) -> Unit,
    val back: () -> Unit
)

/** A tela que o porteiro mais usa: busca grande, números do turno, ações de um toque e quem está chegando. */
@Composable
fun PainelScreen(me: Me, perfil: Perfil, padding: PaddingValues, nav: PortariaNav, vm: PainelViewModel = hiltViewModel()) {
    val painel by vm.painel.collectAsStateWithLifecycle()
    // o texto fica no próprio campo (síncrono); o ViewModel recebe e busca com atraso
    var query by rememberSaveable { mutableStateOf(vm.query.value) }
    fun onQuery(q: String) { query = q; vm.onQuery(q) }
    val busca by vm.busca.collectAsStateWithLifecycle()

    // atualiza ao abrir e a cada 30 s enquanto a tela está na frente (o turno muda o tempo todo)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            vm.refresh(silent = vm.painel.value.data != null)
            while (true) {
                delay(30_000)
                vm.refresh(silent = true)
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = painel.loading && painel.data != null,
        onRefresh = { vm.refresh() },
        modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            val p = painel.data
            GreetingHero(
                me.iniciais, listOfNotNull("Portaria", p?.turno?.let { "turno da $it" }, p?.faixa).joinToString(" · "),
                me.primeiroNome, perfil.condominioNome, onBell = { nav.soon("port/notifs") }
            ) {
                Spacer(Modifier.height(16.dp))
                TextField(
                    value = query, onValueChange = ::onQuery,
                    placeholder = { Text("Buscar morador, unidade ou placa…") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Rounded.Close, contentDescription = "Limpar") } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White, unfocusedContainerColor = Color.White.copy(alpha = 0.94f),
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color(0xFF0A2C4E), unfocusedTextColor = Color(0xFF0A2C4E),
                        focusedPlaceholderColor = Color(0xFF5B6B82), unfocusedPlaceholderColor = Color(0xFF5B6B82),
                        focusedLeadingIconColor = Color(0xFF5B6B82), unfocusedLeadingIconColor = Color(0xFF5B6B82),
                        focusedTrailingIconColor = Color(0xFF5B6B82), unfocusedTrailingIconColor = Color(0xFF5B6B82)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("painel_busca")
                )
            }

            Column(Modifier.padding(horizontal = 16.dp)) {
                val b = busca
                if (b != null) {
                    Spacer(Modifier.height(14.dp))
                    when {
                        b.data != null -> BuscaResultados(b.data, nav)
                        b.loading -> LoadingBox()
                        b.error != null -> NoteBox(Icons.Rounded.Warning, MaterialTheme.ext.warn, b.error)
                    }
                    Spacer(Modifier.height(24.dp))
                    return@Column
                }

                when {
                    p == null && painel.loading -> LoadingBox()
                    p == null -> ErrorBox(painel.error ?: "Não deu para carregar o painel.", onRetry = { vm.refresh() })
                    else -> {
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Kpi(Icons.Rounded.Schedule, Color(0xFF1F4FE0), p.esperados.size.toString(), "Esperados hoje", Modifier.weight(1f), onClick = nav.abaVisitantes)
                            Kpi(Icons.Rounded.SensorOccupied, Color(0xFF059669), p.dentro.size.toString(), "No condomínio", Modifier.weight(1f), onClick = nav.abaVisitantes)
                            Kpi(Icons.Rounded.Inventory2, Color(0xFFD97706), p.encomendasAguardando.toString(), "Encomendas", Modifier.weight(1f), onClick = nav.abaEncomendas)
                        }
                        if (p.pereciveis.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            NoteBox(
                                Icons.Rounded.WaterDrop, Color(0xFF0891B2),
                                "${p.pereciveis.size} perecível(is) na geladeira: " + p.pereciveis.joinToString { it.unidade } + "."
                            )
                        }
                        if (p.encomendasParadas7 > 0) {
                            Spacer(Modifier.height(10.dp))
                            NoteBox(Icons.Rounded.Warning, MaterialTheme.ext.danger, "${p.encomendasParadas7} encomenda(s) parada(s) há mais de 7 dias.")
                        }

                        SectionHeader("Ações rápidas")
                        QuickActions(
                            listOf(
                                QuickAction("Registrar encomenda", Icons.Rounded.Inventory2, Color(0xFFD97706), nav.novaEncomenda),
                                QuickAction("Nova visita", Icons.Rounded.PersonAdd, Color(0xFF1F4FE0), nav.novaVisita),
                                QuickAction("Entregar", Icons.Rounded.LocalShipping, Color(0xFF059669), nav.abaEncomendas),
                                QuickAction("Bloqueados", Icons.Rounded.Block, Color(0xFFDC2626), nav.bloqueados),
                                QuickAction("Reservas de hoje", Icons.Rounded.Event, Color(0xFF7C3AED)) { nav.soon("port/reservas") },
                                QuickAction("Livro", Icons.AutoMirrored.Rounded.MenuBook, Color(0xFF64748B)) { nav.soon("port/livro") }
                            )
                        )

                        SectionTitleLink("Esperados agora", nav.abaVisitantes)
                        if (p.esperados.isEmpty()) EmptyBox(Icons.Rounded.Schedule, "Ninguém esperado agora", "As autorizações dos moradores aparecem aqui.")
                        else CardList(p.esperados.take(3)) { v -> VisitaRow(v) { nav.visitante(v.visitanteId) } }

                        SectionTitleLink("No condomínio agora", nav.abaVisitantes)
                        if (p.dentro.isEmpty()) EmptyBox(Icons.Rounded.SensorOccupied, "Nenhum visitante dentro", "Quem entrar aparece aqui até registrar a saída.")
                        else CardList(p.dentro.take(3)) { v -> VisitaRow(v) { nav.visitante(v.visitanteId) } }
                        Spacer(Modifier.height(96.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitleLink(title: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { SectionHeader(title) }
        TextButton(onClick = onClick, modifier = Modifier.padding(top = 10.dp)) {
            Text("Ver todos")
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun BuscaResultados(b: Busca, nav: PortariaNav) {
    b.bloqueados.firstOrNull()?.let {
        NoteBox(Icons.Rounded.Warning, MaterialTheme.ext.danger, "Atenção: ${it.nome} está na lista de bloqueados. ${it.motivo.orEmpty()}")
        Spacer(Modifier.height(10.dp))
    }
    if (b.vazia) {
        EmptyBox(Icons.Rounded.Search, "Nada encontrado", "Tente outro nome, unidade ou placa.")
        return
    }
    if (b.visitantes.isNotEmpty()) {
        SectionHeader("Visitantes de hoje")
        CardList(b.visitantes) { v -> VisitaRow(v) { nav.visitante(v.visitanteId) } }
    }
    if (b.unidades.isNotEmpty()) {
        SectionHeader("Unidades")
        CardList(b.unidades) { u ->
            MenuRow(
                Icons.Rounded.Apartment, Color(0xFF1F4FE0), "${u.unidade} · ${u.morador ?: "Sem morador cadastrado"}",
                listOfNotNull(
                    "${u.moradores} morador(es)",
                    if (u.encomendasAguardando > 0) "${u.encomendasAguardando} encomenda(s) na portaria" else null
                ).joinToString(" · "),
                onClick = { if (u.encomendasAguardando > 0) nav.abaEncomendas() else nav.novaVisita() }
            )
        }
    }
    if (b.veiculos.isNotEmpty()) {
        SectionHeader("Veículos")
        CardList(b.veiculos) { v ->
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Plate(v.placa)
                Column(Modifier.weight(1f)) {
                    Text(listOfNotNull(v.modelo, v.cor).joinToString(" "), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink)
                    Text(listOfNotNull(v.unidade, v.morador, v.acesso).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted)
                    if (v.bloqueado && v.obs != null) Text(v.obs, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.danger)
                }
                if (v.bloqueado) br.com.condovitta.app.core.designsystem.StatusBadge("Bloqueado", MaterialTheme.ext.danger, MaterialTheme.ext.dangerContainer)
            }
        }
    }
}
