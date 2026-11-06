package br.com.condovitta.app.feature.portaria

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
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
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.ui.platform.LocalContext
import br.com.condovitta.app.core.common.codigoDoQr
import br.com.condovitta.app.core.common.lerQr
import br.com.condovitta.app.ui.Messenger
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
    val back: () -> Unit,
    val notificacoes: () -> Unit = {},
    /** visitantes já abertos em "No condomínio" */
    val dentro: () -> Unit = abaVisitantes,
    /** menu da portaria (rotina e conta) */
    val menu: () -> Unit = {}
)

/**
 * Início da portaria: cabeçalho escuro com a busca (nome, unidade, placa ou QR) e 6 botões grandes de um toque. Os números
 * do turno ficam nos próprios botões; o resto da rotina está no menu do cabeçalho.
 */
@Composable
fun PainelScreen(me: Me, perfil: Perfil, padding: PaddingValues, nav: PortariaNav, messenger: Messenger, naoLidas: Long = 0, vm: PainelViewModel = hiltViewModel()) {
    val context = LocalContext.current
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
            PortariaHero(
                me.iniciais, listOfNotNull("Portaria", p?.turno?.let { "turno da $it" }).joinToString(" · ").uppercase(),
                me.primeiroNome, perfil.condominioNome, naoLidas, onBell = nav.notificacoes, onMenu = nav.menu
            ) {
                Spacer(Modifier.height(18.dp))
                TextField(
                    value = query, onValueChange = ::onQuery,
                    placeholder = { Text("Buscar morador, unidade ou placa…") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Rounded.Close, contentDescription = "Limpar") }
                        else IconButton(onClick = {
                            // o QR da autorização traz o código de 6 dígitos: vai para a busca, que acha o visitante
                            lerQr(context, onCodigo = { lido -> onQuery(codigoDoQr(lido) ?: lido.take(40)) }, onErro = { messenger.show(it) })
                        }, modifier = Modifier.testTag("painel_qr")) { Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Ler QR do visitante") }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    shape = RoundedCornerShape(18.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color(0xFF111827), unfocusedTextColor = Color(0xFF111827),
                        focusedPlaceholderColor = Color(0xFF94A3B8), unfocusedPlaceholderColor = Color(0xFF94A3B8),
                        focusedLeadingIconColor = Color(0xFF94A3B8), unfocusedLeadingIconColor = Color(0xFF94A3B8),
                        focusedTrailingIconColor = Color(0xFF64748B), unfocusedTrailingIconColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp).testTag("painel_busca")
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

                Spacer(Modifier.height(16.dp))
                if (p == null && !painel.loading) {
                    ErrorBox(painel.error ?: "Não deu para carregar o painel.", onRetry = { vm.refresh() })
                }
                // os botões aparecem mesmo antes de carregar: o porteiro não espera a rede para registrar um pacote
                val botoes = listOf(
                    BotaoPortaria("Registrar encomenda", Icons.Rounded.Inventory2, "porteiro_registrar_encomenda", onClick = nav.novaEncomenda),
                    BotaoPortaria("Entregar", Icons.Rounded.LocalShipping, "porteiro_entregar", p?.encomendasAguardando?.toInt(), onClick = nav.abaEncomendas),
                    BotaoPortaria("Nova visita", Icons.Rounded.PersonAdd, "porteiro_nova_visita", onClick = nav.novaVisita),
                    BotaoPortaria("Esperados", Icons.Rounded.Schedule, "porteiro_esperados", p?.esperados?.size, onClick = nav.abaVisitantes),
                    BotaoPortaria("No condomínio", Icons.Rounded.Groups, "porteiro_dentro", p?.dentro?.size, contadorNeutro = true, onClick = nav.dentro),
                    BotaoPortaria("Livro", Icons.AutoMirrored.Rounded.MenuBook, "porteiro_livro", onClick = { nav.soon("port/livro") })
                )
                botoes.chunked(2).forEach { linha ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        linha.forEach { BotaoGrande(it, Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                if (p != null) {
                    if (p.pereciveis.isNotEmpty()) {
                        NoteBox(Icons.Rounded.WaterDrop, Color(0xFF0891B2), "${p.pereciveis.size} perecível(is) na geladeira: " + p.pereciveis.joinToString { it.unidade } + ".")
                        Spacer(Modifier.height(10.dp))
                    }
                    if (p.encomendasParadas7 > 0) {
                        NoteBox(Icons.Rounded.Warning, MaterialTheme.ext.danger, "${p.encomendasParadas7} encomenda(s) parada(s) há mais de 7 dias.")
                        Spacer(Modifier.height(10.dp))
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** Um botão grande do Início (contador opcional no canto). */
data class BotaoPortaria(val label: String, val icon: ImageVector, val tag: String, val contador: Int? = null, val contadorNeutro: Boolean = false, val onClick: () -> Unit)

@Composable
private fun BotaoGrande(b: BotaoPortaria, modifier: Modifier = Modifier) {
    Surface(
        onClick = b.onClick, shape = RoundedCornerShape(24.dp), color = MaterialTheme.ext.card,
        border = BorderStroke(1.dp, MaterialTheme.ext.cardBorder),
        modifier = modifier.heightIn(min = 150.dp).testTag(b.tag)
    ) {
        Box(Modifier.fillMaxWidth().padding(12.dp)) {
            Column(Modifier.align(Alignment.Center).padding(top = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(b.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    b.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.ext.ink,
                    textAlign = TextAlign.Center, maxLines = 2
                )
            }
            val n = b.contador
            if (n != null && n > 0) {
                Box(
                    Modifier.align(Alignment.TopEnd).clip(CircleShape)
                        .background(if (b.contadorNeutro) MaterialTheme.ext.muted else MaterialTheme.ext.danger)
                        .heightIn(min = 26.dp).padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) { Text(if (n > 99) "99+" else "$n", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

/** Cabeçalho escuro da portaria: quem está no turno, o condomínio, o sino e o menu. */
@Composable
fun PortariaHero(initials: String, kicker: String, nome: String, condominio: String, naoLidas: Long, onBell: () -> Unit, onMenu: () -> Unit, extra: @Composable () -> Unit = {}) {
    br.com.condovitta.app.core.designsystem.Hero {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text(initials, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(kicker, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.sp, color = Color.White.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Olá, $nome", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(condominio, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.65f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            BadgedBox(badge = { if (naoLidas > 0) Badge { Text(if (naoLidas > 9) "9+" else "$naoLidas") } }) {
                IconButton(onClick = onBell, modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f))) {
                    Icon(Icons.Rounded.Notifications, contentDescription = if (naoLidas > 0) "Notificações, $naoLidas não lidas" else "Notificações", tint = Color.White)
                }
            }
            Spacer(Modifier.size(8.dp))
            IconButton(onClick = onMenu, modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f)).testTag("portaria_menu")) {
                Icon(Icons.Rounded.Menu, contentDescription = "Menu da portaria", tint = Color.White)
            }
        }
        extra()
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
