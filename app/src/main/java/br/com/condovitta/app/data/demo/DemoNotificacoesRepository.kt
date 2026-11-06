package br.com.condovitta.app.data.demo

import br.com.condovitta.app.core.common.TimeProvider
import br.com.condovitta.app.domain.AuthRepository
import br.com.condovitta.app.domain.CategoriaNotificacao
import br.com.condovitta.app.domain.Notificacao
import br.com.condovitta.app.domain.Notificacoes
import br.com.condovitta.app.domain.NotificacoesRepository
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.SessionState
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sino de exemplo (os itens do prototype/data.js). Para dar para ver a notificação chegando, uma nova aparece a cada
 * consulta depois de um minuto de uso, até três.
 */
@Singleton
class DemoNotificacoesRepository @Inject constructor(private val time: TimeProvider, private val auth: AuthRepository) : NotificacoesRepository {

    private val lidas = mutableSetOf<Long>()
    private var inicio = 0L
    private var extras = 0

    private fun portaria() = (auth.session.value as? SessionState.LoggedIn)?.email?.contains("portaria", ignoreCase = true) == true

    private fun base(): List<Notificacao> {
        val agora = time.now()
        return if (portaria()) listOf(
            Notificacao(1, CategoriaNotificacao.VISITANTES, "Visitante pré-autorizado", "Lucas Prado para C-303, 14h–18h · RTA-4B21.", "/app/portaria/acesso", false, agora.minusMinutes(5)),
            Notificacao(2, CategoriaNotificacao.RESERVAS, "Reserva hoje à noite", "Salão · A-501 · 40 convidados liberados.", "/app/portaria/painel", false, agora.minusHours(1)),
            Notificacao(3, CategoriaNotificacao.MANUTENCOES, "Leitura de gás pendente", "Faltam 38 unidades do Bloco D.", "/app/portaria/gas", true, agora.minusHours(3))
        ) else listOf(
            Notificacao(11, CategoriaNotificacao.ENCOMENDAS, "Encomenda na portaria", "Mercado Livre · pacote médio. Código 4821.", "/app/morador/encomendas", false, agora.minusMinutes(12)),
            Notificacao(12, CategoriaNotificacao.ASSEMBLEIAS, "Assembleia ao vivo agora", "AGE de novembro começou. Entre para votar.", "/app/morador/assembleias", false, agora.minusMinutes(20)),
            Notificacao(13, CategoriaNotificacao.DOCUMENTOS, "Documento para assinar", "Termo do novo regimento das churrasqueiras.", "/app/morador/documentos", false, agora.minusHours(2)),
            Notificacao(14, CategoriaNotificacao.RESERVAS, "Reserva confirmada", "Churrasqueira 3 · sábado · Almoço.", "/app/morador/reservas", true, agora.minusDays(1)),
            Notificacao(15, CategoriaNotificacao.FINANCEIRO, "Boleto do mês disponível", "Vence dia 10 · R$ 612,40.", "/app/morador/financeiro", true, agora.minusDays(3))
        )
    }

    private val novas = mutableListOf<Notificacao>()

    override suspend fun listar(apenasNaoLidas: Boolean, limite: Int): Outcome<Notificacoes> {
        delay(200)
        if (inicio == 0L) inicio = time.epochMillis()
        if (extras < 3 && time.epochMillis() - inicio > 60_000L * (extras + 1)) {
            extras++
            novas.add(0, if (portaria()) Notificacao(100L + extras, CategoriaNotificacao.VISITANTES, "Morador respondeu", "Ana Souza (B-204) liberou Bruno Teixeira pelo app.", "/app/portaria/acesso", false, time.now())
            else Notificacao(100L + extras, CategoriaNotificacao.ENCOMENDAS, "Chegou uma encomenda", "Amazon · envelope. Código ${1000 + extras * 137}.", "/app/morador/encomendas", false, time.now()))
        }
        val todos = (novas + base()).map { if (it.id in lidas) it.copy(lida = true) else it }
        return Outcome.Ok(Notificacoes(todos.count { !it.lida }.toLong(), todos.filter { !apenasNaoLidas || !it.lida }.take(limite)))
    }

    override suspend fun marcarLida(id: Long): Outcome<Unit> { lidas += id; return Outcome.Ok(Unit) }
    override suspend fun todasLidas(): Outcome<Unit> { lidas += (novas + base()).map { it.id }; return Outcome.Ok(Unit) }
}
