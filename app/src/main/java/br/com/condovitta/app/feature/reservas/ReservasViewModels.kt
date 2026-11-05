package br.com.condovitta.app.feature.reservas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.condovitta.app.core.common.TimeProvider
import br.com.condovitta.app.domain.AreaComum
import br.com.condovitta.app.domain.Disponibilidade
import br.com.condovitta.app.domain.ModoAssinatura
import br.com.condovitta.app.domain.NovaReserva
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.Reserva
import br.com.condovitta.app.domain.ReservasPortaria
import br.com.condovitta.app.domain.ReservasRepository
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
import java.time.YearMonth
import javax.inject.Inject

data class ReservasData(val minhas: List<Reserva>, val areas: List<AreaComum>)

/** Aba Reservas do morador: as minhas reservas e o catálogo de áreas. */
@HiltViewModel
class ReservasViewModel @Inject constructor(private val repo: ReservasRepository, private val messenger: Messenger) : ViewModel() {
    private val _state = MutableStateFlow(Loadable<ReservasData>())
    val state: StateFlow<Loadable<ReservasData>> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val m = async { repo.minhas() }
            val a = async { repo.areas() }
            val rm = m.await(); val ra = a.await()
            _state.value = when {
                rm is Outcome.Ok && ra is Outcome.Ok -> Loadable(ReservasData(rm.value, ra.value), loading = false)
                else -> Loadable(_state.value.data, false, ((rm as? Outcome.Err) ?: (ra as Outcome.Err)).error.message)
            }
        }
    }

    /** cancela já (o backend decide se a taxa fica) e oferece "Desfazer" */
    fun cancelar(r: Reserva) {
        viewModelScope.launch {
            when (val c = repo.cancelar(r.id)) {
                is Outcome.Ok -> {
                    refresh()
                    messenger.undoable(c.value.mensagem ?: "Reserva cancelada", restore = {
                        when (val d = repo.desfazerCancelamento(r.id)) {
                            is Outcome.Ok -> Outcome.Ok(Unit)
                            is Outcome.Err -> d
                        }
                    }, onRestored = ::refresh)
                }
                is Outcome.Err -> messenger.show(c.error.message)
            }
        }
    }
}

data class AssistenteUi(
    val area: AreaComum? = null,
    val passo: Int = 0,
    val itemId: Long? = null,
    val mes: YearMonth = YearMonth.now(),
    val disp: Disponibilidade? = null,
    val carregandoDisp: Boolean = false,
    val data: LocalDate? = null,
    val turnoId: Long? = null,
    val convidados: Int = 10,
    val nomes: String = "",
    val observacoes: String = "",
    val aceite: Boolean = false,
    val modo: ModoAssinatura = ModoAssinatura.DESENHO,
    val nomeDigitado: String = "",
    val salvando: Boolean = false,
    val erro: String? = null,
    val criada: Reserva? = null
) {
    val item get() = area?.itens?.firstOrNull { it.id == itemId }
    val turno get() = area?.turnos?.firstOrNull { it.id == turnoId }
    /** o menor entre o máximo da área e a capacidade do espaço */
    val limite: Int get() = listOfNotNull(area?.maxConvidados, item?.capacidade).minOrNull() ?: 50
    val taxa get() = turno?.taxa ?: item?.taxa ?: area?.taxa

    fun podeAvancar(): Boolean = when (passo) {
        0 -> item?.disponivel == true
        1 -> data != null
        2 -> turnoId != null && convidados in 1..limite
        else -> area?.exigeTermo != true || (aceite && (modo == ModoAssinatura.DESENHO || nomeDigitado.trim().length >= 5))
    }
}

/** Assistente em 4 passos: espaço → data → horário e convidados → termo e confirmação. */
@HiltViewModel
class NovaReservaViewModel @Inject constructor(
    private val repo: ReservasRepository,
    private val time: TimeProvider,
    saved: SavedStateHandle
) : ViewModel() {
    private val areaId: Long = checkNotNull(saved.get<Long>("areaId"))
    private val _ui = MutableStateFlow(AssistenteUi(mes = YearMonth.from(time.today())))
    val ui: StateFlow<AssistenteUi> = _ui.asStateFlow()
    val hoje: LocalDate get() = time.today()

    init {
        viewModelScope.launch {
            when (val r = repo.areas()) {
                is Outcome.Ok -> {
                    val a = r.value.firstOrNull { it.id == areaId }
                    if (a == null) _ui.update { it.copy(erro = "Esta área não está mais disponível.") }
                    else _ui.update {
                        it.copy(area = a, itemId = a.itens.singleOrNull { i -> i.disponivel }?.id,
                            convidados = minOf(10, listOfNotNull(a.maxConvidados, a.itens.firstOrNull()?.capacidade).minOrNull() ?: 10))
                    }
                    carregarMes(_ui.value.mes)
                }
                is Outcome.Err -> _ui.update { it.copy(erro = r.error.message) }
            }
        }
    }

    fun update(f: (AssistenteUi) -> AssistenteUi) = _ui.update { f(it).copy(erro = null) }

    fun item(id: Long) = update { it.copy(itemId = id, data = null, turnoId = null, convidados = it.convidados.coerceAtMost(it.copy(itemId = id).limite)) }

    fun dia(d: LocalDate) = update { it.copy(data = d, turnoId = null) }

    fun carregarMes(m: YearMonth) {
        _ui.update { it.copy(mes = m, carregandoDisp = true) }
        viewModelScope.launch {
            val r = repo.disponibilidade(areaId, m)
            _ui.update { s -> if (s.mes != m) s else s.copy(carregandoDisp = false, disp = (r as? Outcome.Ok)?.value ?: s.disp, erro = (r as? Outcome.Err)?.error?.message) }
        }
    }

    fun avancar() = update { if (it.podeAvancar()) it.copy(passo = (it.passo + 1).coerceAtMost(3)) else it }
    fun voltar(): Boolean {
        if (_ui.value.passo == 0) return false
        update { it.copy(passo = it.passo - 1) }
        return true
    }

    /** [assinatura]: PNG do quadro, quando o modo é desenho */
    fun confirmar(assinatura: ByteArray?) {
        val s = _ui.value
        val a = s.area ?: return
        if (s.salvando || !s.podeAvancar()) return
        if (a.exigeTermo && s.modo == ModoAssinatura.DESENHO && assinatura == null) {
            _ui.update { it.copy(erro = "Desenhe sua assinatura no quadro (ou escolha “Digitar”).") }
            return
        }
        _ui.update { it.copy(salvando = true, erro = null) }
        viewModelScope.launch {
            val form = NovaReserva(
                a.id, s.itemId!!, s.turnoId!!, s.data!!, s.convidados, s.nomes.lines(), aceite = s.aceite,
                modo = if (a.exigeTermo) s.modo else null, nomeDigitado = s.nomeDigitado.takeIf { s.modo == ModoAssinatura.DIGITADA },
                observacoes = s.observacoes, assinatura = assinatura.takeIf { s.modo == ModoAssinatura.DESENHO }
            )
            when (val r = repo.criar(form)) {
                is Outcome.Ok -> _ui.update { it.copy(salvando = false, criada = r.value) }
                is Outcome.Err -> {
                    _ui.update { it.copy(salvando = false, erro = r.error.message) }
                    carregarMes(s.mes) // o horário pode ter sido tomado: atualiza o calendário
                }
            }
        }
    }
}

/** Reservas de hoje e de amanhã na portaria: check-in dos convidados e a chave da área. */
@HiltViewModel
class PortariaReservasViewModel @Inject constructor(private val repo: ReservasRepository, private val messenger: Messenger) : ViewModel() {
    private val _state = MutableStateFlow(Loadable<ReservasPortaria>())
    val state: StateFlow<Loadable<ReservasPortaria>> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            _state.value = when (val r = repo.portaria()) {
                is Outcome.Ok -> Loadable(r.value, loading = false)
                is Outcome.Err -> Loadable(_state.value.data, false, r.error.message)
            }
        }
    }

    private fun troca(r: Reserva) = _state.update { s ->
        s.copy(data = s.data?.let { d -> d.copy(hoje = d.hoje.map { if (it.id == r.id) r else it }, amanha = d.amanha.map { if (it.id == r.id) r else it }) })
    }

    fun checkin(r: Reserva, convidadoId: Long) {
        viewModelScope.launch {
            when (val c = repo.checkin(r.id, convidadoId)) {
                is Outcome.Ok -> troca(r.copy(lista = r.lista.map { if (it.id == convidadoId) c.value else it }))
                is Outcome.Err -> messenger.show(c.error.message)
            }
        }
    }

    fun incluir(r: Reserva, nome: String) {
        viewModelScope.launch {
            when (val c = repo.incluirConvidado(r.id, nome)) {
                is Outcome.Ok -> { troca(r.copy(lista = r.lista + c.value)); messenger.show("${c.value.nome} entrou como convidado extra") }
                is Outcome.Err -> messenger.show(c.error.message)
            }
        }
    }

    fun entregarChave(r: Reserva) {
        viewModelScope.launch {
            when (val x = repo.entregarChave(r.id)) {
                is Outcome.Ok -> {
                    troca(x.value)
                    messenger.undoable("Chave entregue · ${r.unidade}", restore = {
                        when (val d = repo.desfazerChave(r.id)) {
                            is Outcome.Ok -> { troca(d.value); Outcome.Ok(Unit) }
                            is Outcome.Err -> d
                        }
                    })
                }
                is Outcome.Err -> messenger.show(x.error.message)
            }
        }
    }

    fun devolucao(r: Reserva, ok: Boolean, obs: String?) {
        viewModelScope.launch {
            when (val x = repo.devolucao(r.id, ok, obs)) {
                is Outcome.Ok -> { troca(x.value); messenger.show(if (ok) "Chave devolvida · área em ordem" else "Chave devolvida · problema registrado") }
                is Outcome.Err -> messenger.show(x.error.message)
            }
        }
    }
}
