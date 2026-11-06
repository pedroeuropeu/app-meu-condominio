package br.com.condovitta.app.data.demo

import br.com.condovitta.app.core.common.TimeProvider
import br.com.condovitta.app.domain.Anexo
import br.com.condovitta.app.domain.AppError
import br.com.condovitta.app.domain.ArquivoEnvio
import br.com.condovitta.app.domain.AtendimentoRepository
import br.com.condovitta.app.domain.CategoriaChamado
import br.com.condovitta.app.domain.Chamado
import br.com.condovitta.app.domain.ChamadoDetalhe
import br.com.condovitta.app.domain.MensagemChamado
import br.com.condovitta.app.domain.MeusChamados
import br.com.condovitta.app.domain.MotivoOcorrencia
import br.com.condovitta.app.domain.NovaOcorrencia
import br.com.condovitta.app.domain.NovoChamado
import br.com.condovitta.app.domain.OcorrenciaContra
import br.com.condovitta.app.domain.OcorrenciaMinha
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.PainelOcorrencias
import br.com.condovitta.app.domain.PrioridadeChamado
import br.com.condovitta.app.domain.ResumoChamados
import br.com.condovitta.app.domain.StatusChamado
import br.com.condovitta.app.domain.StatusOcorrencia
import br.com.condovitta.app.domain.UnidadeOpcao
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Base dos repositórios de exemplo: pequena espera (para ver o carregando), trava e erros de regra. */
abstract class DemoBase(protected val time: TimeProvider) {
    private val lock = Mutex()
    private var seq = 20_000L
    protected fun nextId() = ++seq

    protected class DemoError(val error: AppError) : RuntimeException()

    protected fun falha(msg: String): Nothing = throw DemoError(AppError.Validation(msg))
    protected fun naoAchou(): Nothing = throw DemoError(AppError.NotFound)

    protected suspend fun <T> op(block: () -> T): Outcome<T> {
        delay(200)
        return lock.withLock {
            try { Outcome.Ok(block()) } catch (e: DemoError) { Outcome.Err(e.error) }
        }
    }
}

/** Chamados e ocorrências da Ana Souza (B-204). */
@Singleton
class DemoAtendimentoRepository @Inject constructor(time: TimeProvider) : DemoBase(time), AtendimentoRepository {

    private fun hoje() = time.today()
    private fun dia(d: Long, h: Int, m: Int = 0) = hoje().minusDays(d).atTime(h, m)

    private val arquivos = mutableMapOf<Long, ByteArray>()

    private fun anexar(lista: List<ArquivoEnvio>): List<Anexo> = lista.map { a ->
        val id = nextId()
        arquivos[id] = a.bytes
        Anexo(id, a.nome, a.bytes.size.toLong(), a.mime)
    }

    private val chamados = mutableListOf(
        ChamadoDetalhe(
            Chamado(101, "Vazamento no teto do banheiro", CategoriaChamado.MANUTENCAO, PrioridadeChamado.ALTA, StatusChamado.EM_ANDAMENTO,
                "Banheiro social", "B-204", "Zelador Jorge", dia(2, 9, 15), dia(-1, 18), null, dia(0, 8, 40),
                "Jorge vai subir hoje às 14h para ver o apartamento de cima.", true, null, 1),
            "Mancha de umidade crescendo no teto do banheiro social, perto do box. Pinga quando o vizinho de cima usa o chuveiro.",
            null, true, null,
            listOf(
                MensagemChamado(1, "Você", false, true, false, "Mancha de umidade crescendo no teto do banheiro social.", dia(2, 9, 15), emptyList()),
                MensagemChamado(2, null, false, false, true, "Chamado atribuído a Zelador Jorge.", dia(2, 10, 2), emptyList()),
                MensagemChamado(3, "Jorge (zeladoria)", true, false, false, "Bom dia, Ana! Vou subir hoje às 14h para ver o apartamento de cima (B-304).", dia(0, 8, 40), emptyList())
            ),
            listOf(Anexo(901, "mancha-teto.pdf", 182_000, "application/pdf"))
        ),
        ChamadoDetalhe(
            Chamado(98, "Lâmpada queimada no corredor do 2º andar", CategoriaChamado.MANUTENCAO, PrioridadeChamado.BAIXA, StatusChamado.RESOLVIDO,
                "Bloco B · 2º andar", "B-204", "Zelador Jorge", dia(9, 19, 30), null, dia(7, 11), dia(7, 11),
                "Troquei a lâmpada hoje cedo. Pode conferir?", true, null, 0),
            "A lâmpada do corredor perto do elevador está queimada desde ontem.", null, false, null,
            listOf(
                MensagemChamado(4, "Você", false, true, false, "A lâmpada do corredor perto do elevador está queimada.", dia(9, 19, 30), emptyList()),
                MensagemChamado(5, "Jorge (zeladoria)", true, false, false, "Troquei a lâmpada hoje cedo. Pode conferir?", dia(7, 11), emptyList())
            ),
            emptyList()
        ),
        ChamadoDetalhe(
            Chamado(90, "Segunda via do boleto de setembro", CategoriaChamado.FINANCEIRO, PrioridadeChamado.MEDIA, StatusChamado.CONCLUIDO,
                null, "B-204", "Administração", dia(40, 10), null, dia(39, 15), dia(39, 15), "Enviamos por e-mail.", true, 5, 0),
            "Não recebi o boleto de setembro.", null, false, "Resolveram no mesmo dia, obrigada!",
            listOf(MensagemChamado(6, "Administração", true, false, false, "Enviamos por e-mail. Também está no app em Boletos.", dia(39, 15), emptyList())),
            emptyList()
        )
    )

    private fun resumo() = ResumoChamados(
        chamados.count { it.chamado.status.aberto }.toLong(),
        chamados.count { it.chamado.status == StatusChamado.AGUARDANDO }.toLong(),
        chamados.count { !it.chamado.status.aberto && it.chamado.status != StatusChamado.CANCELADO }.toLong(),
        95
    )

    private fun achar(id: Long): Int = chamados.indexOfFirst { it.chamado.id == id }.takeIf { it >= 0 } ?: naoAchou()

    override suspend fun chamados(): Outcome<MeusChamados> = op { MeusChamados(chamados.map { it.chamado }.sortedByDescending { it.abertoEm }, resumo()) }

    override suspend fun chamado(id: Long): Outcome<ChamadoDetalhe> = op { chamados[achar(id)] }

    override suspend fun abrirChamado(form: NovoChamado, arquivos: List<ArquivoEnvio>): Outcome<ChamadoDetalhe> = op {
        if (form.titulo.isBlank()) falha("Dê um título para o chamado.")
        val agora = time.now()
        val anexos = anexar(arquivos)
        val d = ChamadoDetalhe(
            Chamado(nextId(), form.titulo.trim(), form.categoria, form.prioridade, StatusChamado.ABERTO, form.local, "B-204", null, agora,
                agora.plusDays(if (form.prioridade == PrioridadeChamado.URGENTE) 1 else 3), null, agora, form.descricao, false, null, anexos.size),
            form.descricao, null, form.autorizaEntrada, null,
            listOf(MensagemChamado(nextId(), "Você", false, true, false, form.descricao ?: form.titulo, agora, anexos)),
            anexos
        )
        chamados.add(0, d)
        d
    }

    override suspend fun responder(id: Long, texto: String?, arquivos: List<ArquivoEnvio>): Outcome<ChamadoDetalhe> = op {
        val i = achar(id)
        val d = chamados[i]
        if (!d.chamado.status.aberto) falha("Este chamado está fechado. Reabra para continuar.")
        if (texto.isNullOrBlank() && arquivos.isEmpty()) falha("Escreva uma mensagem ou anexe um arquivo.")
        val agora = time.now()
        val anexos = anexar(arquivos)
        val status = if (d.chamado.status == StatusChamado.AGUARDANDO) StatusChamado.EM_ANDAMENTO else d.chamado.status
        val novo = d.copy(
            chamado = d.chamado.copy(status = status, ultimaEm = agora, ultimaTexto = texto ?: "Anexo", ultimaDaAdministracao = false, anexos = d.chamado.anexos + anexos.size),
            aguardandoMotivo = null,
            mensagens = d.mensagens + MensagemChamado(nextId(), "Você", false, true, false, texto?.trim(), agora, anexos)
        )
        chamados[i] = novo
        novo
    }

    override suspend fun avaliar(id: Long, nota: Int, comentario: String?): Outcome<Unit> = op {
        val i = achar(id)
        val d = chamados[i]
        if (d.chamado.status != StatusChamado.RESOLVIDO) falha("Só dá para avaliar um chamado resolvido.")
        if (nota !in 1..5) falha("Escolha de 1 a 5 estrelas.")
        chamados[i] = d.copy(chamado = d.chamado.copy(status = StatusChamado.CONCLUIDO, avaliacao = nota), avaliacaoComentario = comentario)
    }

    override suspend fun reabrir(id: Long): Outcome<ChamadoDetalhe> = op {
        val i = achar(id)
        val d = chamados[i]
        val novo = d.copy(
            chamado = d.chamado.copy(status = StatusChamado.ABERTO, resolvidoEm = null, avaliacao = null, ultimaEm = time.now()),
            mensagens = d.mensagens + MensagemChamado(nextId(), null, false, false, true, "Chamado reaberto por Ana Souza.", time.now(), emptyList())
        )
        chamados[i] = novo
        novo
    }

    override suspend fun cancelarChamado(id: Long): Outcome<Unit> = op {
        val i = achar(id)
        if (!chamados[i].chamado.status.aberto) falha("Este chamado já está fechado.")
        chamados[i] = chamados[i].copy(chamado = chamados[i].chamado.copy(status = StatusChamado.CANCELADO))
    }

    override suspend fun anexoChamado(id: Long, anexoId: Long): Outcome<ByteArray> = op { arquivos[anexoId] ?: DemoPdf.boleto("Anexo do chamado $id") }

    /* ---------------- ocorrências ---------------- */

    private val minhas = mutableListOf(
        OcorrenciaMinha(301, "OC-2026-0147", MotivoOcorrencia.BARULHO_EXCESSIVO, "B-304", "Apartamento de cima", StatusOcorrencia.ADVERTENCIA,
            "Som alto e arrastar de móveis depois das 23h por três noites seguidas.", "Larissa (B-203)", dia(20, 23, 40), dia(19, 8),
            hoje().minusDays(14), true, "Advertência registrada. O morador foi orientado sobre o horário de silêncio (22h às 8h).", dia(10, 16), emptyList())
    )

    private val contra = mutableListOf(
        OcorrenciaContra(310, "OC-2026-0162", MotivoOcorrencia.ESTACIONAMENTO_IRREGULAR, "Garagem · vaga de visitante", StatusOcorrencia.EM_ANALISE,
            "Veículo SBA2C04 estacionado na vaga de visitante das 19h às 23h.", dia(1, 19), hoje().plusDays(4),
            null, null, null, null, null, null, emptyList()),
        OcorrenciaContra(288, "OC-2026-0098", MotivoOcorrencia.USO_INDEVIDO_AREA_COMUM, "Piscina", StatusOcorrencia.ARQUIVADA,
            "Visitante na piscina sem o morador acompanhando.", dia(60, 15), hoje().minusDays(55),
            "Minha irmã estava comigo; eu tinha subido para buscar toalhas por 5 minutos.", "Ana Souza", dia(58, 10),
            "Arquivada: a defesa foi aceita.", dia(50, 14), null, emptyList())
    )

    private fun painel() = PainelOcorrencias(minhas.sortedByDescending { it.registradaEm }, contra.sortedByDescending { it.ocorridaEm })

    override suspend fun ocorrencias(): Outcome<PainelOcorrencias> = op { painel() }

    override suspend fun unidades(): Outcome<List<UnidadeOpcao>> = op {
        listOf("A-101", "A-102", "A-501", "B-102", "B-203", "B-304", "C-303", "D-305", "D-402", "D-601")
            .mapIndexed { i, r -> UnidadeOpcao(i + 1L, r, "Bloco ${r.first()}") }
    }

    override suspend fun registrarOcorrencia(form: NovaOcorrencia, provas: List<ArquivoEnvio>): Outcome<PainelOcorrencias> = op {
        if (!form.geral && form.unidade.isNullOrBlank()) falha("Escolha a unidade.")
        if (form.unidade == "B-204" && !form.geral) falha("Não é possível registrar contra a sua própria unidade.")
        if (form.ocorridaEm.isAfter(time.now())) falha("A data não pode ser no futuro.")
        if (form.descricao.isBlank()) falha("Descreva o que aconteceu.")
        val id = nextId()
        minhas.add(0, OcorrenciaMinha(id, "OC-2026-0${id % 1000}", form.motivo, if (form.geral) null else form.unidade, form.local,
            StatusOcorrencia.EM_ANALISE, form.descricao.trim(), form.testemunhas, form.ocorridaEm, time.now(), hoje().plusDays(5), false, null, null, anexar(provas)))
        painel()
    }

    override suspend fun defesa(id: Long, texto: String, provas: List<ArquivoEnvio>): Outcome<PainelOcorrencias> = op {
        val i = contra.indexOfFirst { it.id == id }.takeIf { it >= 0 } ?: naoAchou()
        val o = contra[i]
        if (!o.podeDefender(hoje())) falha("O prazo para a defesa terminou ou ela já foi enviada.")
        if (texto.isBlank()) falha("Escreva a sua defesa.")
        contra[i] = o.copy(defesa = texto.trim(), defesaAutor = "Ana Souza", defesaEm = time.now(), provasDefesa = anexar(provas))
        painel()
    }

    override suspend fun anexoOcorrencia(id: Long, anexoId: Long): Outcome<ByteArray> = op { arquivos[anexoId] ?: DemoPdf.boleto("Prova da ocorrência $id") }
}
