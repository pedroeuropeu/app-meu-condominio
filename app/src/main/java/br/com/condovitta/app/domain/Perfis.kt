package br.com.condovitta.app.domain

/**
 * Regra de quem entra em qual modo, a partir dos vínculos do /api/v1/me:
 * - Morador: papel MORADOR, ou SÍNDICO/CONSELHO que também têm unidade no condomínio;
 * - Portaria: quem opera a portaria no backend (PapelEnum.OPERACAO = PORTARIA, SÍNDICO, ADMINISTRAÇÃO).
 * Um perfil por (modo, condomínio). Portaria vem primeiro: quem tem os dois quase sempre está na guarita.
 */
object Perfis {

    private val MORADOR_COM_UNIDADE = setOf(Papel.SINDICO, Papel.CONSELHO)
    private val OPERACAO = setOf(Papel.PORTARIA, Papel.SINDICO, Papel.ADMINISTRACAO)

    fun disponiveis(me: Me): List<Perfil> {
        val porCondominio = me.memberships.groupBy { it.condominioId }
        val portaria = mutableListOf<Perfil>()
        val morador = mutableListOf<Perfil>()
        porCondominio.forEach { (condominioId, vinculos) ->
            val nome = vinculos.first().condominioNome
            if (vinculos.any { it.papel in OPERACAO }) portaria += Perfil(Modo.PORTARIA, condominioId, nome)
            val casa = vinculos.firstOrNull { it.papel == Papel.MORADOR && it.unidadeId != null }
                ?: vinculos.firstOrNull { it.papel in MORADOR_COM_UNIDADE && it.unidadeId != null }
                ?: vinculos.firstOrNull { it.papel == Papel.MORADOR }
            if (casa != null) morador += Perfil(Modo.MORADOR, condominioId, nome, casa.unidadeId, casa.unidade)
        }
        return portaria.sortedBy { it.condominioNome } + morador.sortedBy { it.condominioNome }
    }

    /**
     * Qual perfil abrir: o lembrado (se ainda vale), ou o único disponível. Null = a pessoa escolhe
     * (mais de um) ou não tem acesso pelo app (nenhum).
     */
    fun escolher(disponiveis: List<Perfil>, lembrado: String?): Perfil? =
        disponiveis.firstOrNull { it.chave == lembrado } ?: disponiveis.singleOrNull()
}
