package br.com.condovitta.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

/* Rotas tipadas (Navigation Compose). Cada modo tem a sua barra inferior e uma pilha por aba. */

@Serializable data object PortPainel
@Serializable data object PortVisitantes
@Serializable data object PortEncomendas
@Serializable data object PortMais

@Serializable data object AppInicio
@Serializable data object AppReservas
@Serializable data object AppBoletos
@Serializable data object AppMais

/** Tela ainda não feita: mostra o estado "Em construção" com o que ela vai fazer ([Soon.key] em [SoonCatalog]). */
@Serializable data class Soon(val key: String)

/** O que cada tela futura vai fazer (textos do protótipo) e em que fase entra. */
data class SoonInfo(val title: String, val icon: ImageVector, val color: Color, val description: String)

object SoonCatalog {
    private val Blue = Color(0xFF1F4FE0)
    private val Amber = Color(0xFFD97706)
    private val Cyan = Color(0xFF0891B2)
    private val Green = Color(0xFF059669)
    private val Violet = Color(0xFF7C3AED)
    private val Teal = Color(0xFF0D9488)
    private val Slate = Color(0xFF64748B)
    private val Red = Color(0xFFDC2626)

    val all: Map<String, SoonInfo> = mapOf(
        // Portaria (fase 1 e 4)
        "port/painel" to SoonInfo("Painel do turno", Icons.Rounded.Home, Amber, "Busca por morador, unidade ou placa, os números do turno e as ações de um toque."),
        "port/visitantes" to SoonInfo("Visitantes e acesso", Icons.Rounded.Badge, Blue, "Esperados, quem está no condomínio e o histórico do dia. Liberar entrada e registrar saída em um toque."),
        "port/nova-visita" to SoonInfo("Nova visita", Icons.Rounded.PersonAdd, Blue, "Cadastro rápido de quem chegou sem pré-autorização: libera e avisa o morador."),
        "port/encomendas" to SoonInfo("Encomendas", Icons.Rounded.Inventory2, Amber, "Aguardando retirada e retiradas de hoje. Entrega com código e assinatura."),
        "port/registrar-encomenda" to SoonInfo("Registrar encomenda", Icons.Rounded.Inventory2, Amber, "Unidade → transportadora → foto → onde guardar. Gera o código de retirada e avisa o morador."),
        "port/bloqueados" to SoonInfo("Bloqueados", Icons.Rounded.Block, Red, "Pessoas com entrada bloqueada pelo condomínio."),
        "port/reservas" to SoonInfo("Reservas de hoje", Icons.Rounded.Event, Violet, "Lista de convidados na portaria, check-in e controle de chave."),
        "port/livro" to SoonInfo("Livro da portaria", Icons.AutoMirrored.Rounded.MenuBook, Slate, "Ocorrências do turno, rondas e passagem de turno."),
        "port/gas" to SoonInfo("Leitura de gás", Icons.Rounded.LocalFireDepartment, Red, "Leitura do medidor por unidade, com foto."),
        "port/veiculos" to SoonInfo("Veículos e placas", Icons.Rounded.DirectionsCar, Cyan, "Consulta de placa e veículos cadastrados por unidade."),
        "port/notifs" to SoonInfo("Notificações", Icons.Rounded.Notifications, Amber, "A central do sino: respostas dos moradores e avisos da administração."),
        // Morador (fases 2 a 4)
        "app/inicio" to SoonInfo("Início", Icons.Rounded.Home, Blue, "Boleto do mês em destaque, encomendas para retirar, próximas reservas e avisos."),
        "app/reservas" to SoonInfo("Reservas", Icons.Rounded.CalendarMonth, Violet, "Suas reservas e o assistente em 4 passos: área, data, turno e convidados, termo assinado."),
        "app/boletos" to SoonInfo("Boletos e pagamentos", Icons.Rounded.AccountBalanceWallet, Green, "Boleto do mês, Pix copia-e-cola, composição da taxa e histórico de pagos."),
        "app/visitantes" to SoonInfo("Visitantes", Icons.Rounded.Badge, Blue, "Pré-autorizar visitas e prestadores, QR para compartilhar e visitas de hoje."),
        "app/encomendas" to SoonInfo("Encomendas", Icons.Rounded.Inventory2, Amber, "Aguardando retirada com o código, autorizar terceiro e histórico."),
        "app/avisos" to SoonInfo("Avisos", Icons.Rounded.Campaign, Teal, "Mural do condomínio com confirmação de leitura."),
        "app/documentos" to SoonInfo("Documentos", Icons.Rounded.Description, Slate, "Convenção, regimento, atas e documentos para assinar."),
        "app/chamados" to SoonInfo("Chamados", Icons.Rounded.SupportAgent, Cyan, "Abrir e acompanhar pedidos para a administração."),
        "app/gas" to SoonInfo("Consumo de gás", Icons.Rounded.LocalFireDepartment, Red, "Leituras e consumo da sua unidade."),
        "app/assembleias" to SoonInfo("Assembleias", Icons.Rounded.Gavel, Violet, "Pautas, votação e atas das assembleias."),
        "app/prestacao" to SoonInfo("Prestação de contas", Icons.Rounded.AccountBalance, Green, "Receitas e despesas do condomínio mês a mês."),
        "app/ocorrencias" to SoonInfo("Ocorrências", Icons.Rounded.ReportProblem, Amber, "Registrar e acompanhar ocorrências do condomínio."),
        "app/unidade" to SoonInfo("Minha unidade", Icons.Rounded.Key, Blue, "Moradores, veículos e pets da sua unidade."),
        "app/calendario" to SoonInfo("Calendário", Icons.Rounded.CalendarMonth, Teal, "Manutenções programadas, assembleias e eventos do condomínio."),
        "app/notifs" to SoonInfo("Notificações", Icons.Rounded.Notifications, Blue, "A central do sino: encomendas, visitas, boletos e avisos.")
    )

    operator fun get(key: String): SoonInfo = all[key] ?: SoonInfo("Em breve", Icons.Rounded.Home, Slate, "Esta parte do app ainda está sendo feita.")
}

/* Portaria (fase 1) */
@Serializable data class VisitanteDetalhe(val visitanteId: Long)
@Serializable data object NovaVisitaRoute
@Serializable data object BloqueadosRoute
@Serializable data class EncomendaDetalhe(val id: Long)
@Serializable data object NovaEncomendaRoute

/* Morador (fase 2) */
@Serializable data object AppVisitantes
@Serializable data object NovaAutorizacaoRoute
@Serializable data class AutorizacaoDetalhe(val id: Long)
@Serializable data object AppEncomendas
@Serializable data object AppAvisos

/* Reservas (fase 3) */
@Serializable data class NovaReservaRoute(val areaId: Long)
@Serializable data object PortReservasRoute
