package br.com.condovitta.app.feature.morador

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.condovitta.app.core.common.Fmt
import br.com.condovitta.app.core.designsystem.AppCard
import br.com.condovitta.app.core.designsystem.IconBubble
import br.com.condovitta.app.core.designsystem.SectionHeader
import br.com.condovitta.app.core.designsystem.ext
import br.com.condovitta.app.domain.Boleto
import br.com.condovitta.app.domain.Me
import br.com.condovitta.app.domain.Perfil
import br.com.condovitta.app.feature.home.GreetingHero
import br.com.condovitta.app.feature.home.QuickAction
import br.com.condovitta.app.feature.home.QuickActions
import br.com.condovitta.app.feature.home.saudacao
import br.com.condovitta.app.feature.portaria.CardList
import br.com.condovitta.app.feature.portaria.EmptyBox
import br.com.condovitta.app.feature.portaria.ErrorBox
import br.com.condovitta.app.feature.portaria.LoadingBox
import br.com.condovitta.app.ui.Messenger

/** Para onde as telas do morador levam. */
data class MoradorNav(
    val visitantes: () -> Unit,
    val novaAutorizacao: () -> Unit,
    val autorizacao: (Long) -> Unit,
    val encomendas: () -> Unit,
    val avisos: () -> Unit,
    val abaBoletos: () -> Unit,
    val abaReservas: () -> Unit,
    val soon: (String) -> Unit,
    val back: () -> Unit,
    val notificacoes: () -> Unit = {}
)

/* ------------------------------------------------------------------ Início */

@Composable
fun InicioScreen(me: Me, perfil: Perfil, padding: PaddingValues, nav: MoradorNav, messenger: Messenger, naoLidas: Long = 0, vm: InicioViewModel = hiltViewModel(), boletosVm: BoletosViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val qr by boletosVm.qr.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pix by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }

    PullToRefreshBox(
        isRefreshing = state.loading && state.data != null, onRefresh = vm::refresh,
        modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            GreetingHero(
                me.iniciais, saudacao(java.time.LocalTime.now().hour), me.primeiroNome,
                listOfNotNull(perfil.unidade, perfil.condominioNome).joinToString(" · "), onBell = nav.notificacoes, naoLidas = naoLidas
            )
            Column(Modifier.padding(horizontal = 16.dp)) {
                val d = state.data
                when {
                    d == null && state.loading -> LoadingBox()
                    d == null -> ErrorBox(state.error ?: "Não deu para carregar.", onRetry = vm::refresh)
                    else -> {
                        // a portaria esperando resposta vem primeiro: tem alguém no portão
                        d.visitas?.consultas?.forEach { c ->
                            Spacer(Modifier.height(14.dp))
                            ConsultaCard(c) { liberar, recado -> vm.responder(c, liberar, recado) }
                        }
                        val b = d.financeiro?.atual ?: d.financeiro?.vencidos?.firstOrNull()
                        Spacer(Modifier.height(14.dp))
                        if (b != null) BoletoCard(
                            b, onPix = { boletosVm.carregarQr(b.id); pix = true },
                            onCopiarLinha = { copiar(context, "Linha digitável", b.linhaDigitavel!!); messenger.show("Código de barras copiado") },
                            onClick = nav.abaBoletos
                        ) else if (d.financeiro != null) AppCard {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                IconBubble(Icons.Rounded.CheckCircle, MaterialTheme.ext.success)
                                Text("Tudo em dia: nenhum boleto em aberto.", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink)
                            }
                        }
                        if (pix && b?.pix != null) PixSheet(b, qr, onCopiar = {
                            copiar(context, "Pix", b.pix); pix = false; messenger.show("Pix copia e cola copiado")
                        }, onDismiss = { pix = false })

                        SectionHeader("Atalhos")
                        QuickActions(
                            listOf(
                                QuickAction("Autorizar visita", Icons.Rounded.PersonAdd, Color(0xFF1F4FE0), nav.novaAutorizacao),
                                QuickAction("Visitantes", Icons.Rounded.Badge, Color(0xFF7C3AED), nav.visitantes),
                                QuickAction("Encomendas", Icons.Rounded.Inventory2, Color(0xFFD97706), nav.encomendas),
                                QuickAction("Avisos", Icons.Rounded.Campaign, Color(0xFF0D9488), nav.avisos),
                                QuickAction("Boletos", Icons.Rounded.AccountBalanceWallet, Color(0xFF059669), nav.abaBoletos),
                                QuickAction("Chamados", Icons.Rounded.SupportAgent, Color(0xFF0891B2)) { nav.soon("app/chamados") },
                                QuickAction("Documentos", Icons.Rounded.Description, Color(0xFF64748B)) { nav.soon("app/documentos") }
                            )
                        )

                        val enc = d.encomendas?.aguardando.orEmpty()
                        if (enc.isNotEmpty()) {
                            TituloLink("Encomendas para retirar", "Ver", nav.encomendas)
                            CardList(enc.take(3)) { e ->
                                Row(Modifier.fillMaxWidth().clickable(onClick = nav.encomendas).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    IconBubble(Icons.Rounded.Inventory2, Color(0xFFD97706))
                                    Column(Modifier.weight(1f)) {
                                        Text("${e.transportadora} · ${e.tamanho?.label ?: ""}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink)
                                        Text("Chegou ${Fmt.quando(e.recebidaEm)} · ${e.local ?: "portaria"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted)
                                    }
                                    e.codigo?.let { CodigoBox(it, "CÓDIGO") }
                                }
                            }
                        }

                        val avisos = d.avisos.sortedWith(compareBy({ it.lido }, { !it.fixado })).take(2)
                        if (avisos.isNotEmpty()) {
                            TituloLink("Avisos do condomínio", "Mural", nav.avisos)
                            CardList(avisos) { a ->
                                Row(Modifier.fillMaxWidth().clickable(onClick = nav.avisos).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    IconBubble(Icons.Rounded.Campaign, if (a.fixado) MaterialTheme.ext.danger else Color(0xFF0D9488))
                                    Column(Modifier.weight(1f)) {
                                        Text(a.titulo, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (a.lido) FontWeight.Medium else FontWeight.Bold)
                                        Text("${a.categoria} · ${Fmt.quando(a.publicadoEm)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted)
                                    }
                                    if (!a.lido) Box(Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                }
                            }
                        }
                        Spacer(Modifier.height(96.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun TituloLink(title: String, link: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { SectionHeader(title) }
        TextButton(onClick = onClick, modifier = Modifier.padding(top = 10.dp)) {
            Text(link)
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
        }
    }
}

/* ------------------------------------------------------------------ Boletos */

@Composable
fun BoletosScreen(padding: PaddingValues, messenger: Messenger, vm: BoletosViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val baixando by vm.baixando.collectAsStateWithLifecycle()
    val qr by vm.qr.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pixDe by rememberSaveable { mutableStateOf<Long?>(null) }
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }
    fun pdf(b: Boleto) = vm.pdf(b.id) { bytes ->
        if (!abrirPdf(context, bytes, "boleto-${b.competencia}.pdf")) messenger.show("Instale um leitor de PDF para abrir o boleto.")
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Boletos e pagamentos", style = MaterialTheme.typography.titleLarge) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
    ) { p ->
        val f = state.data
        PullToRefreshBox(isRefreshing = state.loading && f != null, onRefresh = { vm.refresh() }, modifier = Modifier.padding(p).fillMaxSize()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                when {
                    f == null && state.loading -> LoadingBox()
                    f == null -> ErrorBox(state.error ?: "Não deu para carregar.", onRetry = { vm.refresh() })
                    else -> {
                        val abertos = (listOfNotNull(f.atual) + f.vencidos).distinctBy { it.id }
                        if (abertos.isEmpty()) EmptyBox(Icons.Rounded.CheckCircle, "Tudo em dia", "Nenhum boleto em aberto na sua unidade.")
                        abertos.forEach { b ->
                            Spacer(Modifier.height(8.dp))
                            BoletoCard(b, onPix = { vm.carregarQr(b.id); pixDe = b.id }, onCopiarLinha = {
                                copiar(context, "Linha digitável", b.linhaDigitavel!!); messenger.show("Código de barras copiado")
                            })
                            if (b.itens.isNotEmpty()) {
                                SectionHeader("Composição do boleto")
                                AppCard {
                                    Column(Modifier.padding(16.dp)) {
                                        b.itens.forEach { i -> LinhaValor(i.descricao, Fmt.money(i.valor)) }
                                        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.ext.cardBorder)
                                        LinhaValor("Total", Fmt.money(b.valor), destaque = true)
                                        if (b.corrigido != null) LinhaValor("Com multa e juros hoje", Fmt.money(b.corrigido), destaque = true)
                                    }
                                }
                            }
                            if (b.linhaDigitavel != null) {
                                Spacer(Modifier.height(10.dp))
                                Text(b.linhaDigitavel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted, modifier = Modifier.padding(horizontal = 4.dp))
                            }
                            if (b.temPdf) {
                                Spacer(Modifier.height(10.dp))
                                OutlinedButton(onClick = { pdf(b) }, enabled = baixando == null, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                                    if (baixando == b.id) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                    else { Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.size(8.dp)); Text("Ver boleto em PDF") }
                                }
                            }
                        }
                        if (f.vencidos.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            br.com.condovitta.app.feature.portaria.NoteBox(Icons.Rounded.Warning, MaterialTheme.ext.danger, "Há ${f.vencidos.size} boleto(s) vencido(s). Pague pelo Pix ou fale com a administração para um acordo.")
                        }
                        if (f.pagos.isNotEmpty()) {
                            SectionHeader("Pagos")
                            CardList(f.pagos) { b ->
                                Row(Modifier.fillMaxWidth().clickable(enabled = b.temPdf) { pdf(b) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    IconBubble(Icons.Rounded.CheckCircle, MaterialTheme.ext.success)
                                    Column(Modifier.weight(1f)) {
                                        Text(Fmt.competencia(b.competencia), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink)
                                        Text(listOfNotNull(b.pagoEm?.let { "Pago ${Fmt.data(it)}" }, b.forma).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(Fmt.money(b.valorPago ?: b.valor), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink)
                                        BoletoStatusBadge(b)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    val b = state.data?.boletos?.firstOrNull { it.id == pixDe }
    if (b?.pix != null) PixSheet(b, qr, onCopiar = { copiar(context, "Pix", b.pix); pixDe = null; messenger.show("Pix copia e cola copiado") }, onDismiss = { pixDe = null })
}

@Composable
private fun LinhaValor(k: String, v: String, destaque: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(k, style = MaterialTheme.typography.bodyMedium, color = if (destaque) MaterialTheme.ext.ink else MaterialTheme.ext.muted, fontWeight = if (destaque) FontWeight.Bold else null, modifier = Modifier.weight(1f))
        Text(v, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.ink, fontWeight = if (destaque) FontWeight.Bold else FontWeight.Medium)
    }
}
