package br.com.condovitta.app.domain

import kotlinx.coroutines.flow.StateFlow

/** Entrar, sair e recuperar a senha. */
interface AuthRepository {
    val session: StateFlow<SessionState>

    /** Lê a sessão salva no aparelho (abertura do app). */
    suspend fun restore()

    suspend fun login(email: String, password: String, remember: Boolean, totp: String? = null): Outcome<Unit>

    suspend fun forgotPassword(email: String): Outcome<Unit>

    /** Sai da conta. [reason] aparece no login (sessão expirada, turno passado). */
    suspend fun logout(reason: SessionEnd? = null)

    /** Por que a última sessão terminou (null = saiu por conta própria ou nunca entrou). */
    val lastEnd: StateFlow<SessionEnd?>
}

/** Operação da portaria: painel, visitantes e acesso, bloqueados e encomendas. */
interface PortariaRepository {
    suspend fun painel(): Outcome<Painel>
    suspend fun busca(q: String): Outcome<Busca>
    suspend fun unidades(): Outcome<List<UnidadeResumo>>

    suspend fun acesso(): Outcome<AcessoListas>
    suspend fun bloqueados(): Outcome<List<Bloqueado>>
    suspend fun novaVisita(form: NovaVisita): Outcome<Visita>
    suspend fun excluirVisita(visitanteId: Long): Outcome<Unit>
    suspend fun entrada(visitanteId: Long, via: ViaLiberacao? = null): Outcome<Visita>
    suspend fun consultar(visitanteId: Long): Outcome<Visita>
    suspend fun recusar(acessoId: Long): Outcome<Visita>
    suspend fun saida(acessoId: Long): Outcome<Visita>
    /** desfaz a última ação (entrada, saída ou recusa) */
    suspend fun desfazer(acessoId: Long): Outcome<Unit>

    suspend fun encomendas(): Outcome<EncomendasPortaria>
    suspend fun destinatarios(unidadeId: Long): Outcome<Destinatarios>
    suspend fun registrarEncomenda(form: NovaEncomenda, foto: ByteArray?): Outcome<Encomenda>
    suspend fun excluirEncomenda(id: Long): Outcome<Unit>
    suspend fun avisar(id: Long): Outcome<Encomenda>
    suspend fun entregar(id: Long, entrega: Entrega): Outcome<Encomenda>
    suspend fun desfazerEntrega(id: Long): Outcome<Encomenda>
    /** foto tirada na chegada (null quando não há) */
    suspend fun fotoEncomenda(id: Long): Outcome<ByteArray?>
}

/** Quem está logado e em quais condomínios. */
interface ContextRepository {
    suspend fun me(): Outcome<Me>
}
