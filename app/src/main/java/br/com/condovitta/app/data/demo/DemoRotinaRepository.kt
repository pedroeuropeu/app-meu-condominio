package br.com.condovitta.app.data.demo

import br.com.condovitta.app.core.common.TimeProvider
import br.com.condovitta.app.domain.AppError
import br.com.condovitta.app.domain.ChaveEmprestada
import br.com.condovitta.app.domain.ConsultaPlaca
import br.com.condovitta.app.domain.Domestico
import br.com.condovitta.app.domain.GasMes
import br.com.condovitta.app.domain.LeituraGas
import br.com.condovitta.app.domain.Livro
import br.com.condovitta.app.domain.MinhaUnidade
import br.com.condovitta.app.domain.MoradorUnidade
import br.com.condovitta.app.domain.MovimentoGaragem
import br.com.condovitta.app.domain.NovoRegistro
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.Pet
import br.com.condovitta.app.domain.RegistroLivro
import br.com.condovitta.app.domain.RotinaRepository
import br.com.condovitta.app.domain.SentidoGaragem
import br.com.condovitta.app.domain.TipoRegistro
import br.com.condovitta.app.domain.Veiculo
import br.com.condovitta.app.domain.VeiculosPortaria
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/** Livro, garagem e gás do turno do Carlos, e a unidade B-204 da Ana (dados do prototype/data.js). */
@Singleton
class DemoRotinaRepository @Inject constructor(private val time: TimeProvider) : RotinaRepository {

    private val lock = Mutex()
    private var seq = 9000L
    private fun at(h: Int, m: Int) = time.today().atTime(h, m)

    private val registros = mutableListOf(
        RegistroLivro(1, at(9, 12), TipoRegistro.OCORRENCIA, "Carro na vaga de visitante há mais de 4 h (placa PQA1B23). Morador avisado.", "B-102", false, "Carlos Mendes", false),
        RegistroLivro(2, at(8, 30), TipoRegistro.RONDA, "Ronda da manhã: portão de serviço estava destrancado; trancado e registrado.", null, false, "Carlos Mendes", false),
        RegistroLivro(3, at(8, 10), TipoRegistro.CHAVE, "Chave do salão emprestada para o A-501 (decoração da festa de hoje).", "A-501", false, "Carlos Mendes", false),
        RegistroLivro(4, at(8, 5), TipoRegistro.PRESTADOR, "Técnico da Elevadores Atlas na casa de máquinas do Bloco A.", null, false, "Carlos Mendes", false),
        RegistroLivro(5, at(6, 2), TipoRegistro.PASSAGEM, "Passagem de turno: Antônio Ferreira → Carlos Mendes. Assinada pelos dois.", null, false, "Antônio Ferreira", true)
    )
    private val chaves = mutableListOf(ChaveEmprestada(1, "Salão de festas", "Marcos Teixeira (A-501)", at(8, 10), "23h"))

    private val veiculos = listOf(
        Veiculo(1, "SBA2C04", "Compass", "Branco", "B-204", "Ana Souza", "B-17", "TAG 004821", false, null),
        Veiculo(2, "RXL1A02", "Corolla Cross", "Preto", "A-102", "Roberto Lima", "A-02", "Leitura de placa", false, null),
        Veiculo(3, "QPT6B52", "Kicks", "Vermelho", "B-204", "Ana Souza", "B-18", "TAG 004822", false, null),
        Veiculo(4, "RVX3C12", "HB20", "Prata", "C-303", "Larissa Alves", "C-11", "TAG 003310", true, "Condomínio em atraso há 3 meses")
    )
    private val movimentos = mutableListOf(
        MovimentoGaragem(1, at(7, 41), "PQA1B23", SentidoGaragem.ENTRADA, "Social", "Patrícia Nunes → B-102", "Carlos Mendes"),
        MovimentoGaragem(2, at(7, 15), "RXL1A02", SentidoGaragem.SAIDA, "Garagem", "A-102 · morador", "Carlos Mendes")
    )

    private val mes = YearMonth.from(time.today()).toString()
    private val gas = DemoPortariaRepository.RESPONSAVEIS.entries.mapIndexed { i, (u, nome) ->
        val anterior = BigDecimal(120 + i * 7).setScale(3)
        val lida = !u.startsWith("D")
        val atual = if (lida) anterior.add(BigDecimal(if (u == "C-305") "19.400" else "${4 + i % 5}.${(i * 37) % 1000}".padEnd(5, '0'))) else null
        LeituraGas(i + 1L, u, u.take(1), nome, anterior, atual, atual?.subtract(anterior), if (u == "C-305") "Consumo 3x acima da média" else null, if (lida) at(7, 0).plusMinutes(i * 3L) else null)
    }.toMutableList()

    private val pets = mutableListOf(Pet(1, "Mel", "Cachorro", "Shih-tzu", "Pequeno", time.today().minusMonths(4)))
    private val domesticos = mutableListOf(Domestico(1, "Juliana Martins", "CPF ***.551.208-**", "Diarista", "seg, qua e sex · 10h às 17h"))

    private class DemoError(val error: AppError) : RuntimeException()
    private fun falha(msg: String): Nothing = throw DemoError(AppError.Validation(msg))
    private suspend fun <T> op(block: () -> T): Outcome<T> {
        delay(200)
        return lock.withLock { try { Outcome.Ok(block()) } catch (e: DemoError) { Outcome.Err(e.error) } }
    }

    override suspend fun livro(): Outcome<Livro> = op { Livro(registros.sortedByDescending { it.quando }, chaves.toList()) }

    override suspend fun registrar(form: NovoRegistro): Outcome<RegistroLivro> = op {
        if (form.texto.trim().length < 8) falha("Descreva o que aconteceu (pelo menos uma frase).")
        val unidade = form.unidadeId?.let { id -> DemoPortariaRepository.RESPONSAVEIS.keys.elementAtOrNull((id - 1).toInt()) }
        RegistroLivro(++seq, time.now(), form.tipo, form.texto.trim(), unidade, form.avisarSindico, "Carlos Mendes", false).also { registros += it }
    }

    override suspend fun excluirRegistro(id: Long): Outcome<Unit> = op {
        val r = registros.firstOrNull { it.id == id } ?: throw DemoError(AppError.NotFound)
        if (r.travado) falha("Este registro é de outro turno e não pode mais ser apagado.")
        registros.remove(r)
        Unit
    }

    override suspend fun devolverChave(id: Long): Outcome<Unit> = op {
        val c = chaves.firstOrNull { it.id == id } ?: throw DemoError(AppError.NotFound)
        chaves.remove(c)
        registros += RegistroLivro(++seq, time.now(), TipoRegistro.CHAVE, "Chave ${c.chave} devolvida por ${c.com ?: "morador"}.", null, false, "Carlos Mendes", false)
        Unit
    }

    override suspend fun veiculos(): Outcome<VeiculosPortaria> = op { VeiculosPortaria(veiculos, movimentos.sortedByDescending { it.quando }) }

    override suspend fun consultarPlaca(placa: String): Outcome<ConsultaPlaca> = op {
        val p = placa.uppercase().filter { it.isLetterOrDigit() }
        if (p.length < 7) falha("Informe a placa completa (7 caracteres).")
        ConsultaPlaca(p, veiculos.filter { it.placa == p }, emptyList())
    }

    override suspend fun movimento(placa: String, sentido: SentidoGaragem, portao: String?): Outcome<MovimentoGaragem> = op {
        val p = placa.uppercase().filter { it.isLetterOrDigit() }
        if (p.length < 7) falha("Informe a placa completa (7 caracteres).")
        val v = veiculos.firstOrNull { it.placa == p }
        MovimentoGaragem(++seq, time.now(), p, sentido, portao ?: "Garagem", v?.let { "${it.unidade} · morador" } ?: "Não identificado", "Carlos Mendes").also { movimentos += it }
    }

    override suspend fun gas(mes: String?): Outcome<GasMes> = op {
        GasMes(this.mes, false, gas.size, gas.count { it.lida }, gas.count { it.alerta != null }, gas.sortedWith(compareBy({ it.lida }, { it.unidade })))
    }

    override suspend fun lancarGas(mes: String, unidadeId: Long, leitura: BigDecimal): Outcome<LeituraGas> = op {
        val i = gas.indexOfFirst { it.unidadeId == unidadeId }
        if (i < 0) throw DemoError(AppError.NotFound)
        val l = gas[i]
        val consumo = l.anterior?.let { leitura.subtract(it) }
        val alerta = when {
            consumo != null && consumo.signum() < 0 -> "Leitura menor que a anterior: confira o medidor"
            consumo != null && consumo > BigDecimal("15") -> "Consumo acima do normal"
            else -> null
        }
        l.copy(atual = leitura.setScale(3, RoundingMode.HALF_UP), consumo = consumo, alerta = alerta, lidaEm = time.now()).also { gas[i] = it }
    }

    override suspend fun minhaUnidade(): Outcome<MinhaUnidade> = op {
        MinhaUnidade(
            "Residencial Vila do Sol 2", "B-204", 2, "B-17 e B-18 (garagem coberta)",
            listOf(
                MoradorUnidade(1, "Ana Souza", "Proprietária", "ana.souza@email.com", "(62) 99812-4471", true, "Com acesso ao app"),
                MoradorUnidade(2, "Pedro Souza", "Cônjuge", "pedro.souza@email.com", "(62) 99812-4472", false, "Com acesso ao app"),
                MoradorUnidade(3, "Laura Souza", "Filha", null, null, false, "Sem acesso")
            ),
            pets.toList(), domesticos.toList(), true
        )
    }

    override suspend fun adicionarPet(nome: String, especie: String?, raca: String?, porte: String?): Outcome<Pet> = op {
        if (nome.isBlank()) falha("Informe o nome do pet.")
        Pet(++seq, nome.trim(), especie, raca, porte, null).also { pets += it }
    }

    override suspend fun removerPet(id: Long): Outcome<Unit> = op { pets.removeAll { it.id == id }; Unit }

    override suspend fun adicionarDomestico(nome: String, documento: String?, funcao: String?, diasHorarios: String?): Outcome<Domestico> = op {
        if (nome.isBlank()) falha("Informe o nome.")
        Domestico(++seq, nome.trim(), documento, funcao, diasHorarios).also { domesticos += it }
    }

    override suspend fun removerDomestico(id: Long): Outcome<Unit> = op { domesticos.removeAll { it.id == id }; Unit }
}
