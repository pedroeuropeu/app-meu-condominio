package br.com.condovitta.app.feature.servicos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.condovitta.app.core.common.Fmt
import br.com.condovitta.app.core.designsystem.AppCard
import br.com.condovitta.app.core.designsystem.IconBubble
import br.com.condovitta.app.core.designsystem.SectionHeader
import br.com.condovitta.app.core.designsystem.StatusBadge
import br.com.condovitta.app.core.designsystem.ext
import br.com.condovitta.app.domain.EventoCalendario
import br.com.condovitta.app.domain.TipoEvento
import br.com.condovitta.app.feature.morador.abrirArquivo
import br.com.condovitta.app.feature.morador.abrirPdf
import br.com.condovitta.app.feature.portaria.BusyOr
import br.com.condovitta.app.feature.portaria.CardList
import br.com.condovitta.app.feature.portaria.EmptyBox
import br.com.condovitta.app.feature.portaria.InfoRows
import br.com.condovitta.app.feature.portaria.Kpi
import br.com.condovitta.app.feature.portaria.NoteBox
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val PT = Locale.forLanguageTag("pt-BR")
private val Green = Color(0xFF059669)
private val Red = Color(0xFFDC2626)

/** "Out/26" */
private fun mesCurto(ym: String): String = runCatching {
    val m = YearMonth.parse(ym)
    m.month.getDisplayName(TextStyle.SHORT, PT).replace(".", "").replaceFirstChar { it.uppercase() } + "/" + (m.year % 100).toString().padStart(2, '0')
}.getOrDefault(ym)

private fun corHex(hex: String?, padrao: Color): Color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(padrao)

/** Barras verticais simples (sem biblioteca de gráficos). */
@Composable
private fun Barras(valores: List<Pair<String, List<Pair<BigDecimal, Color>>>>, altura: Int = 120) {
    val max = valores.flatMap { it.second.map { v -> v.first } }.maxOrNull()?.takeIf { it.signum() > 0 } ?: BigDecimal.ONE
    Row(Modifier.fillMaxWidth().height(altura.dp + 22.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        valores.forEach { (rotulo, barras) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.height(altura.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    barras.forEach { (v, cor) ->
                        val f = v.divide(max, 4, RoundingMode.HALF_UP).toFloat().coerceIn(0.02f, 1f)
                        Box(Modifier.width(if (barras.size > 1) 8.dp else 14.dp).fillMaxHeight(f).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(cor))
                    }
                }
                Text(rotulo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ext.muted, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun Legenda(itens: List<Pair<String, Color>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 6.dp)) {
        itens.forEach { (t, c) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(c))
                Text(" $t", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ext.muted)
            }
        }
    }
}

/* ------------------------------------------------------------------ prestação de contas */

@Composable
fun PrestacaoScreen(onBack: () -> Unit, vm: PrestacaoViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val historico by vm.historico.collectAsStateWithLifecycle()
    val baixando by vm.baixando.collectAsStateWithLifecycle()
    val context = LocalContext.current
    TelaServico("Prestação de contas", state, onBack, { vm.refresh() }) { t ->
        val ym = t.ym
        if (ym == null) {
            EmptyBox(Icons.Rounded.AccountBalance, "Nada publicado ainda", "Quando a administração publicar o balancete do mês, receitas e despesas aparecem aqui.")
            return@TelaServico
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            t.publicados.forEach { m -> FilterChip(m == ym, { vm.escolher(m) }, label = { Text(mesCurto(m)) }, colors = chipColors()) }
        }
        Text(Fmt.competencia(ym), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.ext.ink, modifier = Modifier.padding(top = 10.dp))
        t.publicadoEm?.let { Text("Publicado em ${Fmt.data(it.toLocalDate())}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted) }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Kpi(Icons.Rounded.TrendingUp, Green, Fmt.money(t.receitas), "Receitas" + (pct(t.variacaoReceitas)?.let { " $it" } ?: ""), Modifier.weight(1f))
            Kpi(Icons.Rounded.TrendingDown, Red, Fmt.money(t.despesas), "Despesas" + (pct(t.variacaoDespesas)?.let { " $it" } ?: ""), Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        AppCard {
            Column(Modifier.padding(16.dp)) {
                InfoRows(
                    listOf(
                        "Saldo do mês" to Fmt.money(t.saldoMes), "Conta corrente" to t.contaCorrente?.let { Fmt.money(it) },
                        "Fundo de reserva" to t.fundo?.let { Fmt.money(it) }
                    )
                )
                val fundo = t.fundo
                val meta = t.metaFundo
                if (fundo != null && meta != null && meta.signum() > 0) {
                    val f = fundo.divide(meta, 4, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
                    LinearProgressIndicator(progress = { f }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(6.dp).clip(RoundedCornerShape(3.dp)), color = Green)
                    Text("${(f * 100).toInt()}% da meta do fundo (${Fmt.money(meta)})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ext.muted, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        if (t.porCategoria.isNotEmpty()) {
            SectionHeader("Para onde foi o dinheiro")
            AppCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val total = t.porCategoria.fold(BigDecimal.ZERO) { a, c -> a + c.valor }.takeIf { it.signum() > 0 } ?: BigDecimal.ONE
                    t.porCategoria.sortedByDescending { it.valor }.forEach { c ->
                        val f = c.valor.divide(total, 4, RoundingMode.HALF_UP).toFloat()
                        Column {
                            Row {
                                Text(c.nome, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.ink, modifier = Modifier.weight(1f))
                                Text("${Fmt.money(c.valor)} · ${(f * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted)
                            }
                            LinearProgressIndicator(progress = { f }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = corHex(c.cor, MaterialTheme.colorScheme.primary), trackColor = MaterialTheme.ext.cardBorder)
                        }
                    }
                }
            }
        }
        if (t.serie.size > 1) {
            SectionHeader("Últimos meses")
            AppCard {
                Column(Modifier.padding(16.dp)) {
                    Barras(t.serie.takeLast(6).map { mesCurto(it.ym).substringBefore('/') to listOf(it.receitas to Green, it.despesas to Red) })
                    Legenda(listOf("Receitas" to Green, "Despesas" to Red))
                }
            }
        }
        if (t.lancamentos.isNotEmpty()) {
            SectionHeader("Despesas do mês", subtitle = "Toque para ver a nota fiscal")
            CardList(t.lancamentos) { l ->
                LinhaServico(
                    Icons.Rounded.ReceiptLong, if (l.temNota) MaterialTheme.colorScheme.primary else MaterialTheme.ext.muted, l.descricao,
                    listOfNotNull(l.fornecedor, l.categoria).joinToString(" · ").ifBlank { null }, l.data?.let { Fmt.data(it) },
                    onClick = if (l.temNota) ({ vm.nota(l.id) { abrirBaixado(context, it, "nota-${l.id}", "Abrir nota fiscal") } }) else null,
                    trailing = { Text(Fmt.money(l.valor), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { vm.pdf(ym) { abrirPdf(context, it, "balancete-$ym.pdf") } }, enabled = baixando == null, modifier = Modifier.fillMaxWidth().testTag("prestacao_pdf")) {
            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp)); Text("  Baixar o balancete em PDF")
        }
        if (historico.isNotEmpty()) {
            SectionHeader("Balancetes anteriores")
            CardList(historico) { b ->
                LinhaServico(
                    Icons.Rounded.Description, MaterialTheme.ext.muted, Fmt.competencia(b.competencia),
                    listOfNotNull(b.receitas?.let { "Receitas ${Fmt.money(it)}" }, b.despesas?.let { "Despesas ${Fmt.money(it)}" }).joinToString(" · ").ifBlank { null },
                    b.observacao,
                    onClick = if (b.temArquivo) ({ vm.arquivo(b.id) { bytes -> val n = b.arquivoNome ?: "balancete-${b.competencia}.pdf"; abrirArquivo(context, bytes, n, mimeDe(n)) } }) else null
                )
            }
        }
        FimDaLista(fab = false)
    }
}

/* ------------------------------------------------------------------ consumo de gás */

@Composable
fun GasMoradorScreen(onBack: () -> Unit, vm: GasMoradorViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val baixando by vm.baixando.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var contestando by rememberSaveable { mutableStateOf(false) }
    TelaServico("Consumo de gás", state, onBack, { vm.refresh() }) { g ->
        val u = g.ultima
        if (u == null) {
            EmptyBox(Icons.Rounded.LocalFireDepartment, "Sem leituras ainda", "Quando a portaria ler o medidor da sua unidade, o consumo aparece aqui.")
            return@TelaServico
        }
        Spacer(Modifier.height(8.dp))
        AppCard {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBubble(Icons.Rounded.LocalFireDepartment, Red)
                    Column(Modifier.weight(1f)) {
                        Text("Leitura de ${Fmt.competencia(u.mes)}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.ext.muted)
                        Text(m3(u.consumo), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.ext.ink)
                    }
                    Text(Fmt.money(u.valor), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                    if (u.estimada) StatusBadge("Estimada", MaterialTheme.ext.warn, MaterialTheme.ext.warnContainer)
                    if (u.contestada) StatusBadge("Contestada", MaterialTheme.ext.info, MaterialTheme.ext.infoContainer)
                    if (u.fechado) StatusBadge("No boleto", MaterialTheme.ext.success, MaterialTheme.ext.successContainer)
                }
                InfoRows(
                    listOf(
                        "Medidor agora" to u.atual?.let { m3(it) }, "Leitura anterior" to u.anterior?.let { m3(it) },
                        "Lida em" to u.lidaEm?.let { Fmt.quando(it) }, "Por" to u.lidoPor, "Preço do m³" to g.preco?.let { Fmt.money(it) }
                    )
                )
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (u.temFoto && u.leituraId != null) OutlinedButton(onClick = { vm.foto(u.leituraId) { abrirBaixado(context, it, "medidor-${u.leituraId}", "Ver foto do medidor") } },
                        enabled = baixando == null, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp)); Text("  Foto")
                    }
                    if (!u.contestada && !u.fechado) OutlinedButton(onClick = { contestando = true }, modifier = Modifier.weight(1f).testTag("gas_contestar")) { Text("Contestar") }
                }
            }
        }
        if (g.acimaDoPadrao) {
            Spacer(Modifier.height(10.dp))
            NoteBox(Icons.Rounded.Warning, MaterialTheme.ext.warn, "Seu consumo ficou acima do normal. Vale conferir se há vazamento ou registro aberto.")
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Kpi(Icons.Rounded.Groups, MaterialTheme.colorScheme.primary, m3(g.mediaBloco), "Média do ${g.bloco ?: "bloco"}", Modifier.weight(1f))
            Kpi(Icons.Rounded.CalendarMonth, Green, m3(g.media12), "Sua média 12 m", Modifier.weight(1f))
        }
        if (g.meses.isNotEmpty()) {
            SectionHeader("Consumo mês a mês")
            AppCard {
                Column(Modifier.padding(16.dp)) {
                    Barras(g.meses.takeLast(12).map { mesCurto(it.mes).substringBefore('/').take(3) to listOfNotNull(it.consumo?.let { c -> c to Red }, it.mediaBloco?.let { m -> m to Color(0xFF94A3B8) }) })
                    Legenda(listOf("Sua unidade" to Red, "Média do bloco" to Color(0xFF94A3B8)))
                    g.picoMes?.let { Text("Maior consumo: ${Fmt.competencia(it)} (${m3(g.picoConsumo)})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ext.muted, modifier = Modifier.padding(top = 6.dp)) }
                }
            }
        }
        if (g.leituras.isNotEmpty()) {
            SectionHeader("Histórico")
            CardList(g.leituras) { h ->
                LinhaServico(
                    Icons.Rounded.LocalFireDepartment, if (h.acimaDaMedia) MaterialTheme.ext.warn else MaterialTheme.ext.muted, Fmt.competencia(h.mes),
                    listOfNotNull(m3(h.consumo), h.estimada.takeIf { it }?.let { "estimada" }, h.acimaDaMedia.takeIf { it }?.let { "acima da média" }).joinToString(" · "),
                    "${h.anterior?.let { m3(it) } ?: "—"} → ${h.atual?.let { m3(it) } ?: "—"}", onClick = null,
                    trailing = { Text(Fmt.money(h.valor), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink) }
                )
            }
        }
        FimDaLista(fab = false)
    }
    if (contestando) ContestarSheet(vm, onDismiss = { contestando = false })
}

@Composable
private fun ContestarSheet(vm: GasMoradorViewModel, onDismiss: () -> Unit) {
    val salvando by vm.envio.salvando.collectAsStateWithLifecycle()
    val erro by vm.envio.erro.collectAsStateWithLifecycle()
    val ok by vm.envio.ok.collectAsStateWithLifecycle()
    val inicial = remember { ok }
    LaunchedEffect(ok) { if (ok != inicial) onDismiss() }
    var texto by rememberSaveable { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding().imePadding()) {
            Text("Contestar a leitura", style = MaterialTheme.typography.titleLarge)
            Text("A administração confere a foto do medidor e responde. Se precisar, a leitura é refeita antes de ir para o boleto.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(texto, { texto = it.take(500) }, label = { Text("O que parece errado? (opcional)") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            erro?.let { Spacer(Modifier.height(8.dp)); NoteBox(Icons.Rounded.ErrorOutline, MaterialTheme.colorScheme.error, it) }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { vm.contestar(texto) }, enabled = !salvando, modifier = Modifier.fillMaxWidth().height(50.dp)) { BusyOr(salvando) { Text("Enviar contestação") } }
        }
    }
}

/* ------------------------------------------------------------------ calendário */

private fun TipoEvento.visual(): Pair<ImageVector, Color> = when (this) {
    TipoEvento.ASSEMBLEIA -> Icons.Rounded.Gavel to Color(0xFFDB2777)
    TipoEvento.MANUTENCAO -> Icons.Rounded.Build to Color(0xFFD97706)
    TipoEvento.SOCIAL -> Icons.Rounded.Celebration to Color(0xFF7C3AED)
    TipoEvento.REUNIAO -> Icons.Rounded.Groups to Color(0xFF2563EB)
    TipoEvento.OBRA -> Icons.Rounded.Construction to Color(0xFFDC2626)
    TipoEvento.SISTEMA -> Icons.Rounded.Info to Color(0xFF0D9488)
}

@Composable
fun CalendarioScreen(onBack: () -> Unit, vm: CalendarioViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val mes by vm.mes.collectAsStateWithLifecycle()
    val dia by vm.dia.collectAsStateWithLifecycle()
    val baixando by vm.baixando.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var lembrando by remember { mutableStateOf<EventoCalendario?>(null) }
    TelaServico("Calendário", state, onBack, { vm.refresh() }) { a ->
        Spacer(Modifier.height(8.dp))
        AppCard {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.mudarMes(mes.minusMonths(1)) }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Mês anterior") }
                    Text(Fmt.competencia(mes.toString()), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    IconButton(onClick = { vm.mudarMes(mes.plusMonths(1)) }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Próximo mês") }
                }
                MesGrade(mes, a, dia, vm.hoje()) { vm.escolherDia(it) }
            }
        }
        val lista = if (a.mes != mes) emptyList() else dia?.let { a.doDia(it) } ?: a.eventos
        val titulo = dia?.let { d -> if (d == vm.hoje()) "Hoje" else d.format(DateTimeFormatter.ofPattern("EEEE, dd/MM", PT)).replaceFirstChar { it.uppercase() } } ?: "Eventos do mês"
        SectionHeader(titulo, subtitle = if (dia != null && a.eventos.isNotEmpty()) "Toque no dia de novo para ver o mês inteiro" else null)
        if (lista.isEmpty()) EmptyBox(Icons.Rounded.EventAvailable, "Nada marcado", if (dia != null) "Nenhum evento neste dia." else "Nenhum evento neste mês.")
        lista.forEach { e -> EventoCard(e, a.lembrete(e)?.minutosAntes, dia == null, baixando == e.chave, onLembrar = { lembrando = e }) {
            vm.agenda(e) { abrirArquivo(context, it, "evento-${e.eventoId}.ics", "text/calendar", "Adicionar à agenda") }
        } }
        FimDaLista(fab = false)
    }
    lembrando?.let { e -> LembreteSheet(e, state.data?.lembrete(e)?.minutosAntes, onDismiss = { lembrando = null }) { vm.lembrar(e, it); lembrando = null } }
}

@Composable
private fun MesGrade(mes: YearMonth, a: Agenda, selecionado: java.time.LocalDate?, hoje: java.time.LocalDate, onDia: (java.time.LocalDate) -> Unit) {
    val comEvento = if (a.mes == mes) a.eventos.groupBy { it.data } else emptyMap()
    val semana = listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
    Row(Modifier.fillMaxWidth()) {
        semana.forEach { d -> Text(d.getDisplayName(TextStyle.NARROW, PT), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ext.muted, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
    }
    val inicio = mes.atDay(1).dayOfWeek.value % 7
    val celulas = List(inicio) { null } + (1..mes.lengthOfMonth()).map { mes.atDay(it) }
    celulas.chunked(7).forEach { linha ->
        Row(Modifier.fillMaxWidth()) {
            (0 until 7).forEach { i ->
                val d = linha.getOrNull(i)
                Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                    if (d != null) {
                        val sel = d == selecionado
                        val evs = comEvento[d].orEmpty()
                        Column(
                            Modifier.fillMaxWidth().fillMaxHeight().clip(CircleShape)
                                .background(if (sel) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .then(if (d == hoje && !sel) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                                .clickable { onDia(d) },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                        ) {
                            Text(d.dayOfMonth.toString(), style = MaterialTheme.typography.bodyMedium,
                                color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.ext.ink, fontWeight = if (evs.isNotEmpty()) FontWeight.Bold else FontWeight.Normal)
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                evs.take(3).forEach { e -> Box(Modifier.size(4.dp).clip(CircleShape).background(if (sel) MaterialTheme.colorScheme.onPrimary else e.tipo.visual().second)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventoCard(e: EventoCalendario, lembrete: Int?, mostrarData: Boolean, baixando: Boolean, onLembrar: () -> Unit, onAgenda: () -> Unit) {
    val (ic, cor) = e.tipo.visual()
    AppCard(Modifier.padding(bottom = 10.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBubble(ic, cor)
                Column(Modifier.weight(1f)) {
                    Text(e.titulo, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink)
                    Text(
                        listOfNotNull(
                            if (mostrarData) Fmt.data(e.data).substring(0, 5) else null,
                            e.inicio?.let { i -> Fmt.hora(i) + (e.fim?.let { " – ${Fmt.hora(it)}" } ?: "") } ?: "Dia todo",
                            e.local
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary
                    )
                    e.descricao?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted, modifier = Modifier.padding(top = 4.dp)) }
                    val extra = listOfNotNull(e.tipo.label, e.fornecedor, e.blocos.takeIf { it.isNotEmpty() }?.let { "Bloco(s) ${it.joinToString()}" },
                        e.recorrencia?.let { r -> mapOf("SEMANAL" to "toda semana", "MENSAL" to "todo mês", "TRIMESTRAL" to "a cada 3 meses", "SEMESTRAL" to "a cada 6 meses", "ANUAL" to "todo ano")[r] })
                    Text(extra.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ext.faint, modifier = Modifier.padding(top = 4.dp))
                }
            }
            if (e.eventoId != null) {
                FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = onLembrar,
                        label = { Text(lembrete?.let { "Lembrete ${antecedencia(it)}" } ?: "Lembrar-me") },
                        leadingIcon = { Icon(if (lembrete != null) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsNone, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    AssistChip(onClick = onAgenda, enabled = !baixando, label = { Text("Adicionar à agenda") },
                        leadingIcon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)) })
                }
            }
        }
    }
}

@Composable
private fun LembreteSheet(e: EventoCalendario, atual: Int?, onDismiss: () -> Unit, onEscolher: (Int) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
            Text("Lembrar de “${e.titulo}”", style = MaterialTheme.typography.titleLarge)
            Text("Você recebe uma notificação no celular.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted)
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(30, 60, 180, 1440, 2880).forEach { m -> FilterChip(atual == m, { onEscolher(m) }, label = { Text(antecedencia(m)) }, colors = chipColors()) }
            }
            if (atual != null) TextButton(onClick = { onEscolher(0) }, modifier = Modifier.padding(top = 8.dp)) { Text("Tirar o lembrete", color = MaterialTheme.colorScheme.error) }
        }
    }
}
