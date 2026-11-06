package br.com.condovitta.app.domain

import java.time.LocalDateTime

/* Notificações do sino (NotificacaoDTO do gestao-condominio). Chegam por consulta periódica; o FCM entra depois. */

enum class CategoriaNotificacao(val label: String) {
    ENCOMENDAS("Encomendas"), VISITANTES("Visitantes"), RESERVAS("Reservas"), ASSEMBLEIAS("Assembleias"),
    COMUNICADOS("Avisos"), FINANCEIRO("Financeiro"), CHAMADOS("Chamados"), OCORRENCIAS("Ocorrências"),
    DOCUMENTOS("Documentos"), MANUTENCOES("Manutenções"), OUTRAS("Outras");

    companion object {
        fun of(v: String?) = entries.firstOrNull { it.name == v } ?: OUTRAS
    }
}

data class Notificacao(
    val id: Long,
    val categoria: CategoriaNotificacao,
    val titulo: String,
    val texto: String?,
    /** rota da web ("/app/morador/encomendas"): o app abre a tela equivalente */
    val rota: String?,
    val lida: Boolean,
    val criadoEm: LocalDateTime?
)

data class Notificacoes(val naoLidas: Long, val itens: List<Notificacao>)

interface NotificacoesRepository {
    suspend fun listar(apenasNaoLidas: Boolean = false, limite: Int = 50): Outcome<Notificacoes>
    suspend fun marcarLida(id: Long): Outcome<Unit>
    suspend fun todasLidas(): Outcome<Unit>
}

/** Para onde a notificação leva (as rotas da web viram telas do app). */
enum class Destino { ENCOMENDAS, VISITANTES, BOLETOS, RESERVAS, AVISOS, PORTARIA_ACESSO, PORTARIA_ENCOMENDAS, PORTARIA_GAS, NOTIFICACOES }

object Rotas {
    fun destino(rota: String?, categoria: CategoriaNotificacao = CategoriaNotificacao.OUTRAS): Destino {
        val r = rota.orEmpty()
        return when {
            r.startsWith("/app/portaria/acesso") || r.startsWith("/app/portaria/painel") -> Destino.PORTARIA_ACESSO
            r.startsWith("/app/portaria/encomendas") -> Destino.PORTARIA_ENCOMENDAS
            r.startsWith("/app/portaria/gas") -> Destino.PORTARIA_GAS
            r.contains("/encomendas") -> Destino.ENCOMENDAS
            r.contains("/visitantes") -> Destino.VISITANTES
            r.contains("/financeiro") || r.contains("/boletos") -> Destino.BOLETOS
            r.contains("/reservas") -> Destino.RESERVAS
            r.contains("/comunicados") || r.contains("/avisos") || r.contains("/mural") -> Destino.AVISOS
            else -> when (categoria) {
                CategoriaNotificacao.ENCOMENDAS -> Destino.ENCOMENDAS
                CategoriaNotificacao.VISITANTES -> Destino.VISITANTES
                CategoriaNotificacao.FINANCEIRO -> Destino.BOLETOS
                CategoriaNotificacao.RESERVAS -> Destino.RESERVAS
                CategoriaNotificacao.COMUNICADOS -> Destino.AVISOS
                else -> Destino.NOTIFICACOES
            }
        }
    }
}
