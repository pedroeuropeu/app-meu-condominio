package br.com.condovitta.app.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PerfisTest {

    private fun me(vararg m: Membership) = Me(1, "Ana Souza", "ana@x.com", false, m.toList())
    private fun v(condo: Long, papel: Papel, unidadeId: Long? = null, unidade: String? = null, nome: String = "Condo $condo") =
        Membership(condo * 10 + papel.ordinal, condo, nome, unidadeId, unidade, papel)

    @Test fun `morador entra direto no app do morador`() {
        val p = Perfis.disponiveis(me(v(1, Papel.MORADOR, 204, "B-204")))
        assertThat(p).containsExactly(Perfil(Modo.MORADOR, 1, "Condo 1", 204, "B-204"))
        assertThat(Perfis.escolher(p, null)?.modo).isEqualTo(Modo.MORADOR)
    }

    @Test fun `porteiro entra direto na portaria`() {
        val p = Perfis.disponiveis(me(v(1, Papel.PORTARIA)))
        assertThat(p.single().modo).isEqualTo(Modo.PORTARIA)
        assertThat(Perfis.escolher(p, null)?.modo).isEqualTo(Modo.PORTARIA)
    }

    @Test fun `sindico com unidade escolhe entre portaria e morador`() {
        val p = Perfis.disponiveis(me(v(1, Papel.SINDICO, 102, "A-102")))
        assertThat(p.map { it.modo }).containsExactly(Modo.PORTARIA, Modo.MORADOR).inOrder()
        assertThat(Perfis.escolher(p, null)).isNull()
        assertThat(Perfis.escolher(p, "MORADOR:1")?.unidade).isEqualTo("A-102")
    }

    @Test fun `sindico sem unidade so tem a portaria`() {
        val p = Perfis.disponiveis(me(v(1, Papel.SINDICO)))
        assertThat(p.map { it.modo }).containsExactly(Modo.PORTARIA)
    }

    @Test fun `conselho com unidade e morador, sem portaria`() {
        val p = Perfis.disponiveis(me(v(1, Papel.CONSELHO, 301, "B-301")))
        assertThat(p.map { it.modo }).containsExactly(Modo.MORADOR)
    }

    @Test fun `prefere a unidade do vinculo de morador`() {
        val p = Perfis.disponiveis(me(v(1, Papel.SINDICO, 999, "ADM"), v(1, Papel.MORADOR, 204, "B-204")))
        assertThat(p.first { it.modo == Modo.MORADOR }.unidade).isEqualTo("B-204")
    }

    @Test fun `dois condominios viram dois perfis`() {
        val p = Perfis.disponiveis(me(v(2, Papel.MORADOR, 1, "101", "Zeta"), v(1, Papel.MORADOR, 2, "202", "Alfa")))
        assertThat(p.map { it.condominioNome }).containsExactly("Alfa", "Zeta").inOrder()
        assertThat(Perfis.escolher(p, null)).isNull()
        assertThat(Perfis.escolher(p, "MORADOR:2")?.condominioNome).isEqualTo("Zeta")
    }

    @Test fun `perfil lembrado que nao vale mais e ignorado`() {
        val p = Perfis.disponiveis(me(v(1, Papel.MORADOR, 204, "B-204")))
        assertThat(Perfis.escolher(p, "PORTARIA:1")?.modo).isEqualTo(Modo.MORADOR)
    }

    @Test fun `funcionario ou administracao sem portaria nao tem perfil de morador`() {
        assertThat(Perfis.disponiveis(me(v(1, Papel.FUNCIONARIO)))).isEmpty()
        assertThat(Perfis.disponiveis(me(v(1, Papel.ADMINISTRACAO))).map { it.modo }).containsExactly(Modo.PORTARIA)
        assertThat(Perfis.disponiveis(me(v(1, Papel.DESCONHECIDO)))).isEmpty()
    }

    @Test fun `saudacao pelo horario`() {
        assertThat(br.com.condovitta.app.feature.home.saudacao(7)).isEqualTo("Bom dia")
        assertThat(br.com.condovitta.app.feature.home.saudacao(12)).isEqualTo("Boa tarde")
        assertThat(br.com.condovitta.app.feature.home.saudacao(19)).isEqualTo("Boa noite")
        assertThat(br.com.condovitta.app.feature.home.saudacao(2)).isEqualTo("Boa noite")
    }

    @Test fun `iniciais e primeiro nome`() {
        assertThat(Me(1, "Carlos Mendes da Silva", "c@x.com", false, emptyList()).iniciais).isEqualTo("CS")
        assertThat(Me(1, "Carlos", "c@x.com", false, emptyList()).primeiroNome).isEqualTo("Carlos")
        assertThat(Me(1, "", "joao@x.com", false, emptyList()).primeiroNome).isEqualTo("joao")
    }
}
