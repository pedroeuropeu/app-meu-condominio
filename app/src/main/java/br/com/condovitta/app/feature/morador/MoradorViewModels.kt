package br.com.condovitta.app.feature.morador

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.condovitta.app.core.common.TimeProvider
import br.com.condovitta.app.domain.Autorizacao
import br.com.condovitta.app.domain.Aviso
import br.com.condovitta.app.domain.DiaSemana
import br.com.condovitta.app.domain.EscopoAutorizado
import br.com.condovitta.app.domain.Financeiro
import br.com.condovitta.app.domain.MinhasEncomendas
import br.com.condovitta.app.domain.MinhasVisitas
import br.com.condovitta.app.domain.MoradorRepository
import br.com.condovitta.app.domain.NovaAutorizacao
import br.com.condovitta.app.domain.NovoAutorizado
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.TipoVisitante
import br.com.condovitta.app.domain.Visita
import br.com.condovitta.app.feature.portaria.Loadable
import br.com.condovitta.app.ui.Messenger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

private suspend fun <T> MutableStateFlow<Loadable<T>>.load(block: suspend () -> Outcome<T>) {
    update { it.copy(loading = true, error = null) }
    value = when (val r = block()) {
        is Outcome.Ok -> Loadable(r.value, loading = false)
        is Outcome.Err -> Loadable(value.data, false, r.error.message)
    }
}

/** Responder à portaria ("fulano está aqui, posso liberar?"), usado no Início e em Visitantes. */
private fun ViewModel.responderConsulta(repo: MoradorRepository, messenger: Messenger, c: Visita, liberar: Boolean, recado: String?, after: () -> Unit) {
    val acesso = c.acessoId ?: return
    viewModelScope.launch {
        when (val r = repo.responder(acesso, liberar, recado)) {
            is Outcome.Ok -> messenger.show(if (liberar) "${c.nome} liberado(a) · a portaria já foi avisada" else "Entrada de ${c.nome} recusada")
            is Outcome.Err -> messenger.show(r.error.message)
        }
        after()
    }
}

data class InicioData(
    val financeiro: Financeiro?,
    val visitas: MinhasVisitas?,
    val encomendas: MinhasEncomendas?,
    val avisos: List<Aviso>
)

/** Início: boleto do mês, consultas da portaria, encomendas para retirar e avisos — cada parte carrega por conta própria. */
@HiltViewModel
class InicioViewModel @Inject constructor(private val repo: MoradorRepository, private val messenger: Messenger) : ViewModel() {
    private val _state = MutableStateFlow(Loadable<InicioData>())
    val state: StateFlow<Loadable<InicioData>> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val f = async { repo.financeiro() }
            val v = async { repo.visitas() }
            val e = async { repo.encomendas() }
            val a = async { repo.avisos() }
            val rf = f.await(); val rv = v.await(); val re = e.await(); val ra = a.await()
            val tudoFalhou = listOf(rf, rv, re, ra).all { it is Outcome.Err }
            _state.value = if (tudoFalhou) Loadable(_state.value.data, false, (rf as Outcome.Err).error.message)
            else Loadable(
                InicioData((rf as? Outcome.Ok)?.value, (rv as? Outcome.Ok)?.value, (re as? Outcome.Ok)?.value, (ra as? Outcome.Ok)?.value.orEmpty()),
                loading = false
            )
        }
    }

    fun responder(c: Visita, liberar: Boolean, recado: String? = null) = responderConsulta(repo, messenger, c, liberar, recado, ::refresh)
}

@HiltViewModel
class BoletosViewModel @Inject constructor(private val repo: MoradorRepository, private val messenger: Messenger) : ViewModel() {
    private val _state = MutableStateFlow(Loadable<Financeiro>())
    val state: StateFlow<Loadable<Financeiro>> = _state.asStateFlow()
    val baixando = MutableStateFlow<Long?>(null)
    val qr = MutableStateFlow<ByteArray?>(null)

    fun refresh() = viewModelScope.launch { _state.load { repo.financeiro() } }

    /** baixa o PDF e entrega os bytes para a tela abrir no leitor do aparelho */
    fun pdf(id: Long, abrir: (ByteArray) -> Unit) {
        if (baixando.value != null) return
        baixando.value = id
        viewModelScope.launch {
            when (val r = repo.boletoPdf(id)) {
                is Outcome.Ok -> abrir(r.value)
                is Outcome.Err -> messenger.show(r.error.message)
            }
            baixando.value = null
        }
    }

    fun carregarQr(id: Long) {
        qr.value = null
        viewModelScope.launch { (repo.pixQr(id) as? Outcome.Ok)?.let { qr.value = it.value } }
    }
}

@HiltViewModel
class VisitasViewModel @Inject constructor(
    private val repo: MoradorRepository,
    private val messenger: Messenger,
    private val time: TimeProvider
) : ViewModel() {
    private val _state = MutableStateFlow(Loadable<MinhasVisitas>())
    val state: StateFlow<Loadable<MinhasVisitas>> = _state.asStateFlow()
    fun agora(): LocalDateTime = time.now()

    fun refresh() = viewModelScope.launch { _state.load { repo.visitas() } }

    fun responder(c: Visita, liberar: Boolean, recado: String? = null) = responderConsulta(repo, messenger, c, liberar, recado, ::refresh)
}

/** Detalhe de uma autorização: QR e código para compartilhar, e revogar (com Desfazer). */
@HiltViewModel
class AutorizacaoViewModel @Inject constructor(
    private val repo: MoradorRepository,
    private val messenger: Messenger,
    private val time: TimeProvider,
    saved: SavedStateHandle
) : ViewModel() {
    private val id: Long = checkNotNull(saved.get<Long>("id"))
    private val _state = MutableStateFlow(Loadable<Autorizacao>())
    val state: StateFlow<Loadable<Autorizacao>> = _state.asStateFlow()
    val qr = MutableStateFlow<ByteArray?>(null)
    val revogada = MutableStateFlow(false)
    fun agora(): LocalDateTime = time.now()

    fun refresh() {
        viewModelScope.launch {
            _state.load {
                when (val r = repo.visitas()) {
                    is Outcome.Ok -> r.value.autorizacoes.firstOrNull { it.id == id }?.let { Outcome.Ok(it) }
                        ?: Outcome.Err(br.com.condovitta.app.domain.AppError.NotFound)
                    is Outcome.Err -> r
                }
            }
            if (qr.value == null) (repo.qrVisitante(id) as? Outcome.Ok)?.let { qr.value = it.value }
        }
    }

    fun revogar() {
        val a = _state.value.data ?: return
        viewModelScope.launch {
            when (val r = repo.revogar(a.id)) {
                is Outcome.Ok -> {
                    revogada.value = true
                    messenger.undoable("Autorização de ${a.nome} revogada", restore = {
                        when (val re = repo.reativar(a.id)) {
                            is Outcome.Ok -> Outcome.Ok(Unit)
                            is Outcome.Err -> re
                        }
                    })
                }
                is Outcome.Err -> messenger.show(r.error.message)
            }
        }
    }
}

enum class QuandoVisita { AGORA, DATA, RECORRENTE }

data class NovaAutorizacaoUi(
    val nome: String = "",
    val tipo: TipoVisitante = TipoVisitante.VISITA,
    val documento: String = "",
    val telefone: String = "",
    val placa: String = "",
    val obs: String = "",
    val quando: QuandoVisita = QuandoVisita.AGORA,
    val data: LocalDate? = null,
    val horaInicio: LocalTime = LocalTime.of(8, 0),
    val horaFim: LocalTime = LocalTime.of(18, 0),
    val dias: Set<DiaSemana> = emptySet(),
    val validoAte: LocalDate? = null,
    val saving: Boolean = false,
    val error: String? = null
) {
    val valido: Boolean
        get() = nome.trim().length >= 3 && horaFim.isAfter(horaInicio) &&
            (quando != QuandoVisita.DATA || data != null) && (quando != QuandoVisita.RECORRENTE || dias.isNotEmpty())
}

/** Pré-autorizar visita ou prestador: agora (próximas horas), num dia e horário, ou recorrente (diarista, babá). */
@HiltViewModel
class NovaAutorizacaoViewModel @Inject constructor(
    private val repo: MoradorRepository,
    private val messenger: Messenger,
    private val time: TimeProvider
) : ViewModel() {
    private val _ui = MutableStateFlow(NovaAutorizacaoUi())
    val ui: StateFlow<NovaAutorizacaoUi> = _ui.asStateFlow()
    /** autorização criada: a tela mostra o código para compartilhar */
    val criada = MutableStateFlow<Autorizacao?>(null)
    val hoje: LocalDate get() = time.today()

    init {
        val agora = time.now().toLocalTime().withSecond(0).withNano(0)
        _ui.update { it.copy(horaInicio = agora, horaFim = agora.plusHours(4).let { f -> if (f.isBefore(agora)) LocalTime.of(23, 59) else f }) }
    }

    fun update(f: (NovaAutorizacaoUi) -> NovaAutorizacaoUi) = _ui.update { f(it).copy(error = null) }

    /** "Hoje" começa agora (próximas 4 h); outro dia ou toda semana começam no horário comercial (08:00–18:00). */
    fun quando(q: QuandoVisita) = update {
        if (q == QuandoVisita.AGORA) {
            val agora = time.now().toLocalTime().withSecond(0).withNano(0)
            it.copy(quando = q, horaInicio = agora, horaFim = agora.plusHours(4).let { f -> if (f.isBefore(agora)) LocalTime.of(23, 59) else f })
        } else if (it.quando == QuandoVisita.AGORA) it.copy(quando = q, horaInicio = LocalTime.of(8, 0), horaFim = LocalTime.of(18, 0))
        else it.copy(quando = q)
    }

    fun salvar() {
        val s = _ui.value
        if (!s.valido || s.saving) return
        _ui.update { it.copy(saving = true, error = null) }
        val form = when (s.quando) {
            QuandoVisita.AGORA -> base(s).copy(inicio = time.today().atTime(s.horaInicio), fim = time.today().atTime(s.horaFim))
            QuandoVisita.DATA -> base(s).copy(inicio = s.data!!.atTime(s.horaInicio), fim = s.data.atTime(s.horaFim))
            QuandoVisita.RECORRENTE -> base(s).copy(recorrente = true, dias = s.dias.sortedBy { it.ordinal }, horaInicio = s.horaInicio, horaFim = s.horaFim, validoAte = s.validoAte)
        }
        viewModelScope.launch {
            when (val r = repo.autorizar(form)) {
                is Outcome.Ok -> { criada.value = r.value; messenger.show("${r.value.nome} autorizado(a) · a portaria já sabe") }
                is Outcome.Err -> _ui.update { it.copy(saving = false, error = r.error.message) }
            }
        }
    }

    private fun base(s: NovaAutorizacaoUi) = NovaAutorizacao(s.nome, s.tipo, s.documento, s.telefone, s.placa, s.obs)
}

@HiltViewModel
class EncomendasMoradorViewModel @Inject constructor(private val repo: MoradorRepository, private val messenger: Messenger) : ViewModel() {
    private val _state = MutableStateFlow(Loadable<MinhasEncomendas>())
    val state: StateFlow<Loadable<MinhasEncomendas>> = _state.asStateFlow()
    val salvando = MutableStateFlow(false)
    val erroAutorizado = MutableStateFlow<String?>(null)
    val autorizadoOk = MutableStateFlow(0)

    fun refresh() = viewModelScope.launch { _state.load { repo.encomendas() } }

    fun autorizar(nome: String, documento: String, relacao: String, escopo: EscopoAutorizado, validoAte: LocalDate?) {
        if (salvando.value) return
        salvando.value = true
        erroAutorizado.value = null
        viewModelScope.launch {
            when (val r = repo.autorizarRetirada(NovoAutorizado(nome, documento, relacao, escopo, validoAte = validoAte))) {
                is Outcome.Ok -> { autorizadoOk.value++; messenger.show("${r.value.nome} pode retirar suas encomendas"); refresh() }
                is Outcome.Err -> erroAutorizado.value = r.error.message
            }
            salvando.value = false
        }
    }

    /** Remove sem "tem certeza?": some da lista e o servidor só recebe quando o aviso some sem "Desfazer". */
    fun remover(id: Long) {
        val atual = _state.value.data ?: return
        val a = atual.autorizados.firstOrNull { it.id == id } ?: return
        _state.value = Loadable(atual.copy(autorizados = atual.autorizados - a), loading = false)
        messenger.deferred("${a.nome} não pode mais retirar", undo = { refresh() }, commit = { repo.removerAutorizado(id) })
    }
}

@HiltViewModel
class AvisosViewModel @Inject constructor(private val repo: MoradorRepository, private val messenger: Messenger) : ViewModel() {
    private val _state = MutableStateFlow(Loadable<List<Aviso>>())
    val state: StateFlow<Loadable<List<Aviso>>> = _state.asStateFlow()
    val aberto = MutableStateFlow<Aviso?>(null)

    fun refresh() = viewModelScope.launch { _state.load { repo.avisos() } }

    /** abrir confirma a leitura (o síndico acompanha por unidade) */
    fun abrir(a: Aviso) {
        aberto.value = a
        if (!a.lido) viewModelScope.launch {
            if (repo.marcarLido(a.id) is Outcome.Ok) _state.update { s -> s.copy(data = s.data?.map { if (it.id == a.id) it.copy(lido = true) else it }) }
        }
    }

    fun fechar() { aberto.value = null }

    fun ciente(a: Aviso) {
        viewModelScope.launch {
            when (val r = repo.ciente(a.id)) {
                is Outcome.Ok -> {
                    val novo = a.copy(lido = true, ciente = true, cienteEm = LocalDateTime.now())
                    aberto.value = novo
                    _state.update { s -> s.copy(data = s.data?.map { if (it.id == a.id) novo else it }) }
                    messenger.show("Ciência registrada")
                }
                is Outcome.Err -> messenger.show(r.error.message)
            }
        }
    }
}
