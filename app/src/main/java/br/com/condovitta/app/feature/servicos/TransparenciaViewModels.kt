package br.com.condovitta.app.feature.servicos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.condovitta.app.core.common.TimeProvider
import br.com.condovitta.app.domain.BalanceteHistorico
import br.com.condovitta.app.domain.EventoCalendario
import br.com.condovitta.app.domain.Lembrete
import br.com.condovitta.app.domain.MeuGas
import br.com.condovitta.app.domain.Outcome
import br.com.condovitta.app.domain.Transparencia
import br.com.condovitta.app.domain.TransparenciaRepository
import br.com.condovitta.app.feature.portaria.Loadable
import br.com.condovitta.app.ui.Messenger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/* ------------------------------------------------------------------ prestação de contas */

@HiltViewModel
class PrestacaoViewModel @Inject constructor(private val repo: TransparenciaRepository, private val messenger: Messenger) : ViewModel() {
    private val _state = MutableStateFlow(Loadable<Transparencia>())
    val state: StateFlow<Loadable<Transparencia>> = _state.asStateFlow()
    val historico = MutableStateFlow<List<BalanceteHistorico>>(emptyList())
    val baixando = MutableStateFlow<String?>(null)
    /** mês escolhido nos chips (null = o último publicado) */
    val mes = MutableStateFlow<String?>(null)

    fun refresh() = viewModelScope.launch {
        _state.carregar { repo.transparencia(mes.value) }
        (repo.balancetes() as? Outcome.Ok)?.let { historico.value = it.value }
    }

    fun escolher(ym: String) {
        if (ym == (mes.value ?: _state.value.data?.ym)) return
        mes.value = ym
        viewModelScope.launch { _state.carregar { repo.transparencia(ym) } }
    }

    fun pdf(ym: String, abrir: (ByteArray) -> Unit) = viewModelScope.launch { baixar(messenger, baixando, "pdf", { repo.balancetePdf(ym) }, abrir) }
    fun nota(id: Long, abrir: (ByteArray) -> Unit) = viewModelScope.launch { baixar(messenger, baixando, "n$id", { repo.nota(id) }, abrir) }
    fun arquivo(id: Long, abrir: (ByteArray) -> Unit) = viewModelScope.launch { baixar(messenger, baixando, "b$id", { repo.balanceteArquivo(id) }, abrir) }
}

/* ------------------------------------------------------------------ gás */

@HiltViewModel
class GasMoradorViewModel @Inject constructor(private val repo: TransparenciaRepository, private val messenger: Messenger) : ViewModel() {
    private val _state = MutableStateFlow(Loadable<MeuGas>())
    val state: StateFlow<Loadable<MeuGas>> = _state.asStateFlow()
    val baixando = MutableStateFlow<String?>(null)
    val envio = Envio()

    fun refresh() = viewModelScope.launch { _state.carregar { repo.meuGas() } }

    fun foto(leituraId: Long, abrir: (ByteArray) -> Unit) = viewModelScope.launch { baixar(messenger, baixando, "f$leituraId", { repo.fotoGas(leituraId) }, abrir) }

    fun contestar(mensagem: String) = viewModelScope.launch {
        envio.rodar({ repo.contestarGas(mensagem) }) {
            messenger.show("Contestação enviada · a administração vai conferir a leitura")
            refresh()
        }
    }
}

/* ------------------------------------------------------------------ calendário */

data class Agenda(val mes: YearMonth, val eventos: List<EventoCalendario>, val lembretes: List<Lembrete>) {
    fun doDia(d: LocalDate) = eventos.filter { it.data == d }
    fun lembrete(e: EventoCalendario) = e.eventoId?.let { id -> lembretes.firstOrNull { it.eventoId == id && it.data == e.data } }
}

@HiltViewModel
class CalendarioViewModel @Inject constructor(private val repo: TransparenciaRepository, private val messenger: Messenger, private val time: TimeProvider) : ViewModel() {
    private val _state = MutableStateFlow(Loadable<Agenda>())
    val state: StateFlow<Loadable<Agenda>> = _state.asStateFlow()
    val mes = MutableStateFlow(YearMonth.from(time.today()))
    val dia = MutableStateFlow<LocalDate?>(time.today())
    val baixando = MutableStateFlow<String?>(null)
    fun hoje(): LocalDate = time.today()

    fun refresh() = viewModelScope.launch {
        val m = mes.value
        _state.carregar {
            when (val ev = repo.calendario(m.atDay(1), m.atEndOfMonth())) {
                is Outcome.Err -> ev
                is Outcome.Ok -> Outcome.Ok(Agenda(m, ev.value, (repo.lembretes() as? Outcome.Ok)?.value ?: emptyList()))
            }
        }
    }

    fun mudarMes(m: YearMonth) {
        mes.value = m
        dia.value = if (m == YearMonth.from(time.today())) time.today() else null
        refresh()
    }

    fun escolherDia(d: LocalDate) { dia.value = if (dia.value == d) null else d }

    /** minutos 0 = tirar o lembrete */
    fun lembrar(e: EventoCalendario, minutos: Int) {
        val id = e.eventoId ?: return
        viewModelScope.launch {
            when (val r = repo.lembrete(id, e.data, minutos)) {
                is Outcome.Ok -> {
                    _state.value.data?.let { a ->
                        val outros = a.lembretes.filterNot { it.eventoId == id && it.data == e.data }
                        _state.value = Loadable(a.copy(lembretes = if (minutos > 0) outros + Lembrete(id, e.data, minutos) else outros), loading = false)
                    }
                    messenger.show(if (minutos > 0) "Vamos te lembrar ${antecedencia(minutos)}" else "Lembrete removido")
                }
                is Outcome.Err -> messenger.show(r.error.message)
            }
        }
    }

    fun agenda(e: EventoCalendario, abrir: (ByteArray) -> Unit) {
        val id = e.eventoId ?: return
        viewModelScope.launch { baixar(messenger, baixando, e.chave, { repo.ics(id, e.data) }, abrir) }
    }
}

/** "30 min antes", "1 h antes", "1 dia antes" */
fun antecedencia(min: Int): String = when {
    min < 60 -> "$min min antes"
    min < 1440 -> "${min / 60} h antes"
    else -> "${min / 1440} dia(s) antes"
}
